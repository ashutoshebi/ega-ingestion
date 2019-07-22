/*
 *
 * Copyright 2019 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.ebi.ega.file.encryption.processor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import uk.ac.ebi.ega.file.encryption.processor.models.IngestionProcess;
import uk.ac.ebi.ega.ingestion.commons.messages.EncryptComplete;
import uk.ac.ebi.ega.ingestion.commons.messages.IngestionEvent;
import uk.ac.ebi.ega.jobs.core.Job;
import uk.ac.ebi.ega.jobs.core.JobExecution;
import uk.ac.ebi.ega.jobs.core.JobExecutor;
import uk.ac.ebi.ega.jobs.core.Result;
import uk.ac.ebi.ega.jobs.core.exceptions.JobNotRegistered;
import uk.ac.ebi.ega.jobs.core.services.ExecutorPersistenceService;
import uk.ac.ebi.ega.jobs.core.utils.DelayConfiguration;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Optional;

public class EncryptService extends JobExecutor implements IEncryptService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptService.class);

    private static final String ENCRYPT_JOB = "encrypt-job";

    private Path stagingRoot;

    private KafkaTemplate<String, EncryptComplete> kafkaTemplate;

    private String completeJobTopic;

    public EncryptService(final Path stagingRoot,
                          final ExecutorPersistenceService persistenceService,
                          final Job<IngestionProcess> job,
                          final KafkaTemplate<String, EncryptComplete> kafkaTemplate,
                          final String completeJobTopic,
                          final DelayConfiguration delayConfiguration) {
        super(persistenceService, delayConfiguration);
        this.stagingRoot = stagingRoot;
        this.kafkaTemplate = kafkaTemplate;
        this.completeJobTopic = completeJobTopic;
        registerJob(ENCRYPT_JOB, IngestionProcess.class, job);
    }

    @Override
    public Optional<JobExecution<IngestionProcess>> createJob(String jobId, IngestionEvent data)
            throws JobNotRegistered {
        return assignExecution(jobId, ENCRYPT_JOB, new IngestionProcess(jobId, data, stagingRoot));
    }

    @Override
    public Result encrypt(JobExecution<IngestionProcess> jobExecution) {
        Result result;
        try {
            result = execute(jobExecution);
        } catch (JobNotRegistered jobNotRegistered) {
            result = Result.abort("Unexpected exception - JobParameters not registered", jobNotRegistered,
                    LocalDateTime.now());
        }
        if (Result.Status.SUCCESS.equals(result.getStatus())) {
            reportToFileManager(jobExecution.getJobId(), result);
        }
        if (Result.Status.FAILURE.equals(result.getStatus())) {
            logger.info("File or md5 values where not supplied properly");
            // TODO handle failures
        }

        return result;
    }

    @Override
    public Optional<JobExecution<IngestionProcess>> getUnfinishedJob() {
        return getAssignedExecution(ENCRYPT_JOB, IngestionProcess.class);
    }

    private void reportToFileManager(String jobId, Result<EncryptComplete> result) {
        kafkaTemplate.send(completeJobTopic, jobId, result.getData());
    }

    @Override
    public void cancelJobExecution(Optional<JobExecution<IngestionProcess>> optionalJobExecution, Exception e) {
        optionalJobExecution.ifPresent(jobExecution -> {
            cancelJobExecution(jobExecution.getJobId(), e);
        });
    }
}
