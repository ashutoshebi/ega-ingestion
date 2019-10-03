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
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.fire.ingestion.service.IFireService;
import uk.ac.ebi.ega.ingestion.commons.messages.ArchiveEvent;
import uk.ac.ebi.ega.ingestion.commons.messages.EncryptEvent;
import uk.ac.ebi.ega.ingestion.commons.messages.NewFileEvent;
import uk.ac.ebi.ega.ingestion.commons.models.FileStatus;
import uk.ac.ebi.ega.ingestion.commons.models.IFileDetails;
import uk.ac.ebi.ega.ingestion.commons.services.IEncryptedKeyService;
import uk.ac.ebi.ega.ingestion.file.manager.controller.exceptions.FileHierarchyException;
import uk.ac.ebi.ega.ingestion.file.manager.models.ArchivedFile;
import uk.ac.ebi.ega.ingestion.file.manager.models.FileHierarchyModel;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.EncryptedObject;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileHierarchy;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.QEncryptedObject;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.EncryptedObjectRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.FileHierarchyRepository;
import uk.ac.ebi.ega.ingestion.file.manager.utils.FileStructureType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileManagerService implements IFileManagerService {

    private final Logger LOGGER = LoggerFactory.getLogger(FileManagerService.class);

    private final IFireService fireService;
    private final Path fireBoxRelativePath;
    private final FileHierarchyRepository fileHierarchyRepository;
    private final EncryptedObjectRepository encryptedObjectRepository;

    private final String encryptEventTopic;
    private final KafkaTemplate<String, EncryptEvent> encryptEventKafkaTemplate;

    private final IEncryptedKeyService encryptedKeyService;

    public FileManagerService(final IFireService fireService,
                              final Path fireBoxRelativePath,
                              final FileHierarchyRepository fileHierarchyRepository,
                              final EncryptedObjectRepository encryptedObjectRepository,
                              final String encryptEventTopic,
                              final KafkaTemplate<String, EncryptEvent> encryptEventKafkaTemplate,
                              final IEncryptedKeyService encryptedKeyService) {
        this.fireService = fireService;
        this.fireBoxRelativePath = fireBoxRelativePath;
        this.fileHierarchyRepository = fileHierarchyRepository;
        this.encryptedObjectRepository = encryptedObjectRepository;
        this.encryptEventTopic = encryptEventTopic;
        this.encryptEventKafkaTemplate = encryptEventKafkaTemplate;
        this.encryptedKeyService = encryptedKeyService;
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
    @Transactional(transactionManager = "fileManagerFireChainedTransactionManager", rollbackFor = Exception.class)
    public void archive(final ArchiveEvent archiveEvent) throws IOException, FileHierarchyException {
        // TODO EE-888 update this part
        final char[] password = FileUtils.readPasswordFile(Paths.get(archiveEvent.getKeyPath()));

        Path relativePathInFire = fireBoxRelativePath.resolve(archiveEvent.getStagingAreaId());
        final Optional<Long> fireId = fireService.archiveFile(null, new File(archiveEvent.getStagingPath()),
                archiveEvent.getEncryptedMd5(), relativePathInFire.toString());
        Path completePathInFire = relativePathInFire.resolve(new File(archiveEvent.getStagingPath()).getName());

        ArchivedFile fileToBeArchived = new ArchivedFile(
                archiveEvent.getAccountId(),
                archiveEvent.getStagingAreaId(),
                fireId.get(),
                completePathInFire.toString(),
                archiveEvent.getOriginalPath(),
                archiveEvent.getPlainSize(),
                archiveEvent.getPlainMd5(),
                archiveEvent.getEncryptedSize(),
                archiveEvent.getEncryptedMd5(),
                password
        );
    }

    @Override
    public List<FileHierarchyModel> findAllFilesAndFoldersInPathNonRecursive(final String accountId,
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
                        }).orElseGet(() -> new ArrayList<>()))
                .orElseGet(() -> fileHierarchyRepository.findAllFilesAndFoldersInPathNonRecursive(accountId,
                        stagingAreaId).stream().map(FileHierarchy::toModel).collect(Collectors.toList()));
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
    public Optional<FileHierarchyModel> findParentOfPath(String accountId, String stagingAreaId, Path path) {
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
                                                     final Predicate predicate, final Pageable pageable) throws FileNotFoundException {
        final QEncryptedObject encryptedObject = QEncryptedObject.encryptedObject;
        final BooleanExpression completePredicate =
                encryptedObject.accountId.eq(accountId).and(encryptedObject.stagingId.eq(stagingAreaId)).and(predicate);
        Page<? extends IFileDetails> page = encryptedObjectRepository.findAll(completePredicate, pageable);

        if (page.isEmpty()) {
            throw new FileNotFoundException();
        }

        return page;
    }
//
//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public Stream<FileHierarchyModel> findAllFilesInPathNonRecursive(final String accountId, final String stagingAreaId,
//                                                                     final Path filePath) throws FileNotFoundException {
//        if (StringUtils.isEmpty(filePath.toString())) {
//            return fileHierarchyRepository.findAllFilesInPathNonRecursive(accountId, stagingAreaId).
//                    map(fileHierarchyFileTypeMapEntityToModel());
//        }
//
//        final Optional<FileHierarchy> optionalFileHierarchy = fileHierarchyRepository.findOne(filePath.normalize().toString(), accountId, stagingAreaId);
//        final FileHierarchy fileHierarchy = optionalFileHierarchy.orElseThrow(FileNotFoundException::new);
//
//        if (FileStructureType.FILE.equals(fileHierarchy.getFileType())) {
//            return Stream.of(fileHierarchy.toFile());
//        }
//        return fileHierarchyRepository.findAllFilesOrFoldersInRootPathNonRecursive(accountId, stagingAreaId, fileHierarchy.getId(), FileStructureType.FILE).
//                map(fileHierarchyFileTypeMapEntityToModel());
//    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public Stream<? extends IFileDetails> findAllFiles(final String accountId,
                                                       final String stagingAreaId,
                                                       Optional<String> optionalPath) {
        if (!optionalPath.isPresent()) {
            return encryptedObjectRepository.findAllByAccountIdAndStagingId(accountId, stagingAreaId);
        } else {
            return encryptedObjectRepository.findAllByAccountIdAndStagingIdAndPathStartingWith(
                    accountId, stagingAreaId, optionalPath.get());
        }
    }

}
