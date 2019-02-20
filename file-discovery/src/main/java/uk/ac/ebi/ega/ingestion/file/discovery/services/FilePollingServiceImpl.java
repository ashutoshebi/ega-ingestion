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
import org.springframework.messaging.MessageChannel;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.exceptions.StagingAreaNotFoundException;
import uk.ac.ebi.ega.ingestion.file.discovery.message.FileEvent;
import uk.ac.ebi.ega.ingestion.file.discovery.message.sources.file.event.FileEventMessageSource;
import uk.ac.ebi.ega.ingestion.file.discovery.message.sources.file.event.FileEventRecursiveDirectoryScanner;
import uk.ac.ebi.ega.ingestion.file.discovery.message.sources.file.event.FileStatic;
import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingArea;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.StagingAreaService;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.exceptions.StagingAreaAlreadyExistsException;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class FilePollingServiceImpl implements FilePollingService {

    private final static Logger logger = LoggerFactory.getLogger(FilePollingServiceImpl.class);

    private StagingAreaService stagingAreaService;

    private IntegrationFlowContext integrationFlowContext;

    private TaskExecutor taskExecutor;

    private MessageChannel inboundChannel;

    public FilePollingServiceImpl(StagingAreaService stagingAreaService, IntegrationFlowContext integrationFlowContext,
                                  TaskExecutor taskExecutor, MessageChannel inboundChannel) {
        this.stagingAreaService = stagingAreaService;
        this.integrationFlowContext = integrationFlowContext;
        this.taskExecutor = taskExecutor;
        this.inboundChannel = inboundChannel;
    }

    @PostConstruct
    private void init() {
        logger.info("File poller registration");
        stagingAreaService.findAll(null, Pageable.unpaged()).forEach(this::registerFilePoller);
        logger.info("File poller registration finished successfully");
    }

    private StagingArea registerFilePoller(StagingArea stagingArea) {
        String flowName = getFlowName(stagingArea);
        MessageSource<FileEvent> source = buildFileReadingMessageSource(stagingArea);
        IntegrationFlow flow = getIntegrationFlow(source, stagingArea);
        integrationFlowContext.registration(flow)
                .id(stagingArea.getId())
                .useFlowIdAsPrefix()
                .autoStartup(false)
                .addBean(flowName, source)
                .register();
        logger.info("File poller registered as {}", stagingArea.getId());
        return stagingArea;
    }

    private IntegrationFlow getIntegrationFlow(MessageSource<FileEvent> source, StagingArea stagingArea) {
        return IntegrationFlows
                .from(source,
                        c -> c.poller(Pollers.fixedDelay(stagingArea.getDiscoveryPollingPeriod(), 0)
                                .taskExecutor(taskExecutor))
                                .autoStartup(false))
                .channel(inboundChannel)
                .get();
    }

    private String getFlowName(StagingArea stagingArea) {
        return stagingArea.getId() + "-messageSource";
    }

    @Override
    public void startAllEnabled() {
        stagingAreaService.findAllEnabled().forEach(this::start);
    }

    private void start(StagingArea stagingArea) {
        integrationFlowContext.getRegistrationById(stagingArea.getId()).start();
    }

    @Override
    public void stopAll() {
        stagingAreaService.findAllEnabled().forEach(this::stop);
    }

    @Override
    public StagingArea newStagingArea(StagingArea stagingArea) throws FileNotFoundException,
            StagingAreaAlreadyExistsException {
        return registerFilePoller(stagingAreaService.newStagingArea(stagingArea));
    }

    @Override
    public StagingArea updateStagingArea(String stagingId, Boolean discoveryEnabled, Boolean ingestionEnabled,
                                         Long discoveryPollingPeriod, Long ingestionPollingPeriod)
            throws StagingAreaNotFoundException {
        StagingArea stagingArea = stagingAreaService.updateStagingArea(stagingId, discoveryEnabled, ingestionEnabled,
                discoveryPollingPeriod, ingestionPollingPeriod);
        deregisterFilePoller(stagingArea);
        registerFilePoller(stagingArea);
        if (stagingArea.isDiscoveryEnabled()) {
            start(stagingArea);
        }
        return stagingArea;
    }

    private void deregisterFilePoller(StagingArea stagingArea) {
        integrationFlowContext.getRegistrationById(stagingArea.getId()).destroy();
        logger.info("File poller {} destroyed", stagingArea.getId());
    }

    private void stop(StagingArea stagingArea) {
        integrationFlowContext.getRegistrationById(stagingArea.getId()).stop();
    }

    private MessageSource<FileEvent> buildFileReadingMessageSource(StagingArea stagingArea) {
        FileEventRecursiveDirectoryScanner scanner = new FileEventRecursiveDirectoryScanner();
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
