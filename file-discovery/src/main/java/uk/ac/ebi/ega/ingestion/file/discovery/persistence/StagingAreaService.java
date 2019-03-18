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
package uk.ac.ebi.ega.ingestion.file.discovery.persistence;

import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.exceptions.StagingAreaNotFoundException;
import uk.ac.ebi.ega.ingestion.file.discovery.message.FileEvent;
import uk.ac.ebi.ega.ingestion.file.discovery.models.FileSystemNode;
import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingArea;
import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingFile;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.exceptions.StagingAreaAlreadyExistsException;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.repositories.StagingAreaImpl;

import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StagingAreaService {

    Optional<? extends StagingAreaImpl> findById(String boxId);

    Iterable<? extends StagingArea> findAll(Predicate predicate);

    Page<? extends StagingArea> findAll(Predicate predicate, Pageable pageable);

    FileSystemNode getFilesOfStagingArea(String stagingAreaId);

    void update(List<FileEvent> fileEvents);

    Iterable<? extends StagingFile> findAllFilesByStagingId(String stagingId);

    Iterable<? extends StagingFile> findAllFilesOfStagingAreaOlderThan(String stagingId, LocalDateTime cutOffDate);

    Page<? extends StagingFile> findAllFilesByStagingId(String stagingId, Predicate predicate,
                                                        Pageable pageable);

    StagingArea newStagingArea(StagingArea stagingArea) throws FileNotFoundException, StagingAreaAlreadyExistsException;

    StagingArea updateStagingArea(String stagingId, Boolean discoveryEnabled, Boolean ingestionEnabled,
                                  Long discoveryPollingPeriod, Long ingestionPollingPeriod) throws StagingAreaNotFoundException;
}
