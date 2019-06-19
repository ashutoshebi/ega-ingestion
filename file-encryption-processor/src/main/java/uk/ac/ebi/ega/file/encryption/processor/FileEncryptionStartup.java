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
package uk.ac.ebi.ega.file.encryption.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;
import uk.ac.ebi.ega.file.encryption.processor.models.EncryptJobParameters;
import uk.ac.ebi.ega.file.encryption.processor.services.IEncryptService;
import uk.ac.ebi.ega.jobs.core.JobExecution;
import uk.ac.ebi.ega.jobs.core.Result;

import java.util.Optional;

@Component
public class FileEncryptionStartup implements ApplicationListener<ApplicationReadyEvent> {

    private Logger logger = LoggerFactory.getLogger(FileEncryptionStartup.class);

    @Value("${spring.kafka.client-id}")
    private String instanceId;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private IEncryptService encryptService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        logger.info("File encryption instance-id {} started", instanceId);
        logger.info("Recovering last known status");

        restartLastAssignedJob();

        logger.info("File encryption instance-id {} starting kafka listener", instanceId);
        kafkaListenerEndpointRegistry.getListenerContainer(instanceId).start();
        logger.info("File encryption instance-id {} starting kafka started", instanceId);
    }

    private void restartLastAssignedJob() {
        final Optional<JobExecution<EncryptJobParameters>> job = encryptService.getUnfinishedJob();
        if (job.isPresent()) {
            final Result result = encryptService.encrypt(job.get());
            if (result.getStatus() == Result.Status.ABORTED) {
                logger.error("Process was aborted due to critical error. Unexpected application termination");
                SpringApplication.exit(applicationContext, () -> 1);
            }
        } else {
            logger.info("No process pending execution was found");
        }
    }
}
