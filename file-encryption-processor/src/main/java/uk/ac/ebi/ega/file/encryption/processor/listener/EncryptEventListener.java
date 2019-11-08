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
package uk.ac.ebi.ega.file.encryption.processor.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import uk.ac.ebi.ega.file.encryption.processor.service.IFileEncryptionService;
import uk.ac.ebi.ega.ingestion.commons.messages.EncryptEvent;
import uk.ac.ebi.ega.ingestion.commons.messages.FileEncryptionResult;

public class EncryptEventListener {

    private final Logger logger = LoggerFactory.getLogger(EncryptEventListener.class);

    private final IFileEncryptionService fileEncryptionProcessor;
    private final IFileEncryptionService gsEncryptionProcessor;
    private final KafkaTemplate<String, FileEncryptionResult> kafkaTemplate;
    private final String completeJobTopic;

    public EncryptEventListener(@Qualifier("system_file_encryption") final IFileEncryptionService fileEncryptionProcessor,
                                @Qualifier("gs_file_encryption") final IFileEncryptionService gsEncryptionProcessor,
                                final KafkaTemplate<String, FileEncryptionResult> kafkaTemplate,
                                final String completeJobTopic) {
        this.fileEncryptionProcessor = fileEncryptionProcessor;
        this.gsEncryptionProcessor = gsEncryptionProcessor;
        this.kafkaTemplate = kafkaTemplate;
        this.completeJobTopic = completeJobTopic;
    }

    @KafkaListener(id = "${spring.kafka.client-id}", topics = "${spring.kafka.file.encrypt.queue.name}",
            groupId = "file-ingestion", clientIdPrefix = "executor", autoStartup = "true")
    public void listen(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key, EncryptEvent encryptEvent,
                       Acknowledgment acknowledgment) {
        logger.info("Process - key: {} data {}", key, encryptEvent);

        if (encryptEvent.getUri().getPath().startsWith("file://")) {
            reportToFileManager(key, fileEncryptionProcessor.encrypt(encryptEvent));
        } else if (encryptEvent.getUri().getPath().startsWith("gs://")) {
            reportToFileManager(key, gsEncryptionProcessor.encrypt(encryptEvent));
        }
        acknowledgment.acknowledge();
    }

    private void reportToFileManager(final String key, final FileEncryptionResult fileEncryptionResult) {
        logger.info("Data sent to kafka topic={}, key={}, data={}", completeJobTopic, key, fileEncryptionResult.getData());
        kafkaTemplate.send(completeJobTopic, key, fileEncryptionResult);
    }
}
