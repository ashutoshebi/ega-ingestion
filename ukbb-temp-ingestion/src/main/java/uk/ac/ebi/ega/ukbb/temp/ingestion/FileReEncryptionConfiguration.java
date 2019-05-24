/*
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
 */
package uk.ac.ebi.ega.ukbb.temp.ingestion;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.ega.encryption.core.BaseEncryptionService;
import uk.ac.ebi.ega.encryption.core.EncryptionService;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;
import uk.ac.ebi.ega.encryption.core.services.PasswordEncryptionService;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.services.ExecutorPersistenceService;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.utils.DelayConfiguration;
import uk.ac.ebi.ega.ukbb.temp.ingestion.services.IReEncryptService;
import uk.ac.ebi.ega.ukbb.temp.ingestion.services.ReEncryptService;

@Configuration
public class FileReEncryptionConfiguration {

    @Bean
    public EncryptionService encryptionService(){
        return new BaseEncryptionService();
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
