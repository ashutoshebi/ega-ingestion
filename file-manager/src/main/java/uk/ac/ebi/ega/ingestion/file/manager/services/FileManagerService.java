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
import uk.ac.ebi.ega.ingestion.commons.messages.EncryptComplete;
import uk.ac.ebi.ega.ingestion.file.manager.controller.exceptions.FileHierarchyException;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileHierarchy;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.FileHierarchyRepository;
import uk.ac.ebi.ega.ingestion.file.manager.utils.FileStructureType;

import java.util.Collections;
import java.util.List;

public class FileManagerService implements IFileManagerService {

    private final FileHierarchyRepository fileHierarchyRepository;

    public FileManagerService(final FileHierarchyRepository fileHierarchyRepository) {
        this.fileHierarchyRepository = fileHierarchyRepository;
    }

    @Override
    public List<FileHierarchy> findAll(final String filePath) {

        final FileHierarchy fileHierarchy = fileHierarchyRepository.findByOriginalPath(filePath);
        final List<FileHierarchy> fileHierarchies = fileHierarchy.getChildPaths();

        if (fileHierarchies.isEmpty()) {
            return Collections.singletonList(fileHierarchy);
        }
        return fileHierarchies;
    }

    @Transactional(transactionManager = "fileManager_transactionManager")
    @Override
    public void createFileHierarchy(final EncryptComplete encryptComplete) throws FileHierarchyException {

        try {
            final String[] filePathSubString = encryptComplete.getOriginalPath().substring(1).split("/");
            final StringBuilder filePathBuilder = new StringBuilder();

            FileHierarchy parentFileHierarchy = null;

            for (int i = 0; i < filePathSubString.length; i++) {
                String subPathString = filePathSubString[i];
                filePathBuilder.append("/").append(subPathString);

                FileHierarchy fileHierarchy = fileHierarchyRepository.findByOriginalPath(filePathBuilder.toString());

                if (fileHierarchy == null) {

                    if (i == (filePathSubString.length - 1)) {
                        fileHierarchy = newFileHierarchyForFile(encryptComplete, subPathString, parentFileHierarchy, filePathBuilder.toString());
                    } else {
                        fileHierarchy = newFileHierarchyForFolder(encryptComplete, subPathString, parentFileHierarchy, filePathBuilder.toString());
                    }
                    fileHierarchy = fileHierarchyRepository.save(fileHierarchy);
                }
                parentFileHierarchy = fileHierarchy;
            }
        } catch (Exception e) {
            throw FileHierarchyException.newFileHierarchyException("Exception while creating file structure => " +
                    "FileManagerService::createFileHierarchy(EncryptComplete) " + e.getMessage());
        }
    }

    private FileHierarchy newFileHierarchyForFile(final EncryptComplete encryptComplete, final String subPathString,
                                                  final FileHierarchy parentFileHierarchy, final String originalPath) {
        return new FileHierarchy(encryptComplete.getAccountId(), encryptComplete.getStagingAreaId(),
                subPathString, parentFileHierarchy, originalPath, FileStructureType.FILE,
                encryptComplete.getStagingPath(), encryptComplete.getPlainSize(), encryptComplete.getPlainMd5(),
                encryptComplete.getEncryptedSize(), encryptComplete.getEncryptedMd5(), encryptComplete.getKeyPath(),
                encryptComplete.getStartDateTime(), encryptComplete.getEndDateTime(), "Completed");
    }

    private FileHierarchy newFileHierarchyForFolder(final EncryptComplete encryptComplete, final String subPathString,
                                                    final FileHierarchy parentFileHierarchy, final String originalPath) {
        return new FileHierarchy(encryptComplete.getAccountId(), encryptComplete.getStagingAreaId(),
                subPathString, originalPath, parentFileHierarchy, FileStructureType.FOLDER);
    }
}
