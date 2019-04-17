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
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.repository.HistoricProcessDownloadBoxFileRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.repository.ProcessDownloadBoxFileRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.services.IMailingService;
import uk.ac.ebi.ega.file.re.encryption.processor.services.IReEncryptService;
import uk.ac.ebi.ega.file.re.encryption.processor.services.MailingService;
import uk.ac.ebi.ega.file.re.encryption.processor.services.ProcessDownloadService;
import uk.ac.ebi.ega.file.re.encryption.processor.services.ProcessService;
import uk.ac.ebi.ega.file.re.encryption.processor.services.ReEncryptService;
import uk.ac.ebi.ega.fire.IFireService;

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

    @Bean
    public ProcessService processDownloadService(ProcessDownloadBoxFileRepository processDownloadBoxFileRepository,
                                                 HistoricProcessDownloadBoxFileRepository historicRepository) {
        return new ProcessDownloadService(instanceId, processDownloadBoxFileRepository, historicRepository);
    }

    @Bean
    public IMailingService mailingService(JavaMailSender javaMailSender) {
        return new MailingService(javaMailSender, applicationName, instanceId);
    }

    @Bean
    public IReEncryptService reEncryptService(IFireService fireService, IMailingService mailingService) {
        return new ReEncryptService(fireService, passwordFile, mailingService, reportTo);
    }

}
