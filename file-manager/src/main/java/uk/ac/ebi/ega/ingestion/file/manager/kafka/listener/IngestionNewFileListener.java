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
import uk.ac.ebi.ega.ingestion.commons.messages.NewFileEvent;
import uk.ac.ebi.ega.ingestion.file.manager.services.IFileManagerService;

public class IngestionNewFileListener {

    private final static Logger logger = LoggerFactory.getLogger(IngestionNewFileListener.class);

    private IFileManagerService fileManagerService;

    public IngestionNewFileListener(IFileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @KafkaListener(
            topics = "${new.file.queue.name}",
            groupId = "${new.file.group-id}",
            clientIdPrefix = "${new.file.group-id}",
            containerFactory = "ingestionNewFileListenerContainerFactory")
    public void listenIngestionNewFileEventQueue(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
                                                 @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                                 @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long ts,
                                                 NewFileEvent newFileEvent,
                                                 Acknowledgment acknowledgment) {
        logger.info("key: {}, event: {}", key, newFileEvent);
        fileManagerService.newFile(key, newFileEvent);
        acknowledgment.acknowledge();
    }

}
