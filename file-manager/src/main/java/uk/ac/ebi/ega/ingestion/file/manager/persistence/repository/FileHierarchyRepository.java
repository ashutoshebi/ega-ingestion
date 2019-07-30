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
package uk.ac.ebi.ega.ingestion.file.manager.persistence.repository;

import org.springframework.data.repository.CrudRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileDetails;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileHierarchy;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public interface FileHierarchyRepository extends CrudRepository<FileHierarchy, Long> {

    Optional<FileHierarchy> findByOriginalPathAndAccountIdAndStagingAreaId(String filePath, String accountId, String stagingAreaId);

    default FileHierarchy saveNewFolder(final String accountId, final String stagingAreaId, final String name,
                                        final String path, final FileHierarchy parent) {
        return save(FileHierarchy.folder(accountId, stagingAreaId, name, path, parent));
    }

    default FileHierarchy saveNewFile(String accountId, String stagingAreaId, String path, FileDetails fileDetails) {
        return save(createHierarchy(accountId, stagingAreaId, path, fileDetails));
    }

    default FileHierarchy createHierarchy(final String accountId, final String stagingAreaId, final String originalPath,
                                          final FileDetails fileDetails) {
        final Path path = Paths.get(originalPath).normalize();
        final Optional<FileHierarchy> fileHierarchy = findByOriginalPathAndAccountIdAndStagingAreaId(path.toString(), accountId, stagingAreaId);

        if (!fileHierarchy.isPresent()) {
            final FileHierarchy parentFileHierarchy = fileHierarchyRecursion(path.getParent(), accountId, stagingAreaId);
            return FileHierarchy.file(accountId, stagingAreaId, path.getFileName().toString(), path.toString(), parentFileHierarchy,
                    fileDetails);
        }
        return fileHierarchy.get();
    }

    /**
     * @param path          Original file path
     * @param accountId     account id
     * @param stagingAreaId staging area id
     * @return FileHierarchy Parent FileHierarchy object
     */
    default FileHierarchy fileHierarchyRecursion(final Path path, final String accountId, final String stagingAreaId) {//TODO Need to make as private method. Supported in java 9

        if (path != null && path.getFileName() != null) {

            final Optional<FileHierarchy> fileHierarchy = findByOriginalPathAndAccountIdAndStagingAreaId(path.toString(), accountId, stagingAreaId);

            if (!fileHierarchy.isPresent()) {
                final FileHierarchy parentFileHierarchy = fileHierarchyRecursion(path.getParent(), accountId, stagingAreaId);

                return saveNewFolder(accountId, stagingAreaId, path.getFileName().toString(),
                        path.toString(), parentFileHierarchy);
            }
            return fileHierarchy.get();
        }
        return null;
    }
}
