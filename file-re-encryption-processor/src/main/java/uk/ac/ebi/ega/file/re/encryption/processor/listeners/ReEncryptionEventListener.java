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
package uk.ac.ebi.ega.file.re.encryption.processor.listeners;

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
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.JobExecution;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;
import uk.ac.ebi.ega.file.re.encryption.processor.messages.ReEncryptFile;
import uk.ac.ebi.ega.file.re.encryption.processor.models.ReEncryptJobParameters;
import uk.ac.ebi.ega.file.re.encryption.processor.services.IReEncryptService;

import java.util.Optional;

@Component
public class ReEncryptionEventListener {

    private final Logger logger = LoggerFactory.getLogger(ReEncryptionEventListener.class);

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private IReEncryptService reEncryptService;

    @KafkaListener(id = "${spring.kafka.client-id}", topics = "${spring.kafka.file.re.encryption.queue.name}",
            groupId =
            "${spring.kafka.consumer.group-id}", clientIdPrefix = "executor", autoStartup = "false")
    public void listen(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key, ReEncryptFile data,
                       Acknowledgment acknowledgment) {
        logger.info("Process - key: {} data {}", key, data);

        final Optional<JobExecution<ReEncryptJobParameters>> job = reEncryptService.createJob(key, data.getDosId(), data.getResultPath(),
                data.getResultPath().toCharArray());
        acknowledgment.acknowledge();
        if (job.isPresent()) {
            final Result result = reEncryptService.reEncrypt(job.get());
            if (result.getStatus() == Result.Status.ABORTED) {
                logger.error("Process was aborted due to critical error. Unexpected application termination");
                SpringApplication.exit(applicationContext, () -> 1);
            }
        } else {
            logger.info("key: {} is being processed, skip event", key);
        }
    }

}
