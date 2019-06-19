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

import org.springframework.kafka.core.KafkaTemplate;
import uk.ac.ebi.ega.file.encryption.processor.messages.EncryptComplete;
import uk.ac.ebi.ega.file.encryption.processor.models.EncryptJobParameters;
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

    private static final String ENCRYPT_JOB = "encrypt-job";

    private String reportTo; //TODO variable is not used. Can be used once mailing service will be integrated.

    private KafkaTemplate<String, EncryptComplete> kafkaTemplate;

    private String completeJobTopic;

    public EncryptService(final ExecutorPersistenceService persistenceService,
                          final String reportTo,
                          final Job<EncryptJobParameters> job,
                          final KafkaTemplate<String, EncryptComplete> kafkaTemplate,
                          final String completeJobTopic,
                          final DelayConfiguration delayConfiguration) {
        super(persistenceService, delayConfiguration);
        this.reportTo = reportTo;
        this.kafkaTemplate = kafkaTemplate;
        this.completeJobTopic = completeJobTopic;
        registerJob(ENCRYPT_JOB, EncryptJobParameters.class, job);
    }

    @Override
    public Optional<JobExecution<EncryptJobParameters>> createJob(final String id, final String accountId, final String stagingId, final Path filePath, final long size, final LocalDateTime lastUpdate,
                                                                  final Path md5FilePath) throws JobNotRegistered {
        return assignExecution(id, ENCRYPT_JOB,
                new EncryptJobParameters(accountId, stagingId, filePath, size, lastUpdate, md5FilePath));
    }

    @Override
    public Result encrypt(JobExecution<EncryptJobParameters> jobExecution) {
        Result result;
        final LocalDateTime startExecution = LocalDateTime.now();
        try {
            result = execute(jobExecution);
        } catch (JobNotRegistered jobNotRegistered) {
            result = Result.abort("Unexpected exception - JobParameters not registered", jobNotRegistered,
                    LocalDateTime.now());
        }
        if (!Result.Status.SUCCESS.equals(result.getStatus())) {
            //TODO Needs to be implemented
            // mailingService.sendSimpleMessage(reportTo, result.getMessage(), result.getException());
        }
        if (!Result.Status.ABORTED.equals(result.getStatus())) {
            // report to file manager if it has finished.
            reportToFileManager(jobExecution.getJobId(), startExecution, result);
        }
        return result;
    }

    @Override
    public Optional<JobExecution<EncryptJobParameters>> getUnfinishedJob() {
        return getAssignedExecution(ENCRYPT_JOB, EncryptJobParameters.class);
    }

    private void reportToFileManager(String jobId, LocalDateTime startTime, Result<EncryptComplete> result) {
        kafkaTemplate.send(completeJobTopic, jobId, result.getData());
    }
}
