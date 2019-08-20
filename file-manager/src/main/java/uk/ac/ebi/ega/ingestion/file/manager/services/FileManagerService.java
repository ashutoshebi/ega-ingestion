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

import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import io.micrometer.core.instrument.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.fire.ingestion.service.IFireService;
import uk.ac.ebi.ega.ingestion.commons.messages.ArchiveEvent;
import uk.ac.ebi.ega.ingestion.file.manager.controller.exceptions.FileHierarchyException;
import uk.ac.ebi.ega.ingestion.file.manager.models.ArchivedFile;
import uk.ac.ebi.ega.ingestion.file.manager.models.FileHierarchyModel;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileDetails;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileHierarchy;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileStatus;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.QFileHierarchy;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.FileHierarchyRepository;
import uk.ac.ebi.ega.ingestion.file.manager.utils.FileStructureType;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileManagerService implements IFileManagerService {

    private final Logger LOGGER = LoggerFactory.getLogger(FileManagerService.class);

    private final IFireService fireService;
    private final Path fireBoxRelativePath;
    private final FileHierarchyRepository fileHierarchyRepository;
    private final EntityManager entityManager;

    public FileManagerService(final IFireService fireService,
                              final Path fireBoxRelativePath,
                              final FileHierarchyRepository fileHierarchyRepository,
                              final EntityManager entityManager) {
        this.fireService = fireService;
        this.fireBoxRelativePath = fireBoxRelativePath;
        this.fileHierarchyRepository = fileHierarchyRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(transactionManager = "fileManagerFireChainedTransactionManager", rollbackFor = Exception.class)
    public void archive(final ArchiveEvent archiveEvent) throws IOException, FileHierarchyException {
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

        addFile(fileToBeArchived);
    }

    @Override
    public List<FileHierarchyModel> findAllFilesAndFoldersInPathNonRecursive(final String accountId, final String stagingAreaId,
                                                                             final Path filePath) throws FileNotFoundException {
        if (StringUtils.isEmpty(filePath.toString())) {
            return fileHierarchyRepository.findAllFilesAndFoldersInPathNonRecursive(accountId, stagingAreaId).stream().
                    map(fileHierarchyFileAndFolderTypeMapEntityToModel()).
                    collect(Collectors.toList());
        }

        final Optional<FileHierarchy> optionalFileHierarchy = fileHierarchyRepository.findOne(filePath.normalize().toString(),
                accountId, stagingAreaId);
        final FileHierarchy fileHierarchy = optionalFileHierarchy.orElseThrow(FileNotFoundException::new);

        if (FileStructureType.FILE.equals(fileHierarchy.getFileType())) {
            return Collections.singletonList(fileHierarchy.toFile());
        }
        return fileHierarchy.getChildPaths().stream().map(fileHierarchyFileAndFolderTypeMapEntityToModel()).
                collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<FileHierarchyModel> findAllFilesInRootPathRecursive(final String accountId, final String stagingAreaId,
                                                                    final Predicate predicate, final Pageable pageable) throws FileNotFoundException {
        final Predicate filePredicate = Expressions.allOf(Expressions.predicate(Ops.EQ, QFileHierarchy.fileHierarchy.fileType,
                Expressions.constant(FileStructureType.FILE))).and(predicate);

        final Page<FileHierarchy> fileHierarchyPage = fileHierarchyRepository.findAllFilesInRootPathRecursive(accountId, stagingAreaId, filePredicate, pageable);

        if (!fileHierarchyPage.hasContent()) {
            throw new FileNotFoundException();
        }
        return new PageImpl<>(fileHierarchyPage.stream().map(FileHierarchy::toFile).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<FileHierarchyModel> findAllFilesInPathNonRecursive(final String accountId, final String stagingAreaId,
                                                                     final Path filePath) throws FileNotFoundException {
        if (StringUtils.isEmpty(filePath.toString())) {
            return fileHierarchyRepository.findAllFilesInPathNonRecursive(accountId, stagingAreaId).
                    map(fileHierarchyFileTypeMapEntityToModel());
        }

        final Optional<FileHierarchy> optionalFileHierarchy = fileHierarchyRepository.findOne(filePath.normalize().toString(), accountId, stagingAreaId);
        final FileHierarchy fileHierarchy = optionalFileHierarchy.orElseThrow(FileNotFoundException::new);

        if (FileStructureType.FILE.equals(fileHierarchy.getFileType())) {
            return Stream.of(fileHierarchy.toFile());
        }
        return fileHierarchyRepository.findAllFilesOrFoldersInRootPathNonRecursive(accountId, stagingAreaId, fileHierarchy.getId(), FileStructureType.FILE).
                map(fileHierarchyFileTypeMapEntityToModel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<FileHierarchyModel> findAllFilesInRootPathRecursive(final String accountId, final String stagingAreaId) {
        return fileHierarchyRepository.findAllFilesOrFoldersInRootPathRecursive(accountId, stagingAreaId, FileStructureType.FILE).
                map(fileHierarchyFileTypeMapEntityToModel());
    }

    /**
     * After each mapping FileHierarchy object is being detached from current Hibernate session.
     *
     * @return Entity to Model mapping function.
     */
    private Function<FileHierarchy, FileHierarchyModel> fileHierarchyFileTypeMapEntityToModel() {
        return fileHierarchy -> {
            final FileHierarchyModel fileHierarchyModel = fileHierarchy.toFile();
            entityManager.detach(fileHierarchy);
            return fileHierarchyModel;
        };
    }

    private Function<FileHierarchy, FileHierarchyModel> fileHierarchyFileAndFolderTypeMapEntityToModel() {
        return fileHierarchyLocal -> {
            if (FileStructureType.FILE.equals(fileHierarchyLocal.getFileType())) {
                return fileHierarchyLocal.toFile();
            }
            return fileHierarchyLocal.toFolder();
        };
    }

    private void addFile(ArchivedFile fileToBeArchived) throws FileHierarchyException {
        try {
            final FileDetails fileDetails = new FileDetails(fileToBeArchived.getDosPath(),
                    fileToBeArchived.getPlainSize(), fileToBeArchived.getPlainMd5(),
                    fileToBeArchived.getEncryptedSize(), fileToBeArchived.getEncryptedMd5(),
                    new String(fileToBeArchived.getKey()),
                    FileStatus.ARCHIVE_IN_PROGRESS,
                    fileToBeArchived.getFireId());
            fileHierarchyRepository.saveNewFile(fileToBeArchived.getAccountId(), fileToBeArchived.getStagingAreaId(),
                    fileToBeArchived.getPath(), fileDetails);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new FileHierarchyException("Exception while creating file structure => " +
                    "FileManagerService::createFileHierarchy(EncryptComplete) " + e.getMessage(), e);
        }
    }
}
