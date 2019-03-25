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
package uk.ac.ebi.ega.ingestion.file.discovery.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Pageable;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;
import org.springframework.messaging.MessageChannel;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.exceptions.StagingAreaNotFoundException;
import uk.ac.ebi.ega.ingestion.file.discovery.message.FileEvent;
import uk.ac.ebi.ega.ingestion.file.discovery.message.IngestionEvent;
import uk.ac.ebi.ega.ingestion.file.discovery.message.sources.file.event.CompositeAbstractFileListFilter;
import uk.ac.ebi.ega.ingestion.file.discovery.message.sources.file.event.DirectoryPatternFileListFilter;
import uk.ac.ebi.ega.ingestion.file.discovery.message.sources.file.event.FileEventMessageSource;
import uk.ac.ebi.ega.ingestion.file.discovery.message.sources.file.event.FileEventRecursiveDirectoryScanner;
import uk.ac.ebi.ega.ingestion.file.discovery.message.sources.file.event.FileStatic;
import uk.ac.ebi.ega.ingestion.file.discovery.message.sources.ingestion.IngestionMessageSource;
import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingArea;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.exceptions.StagingAreaAlreadyExistsException;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;

public class FilePollingServiceImpl implements FilePollingService {

    private final static Logger logger = LoggerFactory.getLogger(FilePollingServiceImpl.class);

    private StagingAreaService stagingAreaService;

    private IntegrationFlowContext integrationFlowContext;

    private TaskExecutor fileDiscoveryExecutor;

    private TaskExecutor fileIngestionExecutor;

    private MessageChannel inboundDiscoveryChannel;

    private MessageChannel inboundIngestionChannel;

    public FilePollingServiceImpl(StagingAreaService stagingAreaService, IntegrationFlowContext integrationFlowContext,
                                  TaskExecutor fileDiscoveryExecutor, MessageChannel inboundDiscoveryChannel,
                                  TaskExecutor fileIngestionExecutor, MessageChannel inboundIngestionChannel) {
        this.stagingAreaService = stagingAreaService;
        this.integrationFlowContext = integrationFlowContext;
        this.fileDiscoveryExecutor = fileDiscoveryExecutor;
        this.fileIngestionExecutor = fileIngestionExecutor;
        this.inboundDiscoveryChannel = inboundDiscoveryChannel;
        this.inboundIngestionChannel = inboundIngestionChannel;
    }

    @PostConstruct
    private void init() {
        logger.info("File poller registration");
        stagingAreaService.findAll(null, Pageable.unpaged()).forEach(this::register);
        logger.info("File poller registration finished successfully");
    }

    private void register(StagingArea stagingArea) {
        registerFileDiscovery(stagingArea);
        registerFileIngestion(stagingArea);
    }

    private void registerFileIngestion(StagingArea stagingArea) {
        String messageSourceName = getIngestionMessageSourceName(stagingArea);
        String flowId = getIngestionFlowId(stagingArea);
        registerPollFlow(buildFileIngestionMessageSource(stagingArea), stagingArea.getIngestionPollingPeriod(),
                messageSourceName, flowId, fileIngestionExecutor, inboundIngestionChannel);
        logger.info("File ingestion for staging area {} registered as {}", stagingArea.getId(), flowId);
    }

    private String getIngestionMessageSourceName(StagingArea stagingArea) {
        return "ingestion-messageSource-" + stagingArea.getId();
    }

    private String getIngestionFlowId(StagingArea stagingArea) {
        return "ingestion-flow-" + stagingArea.getId();
    }

    private MessageSource<IngestionEvent> buildFileIngestionMessageSource(StagingArea stagingArea) {
        IngestionMessageSource ingestionMessageSource =
                new IngestionMessageSource(stagingAreaService::findAllFilesOfStagingAreaOlderThan);
        ingestionMessageSource.setLocationId(stagingArea.getId());
        ingestionMessageSource.setAccountId(stagingArea.getAccount());
        ingestionMessageSource.setDirectory(Paths.get(stagingArea.getPath()));
        return ingestionMessageSource;
    }

    private void registerFileDiscovery(StagingArea stagingArea) {
        String messageSourceName = getDiscoveryMessageSourceName(stagingArea);
        String flowId = getDiscoveryFlowId(stagingArea);
        registerPollFlow(buildFileEventMessageSource(stagingArea), stagingArea.getDiscoveryPollingPeriod(),
                messageSourceName, flowId, fileDiscoveryExecutor, inboundDiscoveryChannel);
        logger.info("File discovery for staging area {} registered as {}", stagingArea.getId(), flowId);
    }

    private String getDiscoveryMessageSourceName(StagingArea stagingArea) {
        return "discovery-messageSource-" + stagingArea.getId();
    }

