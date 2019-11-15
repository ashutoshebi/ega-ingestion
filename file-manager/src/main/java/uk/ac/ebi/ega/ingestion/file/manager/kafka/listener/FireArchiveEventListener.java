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
import uk.ac.ebi.ega.ingestion.commons.messages.FireArchiveResult;
import uk.ac.ebi.ega.ingestion.file.manager.services.IFileManagerService;

public class FireArchiveEventListener {

    private final Logger LOGGER = LoggerFactory.getLogger(FireArchiveEventListener.class);

    private final IFileManagerService fileManagerService;

    public FireArchiveEventListener(final IFileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @KafkaListener(
            topics = "${fire.archive.queue.name}",
            groupId = "${file.archive.group-id}",
            clientIdPrefix = "${file.archive.group-id}",
            containerFactory = "archiveEventListenerContainerFactory")
    public void listenArchiveEventQueue(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
                                        FireArchiveResult fireArchiveResult,
                                        Acknowledgment acknowledgment) {
        LOGGER.info("File archived event key: {}, data: {}", key, fireArchiveResult);
        fileManagerService.archived(key, fireArchiveResult);
        acknowledgment.acknowledge();
    }
}