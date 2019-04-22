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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.ReEncryptJob;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Job;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.persistence.repository.JobExecutionRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.persistence.repository.JobRunRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.services.ExecutorPersistenceService;
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

@Configuration
public class FileReEncryptionConfiguration {

    @Autowired
    private ApplicationContext applicationContext;

    @Value("${spring.kafka.client-id}")
    private String instanceId;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${file.re.encryption.password.file}")
    private String passwordFile;

    @Value("${file.re.encryption.mail.alert}")
    private String reportTo;

    @Value("${spring.kafka.file.re.encryption.completed.queue.name}")
    private String completedTopic;

    @Bean
    public IMailingService mailingService(JavaMailSender javaMailSender) {
        return new MailingService(javaMailSender, applicationName, instanceId);
    }

    @Bean
    public ReEncryptJobParameterService reEncryptJobParameterService(ReEncryptParametersRepository repository) {
        return new ReEncryptJobParameterService(repository);
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
    public IReEncryptService reEncryptService(ExecutorPersistenceService executorPersistenceService,
                                              IMailingService mailingService,
                                              Job<ReEncryptJobParameters> job,
                                              KafkaTemplate<String, ReEncryptComplete> kafkaTemplate) {
        return new ReEncryptService(executorPersistenceService, mailingService, reportTo, job, kafkaTemplate,
                completedTopic);
    }

}
