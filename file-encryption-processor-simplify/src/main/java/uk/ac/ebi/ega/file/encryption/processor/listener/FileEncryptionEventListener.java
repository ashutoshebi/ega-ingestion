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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import uk.ac.ebi.ega.file.encryption.processor.model.IIngestionEventData;
import uk.ac.ebi.ega.file.encryption.processor.model.IngestionEventData;
import uk.ac.ebi.ega.file.encryption.processor.model.Result;
import uk.ac.ebi.ega.file.encryption.processor.service.IFileEncryptionProcessor;
import uk.ac.ebi.ega.ingestion.commons.messages.ArchiveEventSimplify;
import uk.ac.ebi.ega.ingestion.commons.messages.IngestionEventSimplify;

import java.nio.file.Path;

public class FileEncryptionEventListener {

    private final Logger logger = LoggerFactory.getLogger(FileEncryptionEventListener.class);

    private final IFileEncryptionProcessor<IIngestionEventData> fileEncryptionProcessor;
    private final KafkaTemplate<String, ArchiveEventSimplify> kafkaTemplate;
    private final String completeJobTopic;
    private final Path outputFolderPath;

    public FileEncryptionEventListener(final IFileEncryptionProcessor<IIngestionEventData> fileEncryptionProcessor,
                                       final KafkaTemplate<String, ArchiveEventSimplify> kafkaTemplate, final String completeJobTopic,
                                       final Path outputFolderPath) {
        this.fileEncryptionProcessor = fileEncryptionProcessor;
        this.kafkaTemplate = kafkaTemplate;
        this.completeJobTopic = completeJobTopic;
        this.outputFolderPath = outputFolderPath;
    }

    @SuppressWarnings("unchecked")
    @KafkaListener(id = "${spring.kafka.client-id}", topics = "${spring.kafka.staging.ingestion.queue.name}",
            groupId = "file-ingestion", clientIdPrefix = "executor", autoStartup = "false")
    public void listen(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key, IngestionEventSimplify ingestionEventDataSimplify,
                       Acknowledgment acknowledgment) {
        logger.info("Process - key: {} data {}", key, ingestionEventDataSimplify);

        final IIngestionEventData ingestionEventData = new IngestionEventData(ingestionEventDataSimplify, outputFolderPath);
        final Result<ArchiveEventSimplify> result = fileEncryptionProcessor.encrypt(ingestionEventData);

        /*Calling acknowledge() in both cases Success & Failure assuming FileManager will resend message
        on kafka for re-encryption in case of failure.*/
        acknowledgment.acknowledge();
        reportToFileManager(key, result);
    }

    private void reportToFileManager(final String key, final Result<ArchiveEventSimplify> result) {
        logger.info("Data sent to kafka topic={}, key={}, data={}", completeJobTopic, key, result.getData());
        kafkaTemplate.send(completeJobTopic, key, result.getData());
    }
}
