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
package uk.ac.ebi.ega.file.encryption.processor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.kafka.core.KafkaTemplate;
import uk.ac.ebi.ega.file.encryption.processor.jobs.EncryptJob;
import uk.ac.ebi.ega.file.encryption.processor.messages.EncryptComplete;
import uk.ac.ebi.ega.file.encryption.processor.models.EncryptJobParameters;
import uk.ac.ebi.ega.file.encryption.processor.persistent.repository.EncryptParametersRepository;
import uk.ac.ebi.ega.file.encryption.processor.persistent.services.EncryptJobParameterService;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.PipelineBuilder;
import uk.ac.ebi.ega.file.encryption.processor.services.EncryptPersistenceService;
import uk.ac.ebi.ega.file.encryption.processor.services.EncryptService;
import uk.ac.ebi.ega.file.encryption.processor.services.PipelineService;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Job;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.persistence.repository.JobExecutionRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.persistence.repository.JobRunRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.services.ExecutorPersistenceService;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.utils.DelayConfiguration;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.utils.DelayConfiguration.DelayType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Configuration
public class EncryptionConfiguration {

    @Value("${spring.kafka.client-id}")
    private String instanceId;

    @Value("${file.re.encryption.mail.alert}")
    private String reportTo;

    @Value("${spring.kafka.file.encryption.completed.queue.name}")
    private String completedTopic;

    /**
     * Specifies the type of delay between retries of executing a job.
     * See {@link DelayType}.
     */
    @Value("${file.re.encryption.job.execution.retry.type:LINEAR}")
    private DelayType jobExecutionRetryType;

    /**
     * Specifies the amount of fixed delay (in seconds)
     * between retries of executing a job when the type of delay is {@link DelayType#LINEAR}.
     */
    @Value("${file.re.encryption.job.execution.retry.delay:5}")
    private long jobExecutionRetryDelay;

    /**
     * Specifies the maximum amount of delay (in seconds)
     * between retries of executing a job when the type of delay is {@link DelayType#BACKOFF}.
     */
    @Value("${file.re.encryption.job.execution.retry.max.delay:30}")
    private long jobExecutionRetryMaxDelay;

    @Bean("pipelineService")
    public PipelineService ingestionPipelineService(
            @Value("${file.encryption.keyring.private}") String privateKeyRing,
            @Value("${file.encryption.keyring.private.key}") String privateKeyRingPassword)
            throws FileNotFoundException {
        File privateKeyRingFile = new File(privateKeyRing);
        if (!privateKeyRingFile.exists()) {
            throw new FileNotFoundException("Private key ring file could not be found");
        }
        File privateKeyRingPasswordFile = new File(privateKeyRingPassword);
        if (!privateKeyRingPasswordFile.exists()) {
            throw new FileNotFoundException("Password file for private key ring could not be found");
        }
        return new PipelineBuilder(privateKeyRingFile, privateKeyRingPasswordFile);
    }

    @Bean
    public EncryptJobParameterService reEncryptJobParameterService(EncryptParametersRepository repository) {
        return new EncryptJobParameterService(repository);
    }

    @Bean
    public ExecutorPersistenceService persistenceService(JobExecutionRepository jobExecutionRepository,
                                                         JobRunRepository jobRunRepository,
                                                         EncryptJobParameterService encryptJobParameterService) {
        return new EncryptPersistenceService(jobExecutionRepository, jobRunRepository, instanceId,
                encryptJobParameterService);
    }

    @DependsOn("pipelineService")
    @Bean
    public Job<EncryptJobParameters> encryptJob(PipelineService pipelineService, @Value("${spring.kafka.client-id}") String clientId, @Value("${file.encryption.staging.root}") String staging) throws IOException {

        final File stagingRoot = new File(staging);

        if (!stagingRoot.exists()) {
            throw new FileNotFoundException("Staging path for encryption is not found");
        }
        return new EncryptJob(pipelineService, clientId, stagingRoot.toPath());
    }

    @Bean
    public DelayConfiguration delayConfiguration() {
        return new DelayConfiguration(jobExecutionRetryType, jobExecutionRetryDelay,
                jobExecutionRetryMaxDelay, TimeUnit.SECONDS);
    }

    @Bean
    public EncryptService encryptService(ExecutorPersistenceService executorPersistenceService,
                                         Job<EncryptJobParameters> job,
                                         KafkaTemplate<String, EncryptComplete> kafkaTemplate,
                                         final DelayConfiguration delayConfiguration) {
        return new EncryptService(executorPersistenceService, reportTo, job,
                kafkaTemplate, completedTopic, delayConfiguration);
    }
}
