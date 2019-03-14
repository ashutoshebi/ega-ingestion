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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.exceptions.StagingAreaNotFoundException;
import uk.ac.ebi.ega.ingestion.file.discovery.message.FileEvent;
import uk.ac.ebi.ega.ingestion.file.discovery.models.FileSystemNode;
import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingArea;
import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingFile;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.exceptions.StagingAreaAlreadyExistsException;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.repositories.StagingAreaImpl;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.repositories.StagingAreaRepository;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.repositories.StagingFileImpl;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.repositories.StagingFileRepository;
import uk.ac.ebi.ega.ingestion.file.discovery.services.StagingAreaService;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class StagingAreaServiceImpl implements StagingAreaService {

    private final Logger logger = LoggerFactory.getLogger(StagingAreaServiceImpl.class);

    private NamedParameterJdbcTemplate jdbcTemplate;

    private StagingAreaRepository repository;

    private StagingFileRepository fileRepository;

    public StagingAreaServiceImpl(NamedParameterJdbcTemplate jdbcTemplate, StagingAreaRepository repository,
                                  StagingFileRepository fileRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.repository = repository;
        this.fileRepository = fileRepository;
    }

    @Override
    public Optional<StagingAreaImpl> findById(String boxId) {
        return repository.findById(boxId);
    }

    @Override
    public Page<? extends StagingArea> findAll(Predicate predicate, Pageable pageable) {
        return repository.findAll(predicate, pageable);
    }

    @Override
    public Iterable<? extends StagingArea> findAll(Predicate predicate) {
        return repository.findAll(predicate);
    }

    @Override
    public FileSystemNode getFilesOfStagingArea(String stagingAreaId) {
        final FileSystemNode root = FileSystemNode.root(stagingAreaId);
        root.addChildren(fileRepository.findAllByStagingAreaId(stagingAreaId, null, Pageable.unpaged()));
        return root;
    }

    @Override
    public Page<? extends StagingFile> findAllFilesByStagingId(String stagingId, Predicate predicate,
                                                               Pageable pageable) {
        return fileRepository.findAllByStagingAreaId(stagingId, predicate, pageable);
    }

    @Override
    public StagingArea newStagingArea(StagingArea stagingArea) throws FileNotFoundException,
            StagingAreaAlreadyExistsException {
        if (!new File(stagingArea.getPath()).exists()) {
            throw new FileNotFoundException();
        }
        if (repository.findById(stagingArea.getId()).isPresent()) {
            throw new StagingAreaAlreadyExistsException();
        }
        StagingAreaImpl entity = new StagingAreaImpl(true);
        BeanUtils.copyProperties(stagingArea, entity);
        return repository.save(entity);
    }

    @Override
    public StagingArea updateStagingArea(String stagingId, Boolean discoveryEnabled, Boolean ingestionEnabled,
                                         Long discoveryPollingPeriod, Long ingestionPollingPeriod)
            throws StagingAreaNotFoundException {
        StagingAreaImpl stagingArea = repository.findById(stagingId).orElseThrow(StagingAreaNotFoundException::new);
        if (discoveryEnabled != null) {
            stagingArea.setDiscoveryEnabled(discoveryEnabled);
        }
        if (ingestionEnabled != null) {
            stagingArea.setIngestionEnabled(discoveryEnabled);
        }
        if (discoveryPollingPeriod != null) {
            stagingArea.setDiscoveryPollingPeriod(discoveryPollingPeriod);
        }
        if (ingestionPollingPeriod != null) {
            stagingArea.setIngestionPollingPeriod(ingestionPollingPeriod);
        }
        return repository.save(stagingArea);
    }

    @Override
    public void update(List<FileEvent> fileEvents) {
        HashMap<String, StagingFileImpl> create = new HashMap<>();
        HashMap<String, StagingFileImpl> update = new HashMap<>();
        Set<String> delete = new HashSet<>();

        fileEvents.forEach(fileEvent -> {
            final StagingFileImpl stagingAreaFile = new StagingFileImpl(fileEvent);
            switch (fileEvent.getType()) {
                case CREATED:
                    create.put(stagingAreaFile.getId(), stagingAreaFile);
                    break;
                case UPDATED:
                    if (create.containsKey(stagingAreaFile.getId())) {
                        create.put(stagingAreaFile.getId(), stagingAreaFile);
                    } else {
                        update.put(stagingAreaFile.getId(), stagingAreaFile);
                    }
                    break;
                case DELETED:
                    update.remove(stagingAreaFile.getId());
                    if (create.containsKey(stagingAreaFile.getId())) {
                        create.remove(stagingAreaFile.getId());
                    } else {
                        delete.add(stagingAreaFile.getId());
                    }
                    break;
                default:
                    logger.error("File event {} could not be persisted", fileEvent);
            }
        });

        if (!create.isEmpty()) insertAreaFiles(new ArrayList<>(create.values()));
        if (!update.isEmpty()) updateAreaFiles(new ArrayList<>(update.values()));
        if (!delete.isEmpty()) deleteAreaFiles(new ArrayList<>(delete));
    }

    @Override
    public Iterable<? extends StagingFile> findAllFilesByStagingId(String stagingId) {
        return fileRepository.findAllByStagingAreaId(stagingId);
    }

    @Override
    public Iterable<? extends StagingFile> findAllFilesOfStagingAreaOlderThan(String stagingId, LocalDateTime cutOff) {
        return fileRepository.findAllByStagingAreaIdOlderThan(stagingId, cutOff);
    }

    private int[] insertAreaFiles(List<StagingFileImpl> stagingAreaFiles) {
        final SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(stagingAreaFiles);
        return jdbcTemplate.batchUpdate("insert into STAGING_AREA_FILES (ID, STAGING_AREA_ID, RELATIVE_PATH, " +
                "FILE_SIZE, UPDATE_DATE ) values (:id, :stagingAreaId, :relativePath, :fileSize, :updateDate);", batch);
    }

    private int[] updateAreaFiles(List<StagingFileImpl> stagingAreaFiles) {
        final SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(stagingAreaFiles);
        return jdbcTemplate.batchUpdate("update STAGING_AREA_FILES set STAGING_AREA_ID = :stagingAreaId, " +
                "RELATIVE_PATH = :relativePath, FILE_SIZE = :fileSize, UPDATE_DATE = :updateDate " +
                "where ID = :id;", batch);
    }

    private int deleteAreaFiles(List<String> ids) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("ids", ids);
        return jdbcTemplate.update("delete from STAGING_AREA_FILES where ID in(:ids);", paramMap);
    }

}
