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
package uk.ac.ebi.ega.ingestion.file.manager.services;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import io.micrometer.core.instrument.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.ega.ingestion.commons.messages.EncryptEvent;
import uk.ac.ebi.ega.ingestion.commons.messages.FileEncryptionData;
import uk.ac.ebi.ega.ingestion.commons.messages.FileEncryptionResult;
import uk.ac.ebi.ega.ingestion.commons.messages.FireArchiveResult;
import uk.ac.ebi.ega.ingestion.commons.messages.FireEvent;
import uk.ac.ebi.ega.ingestion.commons.messages.FireResponse;
import uk.ac.ebi.ega.ingestion.commons.messages.NewFileEvent;
import uk.ac.ebi.ega.ingestion.commons.models.FileStatus;
import uk.ac.ebi.ega.ingestion.commons.models.IFileDetails;
import uk.ac.ebi.ega.ingestion.commons.services.IEncryptedKeyService;
import uk.ac.ebi.ega.ingestion.file.manager.controller.exceptions.FileHierarchyException;
import uk.ac.ebi.ega.ingestion.file.manager.models.FileHierarchyModel;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.EncryptedObject;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileHierarchy;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.QEncryptedObject;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.EncryptedObjectRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.FileHierarchyRepository;
import uk.ac.ebi.ega.ingestion.file.manager.utils.FileStructureType;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileManagerService implements IFileManagerService {

    private final Logger LOGGER = LoggerFactory.getLogger(FileManagerService.class);

    private final Path fireBoxRelativePath;
    private final FileHierarchyRepository fileHierarchyRepository;
    private final EncryptedObjectRepository encryptedObjectRepository;
    private final String encryptEventTopic;
    private final KafkaTemplate<String, EncryptEvent> encryptEventKafkaTemplate;
    private final KafkaTemplate<String, FireEvent> fireEventKafkaTemplate;
    private final IEncryptedKeyService encryptedKeyService;
    private final String archiveTopic;

    public FileManagerService(final Path fireBoxRelativePath,
                              final FileHierarchyRepository fileHierarchyRepository,
                              final EncryptedObjectRepository encryptedObjectRepository,
                              final String encryptEventTopic,
                              final KafkaTemplate<String, EncryptEvent> encryptEventKafkaTemplate,
                              final KafkaTemplate<String, FireEvent> fireEventKafkaTemplate,
                              final IEncryptedKeyService encryptedKeyService,
                              final String archiveTopic) {
        this.fireBoxRelativePath = fireBoxRelativePath;
        this.fileHierarchyRepository = fileHierarchyRepository;
        this.encryptedObjectRepository = encryptedObjectRepository;
        this.encryptEventTopic = encryptEventTopic;
        this.encryptEventKafkaTemplate = encryptEventKafkaTemplate;
        this.fireEventKafkaTemplate = fireEventKafkaTemplate;
        this.encryptedKeyService = encryptedKeyService;
        this.archiveTopic = archiveTopic;
    }

    @Override
    @Transactional(transactionManager = "fileManager_transactionManager", rollbackFor = Exception.class)
    public void newFile(String key, NewFileEvent event) throws FileHierarchyException {
        /* More checks can be added if file/folder name has some restrictions.
           Regex checks for filenames, paths etc.
         */
        if (StringUtils.isEmpty(event.getUserPath())) {
            throw new FileHierarchyException("File path is invalid");
        }

        String encryptionKey = encryptedKeyService.generateNewEncryptedKey();
        LOGGER.error("Encryption key: {}", encryptionKey);
        LOGGER.error("Plain encryption key: {}", encryptedKeyService.decryptKey(encryptionKey));
        // Find if exists, otherwise create new object and save into database
        final EncryptedObject encryptedObject = encryptedObjectRepository
                .findByPathAndVersion(event.getUserPath(), event.getLastModified())
                .orElseGet(() -> fileHierarchyRepository.saveNewFile(
                        new EncryptedObject(
                                event.getAccountId(),
                                event.getLocationId(),
                                event.getUserPath(),
                                event.getLastModified(),
                                event.getPath().toUri().toString(),
                                event.getPlainMd5(),
                                -1,
                                event.getEncryptedMd5())).getEncryptedObject());

        if (encryptedObject.getStatus() == FileStatus.PROCESSING) {
            encryptEventKafkaTemplate.send(encryptEventTopic,
                    encryptedObject.getId().toString(),
                    EncryptEvent.ingest(event, encryptionKey));
            LOGGER.info("New file: {} version: {} has been added.", key, event.getLastModified());
        } else {
            LOGGER.info("New file: {} version: {} has already been processed", key, event.getLastModified());
        }
    }

    @Override
    @Transactional(transactionManager = "fileManager_transactionManager", rollbackFor = Exception.class)
    public void encrypted(final String key, final FileEncryptionResult result) {
        switch (result.getStatus()) {
            case SUCCESS:
                updateEncryptedDetails(key, result.getData());
                break;
            case FAILURE:
                // TODO what do we do?
                break;
            case MD5_ERROR:
                // TODO failure
                break;
        }
    }

    private void updateEncryptedDetails(final String key, final FileEncryptionData data) {
        final EncryptedObject encryptedObject = encryptedObjectRepository.findById(Long.parseLong(key)).get();
        if (encryptedObject.getStatus() == FileStatus.PROCESSING) {
            LOGGER.info("File: {} archiving is in progress", key);
            encryptedObject.archive(
                    data.getUri().toString(),
                    data.getEncryptedMD5(),
                    data.getPlainSize(),
                    data.getEncryptedSize(),
                    data.getEncryptionType(),
                    data.getEncryptionKey()
            );
            encryptedObjectRepository.save(encryptedObject);
            final FireEvent fireEvent = new FireEvent(
                    data.getUri(),
                    data.getEncryptedMD5(),
                    fireBoxRelativePath + "/" + encryptedObject.toFirePath()
            );
            fireEventKafkaTemplate.send(archiveTopic, key, fireEvent);
            LOGGER.info("Fire event key: {}, data: {}", key, fireEvent);
        } else {
            LOGGER.info("File: {} has already been processed", key);
        }
    }

    @Transactional(transactionManager = "fileManager_transactionManager", rollbackFor = Exception.class)
    @Override
    public void archived(final String key, final FireArchiveResult fireArchiveResult) {
        final EncryptedObject encryptedObject = encryptedObjectRepository.findById(Long.parseLong(key)).get();
        if (encryptedObject.getStatus() == FileStatus.ARCHIVE_IN_PROGRESS) {
            final FireResponse fireResponse = fireArchiveResult.getResponseData();
            encryptedObject.archived(fireResponse.getFireOid(), fireResponse.getFirePath());
            encryptedObjectRepository.save(encryptedObject);
        } else {
            LOGGER.info("File: {} has already been processed", key);
        }
    }

    @Override
    public List<FileHierarchyModel> findAllFilesAndFoldersInPath(final String accountId,
                                                                 final String stagingAreaId,
                                                                 final Optional<Path> filePath)
            throws FileNotFoundException {
        final List<FileHierarchyModel> fileHierarchyModels = filePath.map(Path::normalize)
                .map(Path::toString)
                .map(s -> fileHierarchyRepository.findOne(s, accountId, stagingAreaId)
                        .map(fileHierarchy -> {
                            if (fileHierarchy.getFileType() == FileStructureType.FILE) {
                                return Collections.singletonList(FileHierarchy.toModel(fileHierarchy));
                            } else {
                                return fileHierarchy.getChildPaths().stream().map(FileHierarchy::toModel)
                                        .collect(Collectors.toList());
                            }
                        })
                        .orElseGet(() -> new ArrayList<>()))
                .orElseGet(() -> fileHierarchyRepository
                        .findAllByAccountIdAndStagingAreaIdAndParentPathIsNullAllIgnoreCaseOrderByOriginalPath(
                                accountId, stagingAreaId)
                        .stream().map(FileHierarchy::toModel).collect(Collectors.toList()));
        if (!fileHierarchyModels.isEmpty()) {
            return fileHierarchyModels;
        }

        String stringPath = new String();
        if (filePath.isPresent()) {
            stringPath = filePath.toString();
        }
        throw new FileNotFoundException("/" + accountId + "/" + stagingAreaId + stringPath);

    }

    @Override
    public Optional<FileHierarchyModel> findParentOfPath(final String accountId, final String stagingAreaId,
                                                         final Path path) {
        return fileHierarchyRepository.findOne(path.normalize().toString(), accountId, stagingAreaId)
                .map(FileHierarchy::getParentPath)
                .map(FileHierarchy::toModel);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public Page<? extends IFileDetails> findAllFiles(final String accountId, final String stagingAreaId,
                                                     final Predicate predicate, final Pageable pageable) {
        final QEncryptedObject encryptedObject = QEncryptedObject.encryptedObject;
        final BooleanExpression completePredicate =
                encryptedObject.accountId.eq(accountId).and(encryptedObject.stagingId.eq(stagingAreaId)).and(predicate);
        Page<? extends IFileDetails> page = encryptedObjectRepository.findAll(completePredicate, pageable);
        return page;
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public Stream<? extends IFileDetails> findAllFiles(final String accountId,
                                                       final String stagingAreaId,
                                                       final Optional<String> optionalPath) {
        return optionalPath.map(path ->
                encryptedObjectRepository.findAllByAccountIdAndStagingIdAndPathStartingWithOrderByPath(
                        accountId, stagingAreaId, optionalPath.get()))
                .orElseGet(() ->
                        encryptedObjectRepository.findAllByAccountIdAndStagingIdOrderByPath(accountId, stagingAreaId)
                );
    }
}
