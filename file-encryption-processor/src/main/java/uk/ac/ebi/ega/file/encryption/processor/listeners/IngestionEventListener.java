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
package uk.ac.ebi.ega.file.encryption.processor.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import uk.ac.ebi.ega.file.encryption.processor.messages.IngestionEvent;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.UserErrorException;
import uk.ac.ebi.ega.file.encryption.processor.services.ProcessEncryptionFileService;

@Component
public class IngestionEventListener {

    private final Logger logger = LoggerFactory.getLogger(IngestionEventListener.class);

    @Autowired
    private ProcessEncryptionFileService processEncryptionFileService;

    @KafkaListener(id = "${spring.kafka.client-id}", topics = "file-ingestion", groupId = "encryption",
            clientIdPrefix = "executor", autoStartup = "false")
    public void listen(IngestionEvent data) {
        logger.error("Received {}", data);

        try {
            processEncryptionFileService.processFile(data.getAccountId(), data.getLocationId(),
                    data.getAbsolutePathFile(), data.getSize(), data.getLastModified(), data.getAbsolutePathMd5File(),
                    data.getMd5Size(), data.getMd5LastModified());
        } catch (UserErrorException e) {
            // Error with password / key used to encrypt, user uploaded something wrong on the first bytes of the file
            // original file is restored, maybe we could contemplate to delete the file instead. Better get real usage
            // first
            logger.warn(e.getMessage());
        } catch (SystemErrorException e) {
            // Big problem on the problem, dead letter to team, files should have been reverted to original places
            logger.error(e.getMessage());
        }
    }

}
