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
package uk.ac.ebi.ega.staging.ingestion.listener;

import org.apache.kafka.clients.consumer.CommitFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import uk.ac.ebi.ega.ingestion.commons.messages.IngestionEvent;
import uk.ac.ebi.ega.ingestion.commons.messages.NewFileEvent;
import uk.ac.ebi.ega.staging.ingestion.service.StagingIngestionService;

import java.time.Duration;
import java.util.Optional;

public class StagingIngestionListener {

    private final static Logger logger = LoggerFactory.getLogger(StagingIngestionListener.class);

    private StagingIngestionService service;

    private String newFileTopic;

    private KafkaTemplate<String, NewFileEvent> kafkaTemplate;

    public StagingIngestionListener(StagingIngestionService service,
                                    String newFileTopic,
                                    KafkaTemplate<String, NewFileEvent> kafkaTemplate) {
        this.service = service;
        this.newFileTopic = newFileTopic;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "${staging.ingestion.queue}",
            groupId = "${staging.ingestion.groupid}", clientIdPrefix = "${staging.ingestion.instance}",
            autoStartup = "true")
    public void listen(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
                       @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                       @Header(KafkaHeaders.RECEIVED_TIMESTAMP) long ts,
                       IngestionEvent ingestionEvent,
                       Acknowledgment acknowledgment) {

        logger.info("Staging ingestion event {}", key);
        try {
            final Optional<NewFileEvent> ingest = service.ingest(key, ingestionEvent);
            if (ingest.isPresent()) {
                success(key, ingestionEvent, acknowledgment, ingest.get());
            } else {
                skip(acknowledgment);
            }
        } catch (CommitFailedException e) {
            error(null, e);
        } catch (Exception e) {
            error(acknowledgment, e);
        }
    }

    private void success(String key, IngestionEvent ingestionEvent, Acknowledgment acknowledgment,
                         NewFileEvent newFileEvent) {
        kafkaTemplate.send(newFileTopic, key, newFileEvent);
        acknowledgment.acknowledge();
        service.cleanMd5Files(ingestionEvent);
        logger.info("Ingestion completed");
    }

    private void skip(Acknowledgment acknowledgment) {
        acknowledgment.acknowledge();
        logger.info("Ingestion skipped");
    }

    private void error(Acknowledgment acknowledgment, Exception e) {
        // TODO error queue (this is likely a fatal error)
        logger.error(e.getMessage(), e);
        if (acknowledgment != null) {
            try {
                acknowledgment.acknowledge();
            } catch (CommitFailedException e2) {
                logger.error(e.getMessage(), e2);
            }
        }
    }

}
