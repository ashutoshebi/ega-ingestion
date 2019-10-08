/*
 *
 * Copyright 2018 EMBL - European Bioinformatics Institute
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
package uk.ac.ebi.ega.ingestion.file.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import uk.ac.ebi.ega.fire.ingestion.service.IFireService;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.EncryptedObjectRepository;
import uk.ac.ebi.ega.ingestion.file.manager.services.FileStatusUpdaterService;
import uk.ac.ebi.ega.ingestion.file.manager.services.IFileStatusUpdaterService;

import java.net.URISyntaxException;

@Configuration
@EnableScheduling
public class FileStatusUpdaterConfiguration {

    @Autowired
    private IFileStatusUpdaterService fileStatusUpdaterService;

    @Bean
    public IFileStatusUpdaterService fileStatusUpdaterService(final EncryptedObjectRepository encryptedObjectRepository,
                                                              final IFireService fireService,
                                                              @Value("${file.status.updater.batch.size}") final int batchSize) {
        return new FileStatusUpdaterService(encryptedObjectRepository, fireService, batchSize);
    }

    @Scheduled(fixedDelayString = "${file.status.updater.fixed.delay.seconds}000")
    public void fileStatusUpdaterService() {
        fileStatusUpdaterService.updateStatus();
    }

}
