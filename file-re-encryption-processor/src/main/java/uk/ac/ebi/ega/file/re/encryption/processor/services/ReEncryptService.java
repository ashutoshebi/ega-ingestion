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
package uk.ac.ebi.ega.file.re.encryption.processor.services;

import org.springframework.kafka.core.KafkaTemplate;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;
import uk.ac.ebi.ega.jobs.core.Job;
import uk.ac.ebi.ega.jobs.core.JobExecution;
import uk.ac.ebi.ega.jobs.core.JobExecutor;
import uk.ac.ebi.ega.jobs.core.Result;
import uk.ac.ebi.ega.jobs.core.exceptions.JobNotRegistered;
import uk.ac.ebi.ega.jobs.core.services.ExecutorPersistenceService;
import uk.ac.ebi.ega.jobs.core.utils.DelayConfiguration;
import uk.ac.ebi.ega.file.re.encryption.processor.messages.ReEncryptComplete;
import uk.ac.ebi.ega.file.re.encryption.processor.models.ReEncryptJobParameters;

import java.time.LocalDateTime;
import java.util.Optional;

public class ReEncryptService extends JobExecutor implements IReEncryptService {

    private static final String RE_ENCRYPT_JOB = "re-encrypt-job";

    private IPasswordEncryptionService passwordService;

    private IMailingService mailingService;

    private String reportTo;

    private KafkaTemplate<String, ReEncryptComplete> kafkaTemplate;

    private String completeJobTopic;

    public ReEncryptService(ExecutorPersistenceService persistenceService,
                            IPasswordEncryptionService passwordService,
                            IMailingService mailingService, String reportTo,
                            Job<ReEncryptJobParameters> job,
                            KafkaTemplate<String, ReEncryptComplete> kafkaTemplate,
                            String completeJobTopic,
                            final DelayConfiguration delayConfiguration) {
        super(persistenceService, delayConfiguration);
        this.passwordService = passwordService;
        this.mailingService = mailingService;
        this.reportTo = reportTo;
        this.kafkaTemplate = kafkaTemplate;
        this.completeJobTopic = completeJobTopic;
        registerJob(RE_ENCRYPT_JOB, ReEncryptJobParameters.class, job);
    }

    @Override
    public Optional<JobExecution<ReEncryptJobParameters>> createJob(String id, String dosId, String resultPath,
                                                                    String encryptedPassword) throws JobNotRegistered {
        return assignExecution(id, RE_ENCRYPT_JOB,
                new ReEncryptJobParameters(passwordService, dosId, resultPath + ".cip", encryptedPassword));
    }

    @Override
    public Result reEncrypt(JobExecution<ReEncryptJobParameters> jobExecution) {
        Result result;
        final LocalDateTime startExecution = LocalDateTime.now();
        try {
            result = execute(jobExecution);
        } catch (JobNotRegistered jobNotRegistered) {
            result = Result.abort("Unexpected exception - JobParameters not registered", jobNotRegistered,
                    LocalDateTime.now());
        }
        if (result.getStatus() != Result.Status.SUCCESS) {
            mailingService.sendSimpleMessage(reportTo, result.getMessage(), result.getException());
        }
        if (result.getStatus() != Result.Status.ABORTED) {
            // report to file manager if it has finished.
            reportToFileManager(jobExecution.getJobId(), startExecution, result);
        }
        return result;
    }

    @Override
    public Optional<JobExecution<ReEncryptJobParameters>> getUnfinishedJob() {
        return getAssignedExecution(RE_ENCRYPT_JOB, ReEncryptJobParameters.class);
    }

    private void reportToFileManager(String jobId, LocalDateTime startTime, Result result) {
        kafkaTemplate.send(completeJobTopic, jobId, new ReEncryptComplete(result.getStatus(),
                result.getMessage(), startTime, result.getEndTime()));
    }
}
