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
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import uk.ac.ebi.ega.ingestion.file.discovery.message.handlers.PersistStagingFileChangesHandler;
import uk.ac.ebi.ega.ingestion.file.discovery.message.handlers.PersistStagingFileChangesHandlerImpl;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.StagingAreaService;
import uk.ac.ebi.ega.ingestion.file.discovery.services.FilePollingService;
import uk.ac.ebi.ega.ingestion.file.discovery.services.FilePollingServiceImpl;

@Configuration
@EnableIntegration
public class FileDiscoveryConfiguration {

    @Autowired
    private IntegrationFlowContext integrationFlowContext;

    @Autowired
    private StagingAreaService stagingAreaService;

    @Bean
    public MessageChannel inboundDiscoveryChannel() {
        return MessageChannels.direct().get();
    }

    @Bean
    public MessageChannel inboundIngestionChannel() {
        return MessageChannels.direct().get();
    }

    @Bean
    public FilePollingService filePollingService() {
        return new FilePollingServiceImpl(stagingAreaService, integrationFlowContext, fileDiscoveryExecutor(),
                inboundDiscoveryChannel(), fileIngestionExecutor(), inboundIngestionChannel());
    }

    @Bean
    public IntegrationFlow flowDiscoveryToDatabaseAndLogging() {
        return IntegrationFlows.from(inboundDiscoveryChannel())
                .publishSubscribeChannel(s -> s.applySequence(true)
                        .subscribe(f -> f.aggregate(aggregatorSpec -> aggregatorSpec.correlationStrategy(message -> true)
                                .releaseStrategy(releaseStrategy -> releaseStrategy.size() >= 5)
                                .sendPartialResultOnExpiry(true)
                                .groupTimeout(10000)
                                .expireGroupsUponCompletion(true)
                                .expireGroupsUponTimeout(true))
                                .handle(persistStagingFileChangesHandler()))
                        .subscribe(f -> f.handle(infoLoggingHandler())))
                .get();
    }

    @Bean
    public IntegrationFlow flowIngestion() {
        return IntegrationFlows.from(inboundIngestionChannel())
                .handle(errorLoggingHandler())
                .get();
    }

    @Bean
    public TaskExecutor fileDiscoveryExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(4);
        return taskExecutor;
    }

    @Bean
    public TaskExecutor fileIngestionExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(4);
        return taskExecutor;
    }

    @Bean
    public LoggingHandler infoLoggingHandler() {
        return new LoggingHandler(LoggingHandler.Level.INFO);
    }

    @Bean
    public LoggingHandler warnLoggingHandler() {
        return new LoggingHandler(LoggingHandler.Level.WARN);
    }

    @Bean
    public LoggingHandler errorLoggingHandler() {
        return new LoggingHandler(LoggingHandler.Level.ERROR);
    }

    @Bean
    public PersistStagingFileChangesHandler persistStagingFileChangesHandler() {
        return new PersistStagingFileChangesHandlerImpl(stagingAreaService);
    }

}
