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
package uk.ac.ebi.ega.ingestion.file.manager.kafka.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import uk.ac.ebi.ega.ingestion.commons.messages.EncryptComplete;
import uk.ac.ebi.ega.ingestion.file.manager.services.IEncryptJobService;

public class FileArchiveListener {

    private final Logger log = LoggerFactory.getLogger(FileArchiveListener.class);

    private final IEncryptJobService encryptJobService;

    public FileArchiveListener(final IEncryptJobService encryptJobService) {
        this.encryptJobService = encryptJobService;
    }

    @KafkaListener(id = "${spring.kafka.client-id}", topics = "${spring.kafka.file.archive.queue.name}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void listenEncryptionCompletedQueue(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key, EncryptComplete encryptComplete,
                                               Acknowledgment acknowledgment) {

        log.info("File encryption id: {} completed", key);

        try {
            encryptJobService.notify(key, encryptComplete);

            // Acknowledge queue that message has been processed successfully
            acknowledgment.acknowledge();
        } catch (Exception e) {
            //TODO report/log error here. Add error handling.
        }
    }
}
