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

import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.fire.ingestion.service.IFireService;
import uk.ac.ebi.ega.ingestion.commons.messages.ArchiveEvent;
import uk.ac.ebi.ega.ingestion.file.manager.controller.exceptions.FileHierarchyException;
import uk.ac.ebi.ega.ingestion.file.manager.models.ArchivedFile;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileDetails;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileHierarchy;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.FileHierarchyRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FileManagerService implements IFileManagerService {

    private final IFireService fireService;

    private final Path fireBoxRelativePath;

    private final FileHierarchyRepository fileHierarchyRepository;

    public FileManagerService(final IFireService fireService,
                              final Path fireBoxRelativePath,
                              final FileHierarchyRepository fileHierarchyRepository) {
        this.fireService = fireService;
        this.fireBoxRelativePath = fireBoxRelativePath;
        this.fileHierarchyRepository = fileHierarchyRepository;
    }

    @Override
    public List<FileHierarchy> findAll(final String filePath) {
        List<FileHierarchy> fileHierarchies = new ArrayList<>();
        Optional<FileHierarchy> optionalFileHierarchy = fileHierarchyRepository.findByOriginalPath(filePath);
        optionalFileHierarchy.ifPresent(fileHierarchy -> fileHierarchies.addAll(fileHierarchy.getChildPaths()));
        return fileHierarchies;
    }

    @Override
    @Transactional(transactionManager = "fileManagerFireChainedTransactionManager", rollbackFor = Exception.class)
    public void archive(final ArchiveEvent archiveEvent) throws IOException, FileHierarchyException {
        final char[] password = FileUtils.readPasswordFile(Paths.get(archiveEvent.getKeyPath()));

        Path relativePathInFire = fireBoxRelativePath.resolve(archiveEvent.getStagingAreaId());
        final Optional<Long> fireId = fireService.archiveFile(null, new File(archiveEvent.getStagingPath()),
                archiveEvent.getEncryptedMd5(), relativePathInFire.toString());
        Path completePathInFire = relativePathInFire.resolve(new File(archiveEvent.getStagingPath()).getName());

        ArchivedFile archivedFile = new ArchivedFile(
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

        addFile(archivedFile);
    }

    private void addFile(ArchivedFile archivedFile) throws FileHierarchyException {
        try {
            final FileDetails fileDetails = new FileDetails(archivedFile.getDosPath(),
                    archivedFile.getPlainSize(), archivedFile.getPlainMd5(),
                    archivedFile.getEncryptedSize(), archivedFile.getEncryptedMd5(),
                    new String(archivedFile.getKey()), "Completed");
            fileHierarchyRepository.saveNewFile(archivedFile.getAccountId(), archivedFile.getStagingAreaId(),
                    archivedFile.getPath(), fileDetails);
        } catch (Exception e) {
            throw new FileHierarchyException("Exception while creating file structure => " +
                    "FileManagerService::createFileHierarchy(EncryptComplete) " + e.getMessage(), e);
        }
    }
}
