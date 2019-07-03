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
package uk.ac.ebi.ega.ingestion.file.discovery.message.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.ingestion.file.discovery.services.StagingAreaService;
import uk.ac.ebi.ega.ingestion.commons.messages.FileEvent;

import java.util.List;

public class PersistStagingFileChangesHandlerImpl implements PersistStagingFileChangesHandler {

    private static final Logger logger = LoggerFactory.getLogger(PersistStagingFileChangesHandlerImpl.class);

    private StagingAreaService stagingAreaService;

    public PersistStagingFileChangesHandlerImpl(StagingAreaService stagingAreaService) {
        this.stagingAreaService = stagingAreaService;
    }

    @Override
    public void persistStagingFileChanges(List<FileEvent> fileEvents) {
        logger.warn("{}", fileEvents);
        stagingAreaService.update(fileEvents);
    }

}