    private String getDiscoveryFlowId(StagingArea stagingArea) {
        return "discovery-flow-" + stagingArea.getId();
    }

    private void registerPollFlow(MessageSource<?> source, long period, String messageSourceName, String flowId,
                                  Executor executor, MessageChannel messageChannel) {
        integrationFlowContext.registration(getPoller(source, period, executor, messageChannel))
                .id(flowId)
                .useFlowIdAsPrefix()
                .autoStartup(false)
                .addBean(messageSourceName, source)
                .register();
    }

    private IntegrationFlow getPoller(MessageSource<?> source, long period, Executor taskExecutor,
                                      MessageChannel channel) {
        return IntegrationFlows
                .from(source,
                        c -> c.poller(Pollers.fixedDelay(period, 0)
                                .taskExecutor(taskExecutor))
                                .autoStartup(false))
                .channel(channel)
                .get();
    }

    @Override
    public void startAllEnabled() {
        stagingAreaService.findAll(null, Pageable.unpaged()).forEach(this::start);
    }

    private void start(StagingArea stagingArea) {
        if (stagingArea.isDiscoveryEnabled()) {
            getDiscoveryFlow(stagingArea).ifPresent(flow -> {
                flow.start();
                logger.info("File discovery for staging area {} started", stagingArea.getId());
            });
        }
        if (stagingArea.isIngestionEnabled()) {
            getIngestionFlow(stagingArea).ifPresent(flow -> {
                flow.start();
                logger.info("File ingestion for staging area {} started", stagingArea.getId());
            });
        }
    }

    private Optional<IntegrationFlowContext.IntegrationFlowRegistration> getIngestionFlow(StagingArea stagingArea) {
        return Optional.ofNullable(integrationFlowContext.getRegistrationById(getIngestionFlowId(stagingArea)));
    }

    @Override
    public void stopAll() {
        stagingAreaService.findAll(null, Pageable.unpaged()).forEach(this::stop);
    }

    @Override
    public StagingArea newStagingArea(StagingArea stagingArea) throws FileNotFoundException,
            StagingAreaAlreadyExistsException {
        return stagingAreaService.newStagingArea(stagingArea);
    }

    @Override
    public StagingArea updateStagingArea(String stagingId, Boolean discoveryEnabled, Boolean ingestionEnabled,
                                         Long discoveryPollingPeriod, Long ingestionPollingPeriod)
            throws StagingAreaNotFoundException {
        StagingArea stagingArea = stagingAreaService.updateStagingArea(stagingId, discoveryEnabled, ingestionEnabled,
                discoveryPollingPeriod, ingestionPollingPeriod);
        deregister(stagingArea);
        register(stagingArea);
        start(stagingArea);
        return stagingArea;
    }

    private void deregister(StagingArea stagingArea) {
        getDiscoveryFlow(stagingArea).ifPresent(flow -> {
            flow.destroy();
            logger.info("File poller for staging area {} destroyed", stagingArea.getId());
        });
        getIngestionFlow(stagingArea).ifPresent(flow -> {
            flow.destroy();
            logger.info("File ingestion for staging area {} destroyed", stagingArea.getId());
        });
    }

    private Optional<IntegrationFlowContext.IntegrationFlowRegistration> getDiscoveryFlow(StagingArea stagingArea) {
        return Optional.ofNullable(integrationFlowContext.getRegistrationById(getDiscoveryFlowId(stagingArea)));
    }

    private void stop(StagingArea stagingArea) {
        integrationFlowContext.getRegistrationById(stagingArea.getId()).stop();
    }

    private MessageSource<FileEvent> buildFileEventMessageSource(StagingArea stagingArea) {
        FileEventRecursiveDirectoryScanner scanner = new FileEventRecursiveDirectoryScanner();
        scanner.setFilter(new CompositeAbstractFileListFilter(new IgnoreHiddenFileListFilter(),
                new DirectoryPatternFileListFilter(stagingArea.getIgnorePathRegex())));
        scanner.initializeDirectoryStatus(getFilesOfStagingArea(stagingArea));
        FileEventMessageSource source = new FileEventMessageSource(scanner);
        source.setAutoCreateDirectory(false);
        source.setLocationId(stagingArea.getId());
        source.setDirectory(new File(stagingArea.getPath()));
        return source;
    }

    private Map<String, FileStatic> getFilesOfStagingArea(StagingArea stagingArea) {
        Map<String, FileStatic> files = new HashMap<>();
        stagingAreaService.findAllFilesByStagingId(stagingArea.getId()).forEach(o -> {
            final String absolutePath = Paths.get(stagingArea.getPath(), o.getRelativePath()).toString();
            files.put(absolutePath, new FileStatic(absolutePath, o.getFileSize(),
                    o.getUpdateDate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        });
        return files;
    }

}
