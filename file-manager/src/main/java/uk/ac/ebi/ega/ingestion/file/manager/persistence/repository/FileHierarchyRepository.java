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

import java.util.Optional;

public interface FileHierarchyRepository extends CrudRepository<FileHierarchy, Long> {

    Optional<FileHierarchy> findByOriginalPath(String filePath);

    default FileHierarchy saveNewFolder(final String accountId, final String stagingAreaId, final String name,
                                        final String path, final FileHierarchy parent) {
        return save(FileHierarchy.folder(accountId, stagingAreaId, name, path, parent));
    }

    default FileHierarchy saveNewFile(String accountId, String stagingAreaId, String path, FileDetails fileDetails) {
        FileHierarchy parentFileHierarchy = createHierarchy(accountId, stagingAreaId, path);
        final String[] filePath = path.substring(1).split("/");
        return save(FileHierarchy.file(accountId, stagingAreaId, filePath[filePath.length - 1], path, parentFileHierarchy,
                fileDetails));
    }

    default FileHierarchy createHierarchy(String accountId, String stagingAreaId, String path) {
        final String[] filePathSubString = path.substring(1).split("/");
        final StringBuilder filePathBuilder = new StringBuilder();

        FileHierarchy parentFileHierarchy = null;

        int pathLevels = filePathSubString.length - 1;
        for (int i = 0; i < pathLevels; i++) {
            final String name = filePathSubString[i];
            final FileHierarchy finalParentFileHierarchy = parentFileHierarchy;
            filePathBuilder.append("/").append(filePathSubString[i]);

            Optional<FileHierarchy> entry = findByOriginalPath(filePathBuilder.toString());
            parentFileHierarchy = entry.orElseGet(() -> saveNewFolder(accountId, stagingAreaId, name,
                    filePathBuilder.toString(), finalParentFileHierarchy));
        }
        return parentFileHierarchy;
    }
}
