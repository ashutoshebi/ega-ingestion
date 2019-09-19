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

import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import uk.ac.ebi.ega.file.encryption.processor.models.IngestionProcess;
import uk.ac.ebi.ega.file.encryption.processor.services.IEncryptService;
import uk.ac.ebi.ega.ingestion.commons.messages.IngestionEvent;
import uk.ac.ebi.ega.jobs.core.JobExecution;
import uk.ac.ebi.ega.jobs.core.Result;
import uk.ac.ebi.ega.jobs.core.exceptions.JobNotRegistered;

import java.util.Optional;

@Component
public class IngestionEventListener {

    private final Logger logger = LoggerFactory.getLogger(IngestionEventListener.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private IEncryptService encryptService;

    @KafkaListener(id = "${spring.kafka.client-id}", topics = "${spring.kafka.staging.ingestion.queue.name}",
            groupId = "file-ingestion", clientIdPrefix = "executor", autoStartup = "false")
    public void listen(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key, IngestionEvent data,
                       Acknowledgment acknowledgment) {
        logger.info("Process - key: {} data {}", key, data);

        Optional<JobExecution<IngestionProcess>> optionalJob = Optional.empty();

        try {
            optionalJob = encryptService.createJob(key, data);
            acknowledgment.acknowledge();
        } catch (JobNotRegistered exception) {
            exitApplication("Critical error: Job is not registered: " + exception.getMessage());
        } catch (KafkaException e) {
            encryptService.cancelJobExecution(optionalJob, e);
            optionalJob = Optional.empty();
        }

        if (optionalJob.isPresent()) {
            final Result result = encryptService.encrypt(optionalJob.get());
            if (result.getStatus() == Result.Status.ABORTED) {
                exitApplication("Process was aborted due to critical error. Unexpected application termination");
            }
        } else {
            logger.info("key: {} is being processed, skip event", key);
        }
    }

    private void exitApplication(final String message) {
        logger.error(message);
        SpringApplication.exit(applicationContext, () -> 1);
    }
}
