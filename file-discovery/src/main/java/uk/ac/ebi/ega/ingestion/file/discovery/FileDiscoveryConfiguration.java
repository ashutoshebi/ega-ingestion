/*
 *
 * Copyright 2018 EMBL - European Bioinformatics Institute
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
package uk.ac.ebi.ega.ingestion.file.discovery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import uk.ac.ebi.ega.ingestion.file.discovery.handlers.FileProcessesHandler;
import uk.ac.ebi.ega.ingestion.file.discovery.handlers.FileProcessesHandlerImpl;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.StagingAreaService;
import uk.ac.ebi.ega.ingestion.file.discovery.services.FilePollingService;
import uk.ac.ebi.ega.ingestion.file.discovery.services.FilePollingServiceImpl;

@Configuration
@EnableIntegration
public class FileDiscoveryConfiguration {

    @Autowired
    private IntegrationFlowContext integrationFlowContext;

    @Bean
    public MessageChannel inboundFilePollingChannel() {
        return MessageChannels.direct().get();
    }

    @Bean
    public FilePollingService filePollingService(StagingAreaService stagingAreaService) {
        return new FilePollingServiceImpl(stagingAreaService, integrationFlowContext, taskExecutor(),
                inboundFilePollingChannel());
    }

    @Bean
    public IntegrationFlow changeUser() {
        return IntegrationFlows.from(inboundFilePollingChannel())
                .log()
                .handle(FileProcessesHandler())
                .get();
    }

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(4);
        return taskExecutor;
    }

    @Bean
    public FileProcessesHandler FileProcessesHandler(){
        return new FileProcessesHandlerImpl();
    }

}
