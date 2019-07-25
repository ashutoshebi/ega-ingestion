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
package uk.ac.ebi.ega.file.re.encryption.processor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;
import uk.ac.ebi.ega.encryption.core.services.PasswordEncryptionService;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.ReEncryptJob;
import uk.ac.ebi.ega.jobs.core.Job;
import uk.ac.ebi.ega.jobs.core.persistence.repository.JobExecutionRepository;
import uk.ac.ebi.ega.jobs.core.persistence.repository.JobRunRepository;
import uk.ac.ebi.ega.jobs.core.services.ExecutorPersistenceService;
import uk.ac.ebi.ega.jobs.core.utils.DelayConfiguration;
import uk.ac.ebi.ega.jobs.core.utils.DelayConfiguration.DelayType;
import uk.ac.ebi.ega.file.re.encryption.processor.messages.ReEncryptComplete;
import uk.ac.ebi.ega.file.re.encryption.processor.models.ReEncryptJobParameters;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.repository.ReEncryptParametersRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.service.ReEncryptJobParameterService;
import uk.ac.ebi.ega.file.re.encryption.processor.services.IMailingService;
import uk.ac.ebi.ega.file.re.encryption.processor.services.IReEncryptService;
import uk.ac.ebi.ega.file.re.encryption.processor.services.MailingService;
import uk.ac.ebi.ega.file.re.encryption.processor.services.ReEncryptPersistenceService;
import uk.ac.ebi.ega.file.re.encryption.processor.services.ReEncryptService;
import uk.ac.ebi.ega.fire.IFireService;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Configuration
public class FileReEncryptionConfiguration {

    @Value("${spring.kafka.client-id}")
    private String instanceId;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${file.re.encryption.password.file}")
    private String passwordFile;

    @Value("${file.re.encryption.mail.alert}")
    private String reportTo;

    @Value("${spring.kafka.download-box.completed.queue.name}")
    private String completedTopic;

    @Value("${file.re.encryption.password.encryption.key}")
    private char[] passwordEncryptionKey;

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

    @Bean
    public IMailingService mailingService(JavaMailSender javaMailSender) {
        return new MailingService(javaMailSender, applicationName, instanceId);
    }

    @Bean
    public IPasswordEncryptionService passwordService(){
        return new PasswordEncryptionService(passwordEncryptionKey);
    }

    @Bean
    public ReEncryptJobParameterService reEncryptJobParameterService(ReEncryptParametersRepository repository,
                                                                     IPasswordEncryptionService passwordService) {
        return new ReEncryptJobParameterService(repository, passwordService);
    }

    @Bean
    public ExecutorPersistenceService persistenceService(JobExecutionRepository jobExecutionRepository,
                                                         JobRunRepository jobRunRepository,
                                                         ReEncryptJobParameterService reEncryptJobParameterService) {
        return new ReEncryptPersistenceService(jobExecutionRepository, jobRunRepository, instanceId,
                reEncryptJobParameterService);
    }

    @Bean
    public Job<ReEncryptJobParameters> reEncryptJob(IFireService fireService) throws IOException {
        return new ReEncryptJob(fireService, FileUtils.readPasswordFile(Paths.get(passwordFile)));
    }

    @Bean
    public DelayConfiguration delayConfiguration() {
        return new DelayConfiguration(jobExecutionRetryType, jobExecutionRetryDelay,
                jobExecutionRetryMaxDelay, TimeUnit.SECONDS);
    }

    @Bean
    public IReEncryptService reEncryptService(ExecutorPersistenceService executorPersistenceService,
                                              IPasswordEncryptionService passwordService,
                                              IMailingService mailingService,
                                              Job<ReEncryptJobParameters> job,
                                              KafkaTemplate<String, ReEncryptComplete> kafkaTemplate,
                                              final DelayConfiguration delayConfiguration) {
        return new ReEncryptService(executorPersistenceService, passwordService, mailingService, reportTo, job,
                kafkaTemplate, completedTopic, delayConfiguration);
    }

}
