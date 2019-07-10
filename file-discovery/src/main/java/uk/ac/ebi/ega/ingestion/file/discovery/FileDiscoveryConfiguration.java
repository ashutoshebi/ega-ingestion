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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.kafka.dsl.Kafka;
import org.springframework.integration.kafka.dsl.KafkaProducerMessageHandlerSpec;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.DefaultKafkaHeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import uk.ac.ebi.ega.ingestion.commons.messages.FileEvent;
import uk.ac.ebi.ega.ingestion.commons.messages.IngestionEvent;
import uk.ac.ebi.ega.ingestion.file.discovery.message.handlers.PersistStagingFileChangesHandler;
import uk.ac.ebi.ega.ingestion.file.discovery.message.handlers.PersistStagingFileChangesHandlerImpl;
import uk.ac.ebi.ega.ingestion.file.discovery.services.FilePollingService;
import uk.ac.ebi.ega.ingestion.file.discovery.services.FilePollingServiceImpl;
import uk.ac.ebi.ega.ingestion.file.discovery.services.StagingAreaService;
import uk.ac.ebi.ega.ingestion.file.discovery.utils.StagingFileId;

import java.util.function.Function;

@Configuration
@EnableIntegration
public class FileDiscoveryConfiguration {

    @Value("${spring.kafka.file.events.queue.name}")
    private String fileEventQueueName;

    @Value("${spring.kafka.file.ingestion.queue.name}")
    private String fileIngestionQueueName;

    @Autowired
    private IntegrationFlowContext integrationFlowContext;

    @Autowired
    private StagingAreaService stagingAreaService;

    @Autowired
    private KafkaTemplate<Integer, FileEvent> fileEventKafkaTemplate;

    @Autowired
    private KafkaTemplate<Integer, IngestionEvent> fileIngestionKafkaTemplate;

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
                        .subscribe(f -> f.handle(discoveryMessageHandler())))
                .get();
    }

    @Bean
    public IntegrationFlow flowIngestion() {
        return IntegrationFlows.from(inboundIngestionChannel())
                .handle(ingestionMessageHandler())
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
    public KafkaProducerMessageHandlerSpec<Integer, FileEvent, ?> discoveryMessageHandler() {
        return Kafka.outboundChannelAdapter(fileEventKafkaTemplate)
                .messageKey(m -> m.getHeaders().get(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER))
                .headerMapper(mapper())
                .topicExpression(new LiteralExpression(fileEventQueueName));
    }

    @Bean
    public KafkaProducerMessageHandlerSpec<Integer, IngestionEvent, ?> ingestionMessageHandler() {
        return Kafka.outboundChannelAdapter(fileIngestionKafkaTemplate)
                .messageKey((Function<Message<IngestionEvent>, String>) m -> {
                    IngestionEvent event = m.getPayload();
                    return StagingFileId.calculateId(event.getLocationId(), event.getEncryptedFile().getAbsolutePath());
                })
                .headerMapper(mapper())
                .topicExpression(new LiteralExpression(fileIngestionQueueName));
    }

    @Bean
    public DefaultKafkaHeaderMapper mapper() {
        return new DefaultKafkaHeaderMapper();
    }

    @Bean
    public PersistStagingFileChangesHandler persistStagingFileChangesHandler() {
        return new PersistStagingFileChangesHandlerImpl(stagingAreaService);
    }

}
