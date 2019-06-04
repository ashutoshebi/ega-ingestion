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
package uk.ac.ebi.ega.file.re.encryption.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.JobExecution;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;
import uk.ac.ebi.ega.file.re.encryption.processor.models.ReEncryptJobParameters;
import uk.ac.ebi.ega.file.re.encryption.processor.services.IReEncryptService;

import java.util.Optional;

@Component
public class FileReEncryptionStartup implements ApplicationListener<ApplicationReadyEvent> {

    private Logger logger = LoggerFactory.getLogger(FileReEncryptionStartup.class);

    @Value("${spring.kafka.client-id}")
    private String instanceId;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @Autowired
    private IReEncryptService reEncryptService;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
        logger.info("File encryption instance-id {} started", instanceId);
        restartLastAssignedJob();
        logger.info("File encryption instance-id {} starting kafka listener", instanceId);
        final MessageListenerContainer container = kafkaListenerEndpointRegistry.getListenerContainer(instanceId);
        container.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        container.start();
        logger.info("File encryption instance-id {} starting kafka started", instanceId);
    }

    private void restartLastAssignedJob() {
        final Optional<JobExecution<ReEncryptJobParameters>> job = reEncryptService.getUnfinishedJob();
        if (job.isPresent()) {
            final Result result = reEncryptService.reEncrypt(job.get());
            if (result.getStatus() == Result.Status.ABORTED) {
                logger.error("Process was aborted due to critical error. Unexpected application termination");
                SpringApplication.exit(applicationContext, () -> 1);
            }
        }else{
            logger.info("No process pending execution was found");
        }
    }
}
