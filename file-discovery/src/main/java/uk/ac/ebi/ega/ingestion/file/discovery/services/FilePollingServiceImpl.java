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
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;
import org.springframework.messaging.MessageChannel;
import uk.ac.ebi.ega.ingestion.file.discovery.models.FileInStaging;
import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingArea;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.StagingAreaService;
import uk.ac.ebi.ega.ingestion.file.discovery.utils.CustomRecursiveDirectoryScanner;

import javax.annotation.PostConstruct;
import java.io.File;

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
        stagingAreaService.findAll().forEach(this::registerFilePoller);
        logger.info("File poller registration finished successfully");
    }

    private void registerFilePoller(StagingArea stagingArea) {
        String flowName = getFlowName(stagingArea);
        MessageSource<File> source = buildFileReadingMessageSource(stagingArea.getPath());
        IntegrationFlow flow = getIntegrationFlow(source, stagingArea);
        integrationFlowContext.registration(flow)
                .id(stagingArea.getId())
                .useFlowIdAsPrefix()
                .autoStartup(false)
                .addBean(flowName, source)
                .register();

        logger.info("File poller registered as {}", stagingArea.getId());
    }

    private IntegrationFlow getIntegrationFlow(MessageSource<File> source, StagingArea stagingArea) {
        return IntegrationFlows
                .from(source,
                        c -> c.poller(Pollers.fixedDelay(stagingArea.getPollingPeriod(), 0)
                                .taskExecutor(taskExecutor))
                                .autoStartup(false))
                .transform((File file) -> new FileInStaging(stagingArea, file))
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

    private void stop(StagingArea stagingArea) {
        integrationFlowContext.getRegistrationById(stagingArea.getId()).stop();
    }

    private MessageSource<File> buildFileReadingMessageSource(String path) {
        CustomRecursiveDirectoryScanner scanner = new CustomRecursiveDirectoryScanner();
        scanner.setFilter(new IgnoreHiddenFileListFilter());
        FileReadingMessageSource source = new FileReadingMessageSource();
        source.setDirectory(new File(path));
        source.setScanner(scanner);
        return source;
    }

}
