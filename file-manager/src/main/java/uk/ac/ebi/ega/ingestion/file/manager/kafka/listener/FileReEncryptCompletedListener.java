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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import uk.ac.ebi.ega.ingestion.file.manager.controller.exceptions.FileJobNotFound;
import uk.ac.ebi.ega.ingestion.file.manager.kafka.message.ReEncryptComplete;
import uk.ac.ebi.ega.ingestion.file.manager.services.IDownloadBoxJobService;

@Component
public class FileReEncryptCompletedListener {

    private final Logger log = LoggerFactory.getLogger(FileReEncryptCompletedListener.class);

    @Autowired
    private IDownloadBoxJobService service;

    @KafkaListener(id = "${spring.kafka.client-id}", topics = "${spring.kafka.download-box.completed.queue.name}",
            groupId = "${spring.kafka.consumer.group-id}")
    public void listen(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key, ReEncryptComplete complete) {
        log.info("File re-encryption id: {} completed with status: {}", key, complete.getStatus());
        try {
            service.finishFileJob(key, complete.getMessage(), complete.getStartTime(), complete.getEndTime());
        } catch (FileJobNotFound fileJobNotFound) {
            log.error(fileJobNotFound.getMessage(), fileJobNotFound);
            // TODO send mail to dev team, there is a db problem somewhere?
        }
    }

}
