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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.ac.ebi.ega.ingestion.commons.messages.ArchiveEvent;
import uk.ac.ebi.ega.ingestion.file.manager.controller.exceptions.FileHierarchyException;
import uk.ac.ebi.ega.ingestion.file.manager.models.FileHierarchyModel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public interface IFileManagerService {

    List<FileHierarchyModel> findAll(Path filePath, String accountId, String stagingAreaId) throws FileNotFoundException;

    void archive(ArchiveEvent archiveEvent) throws IOException, FileHierarchyException;

    Page<FileHierarchyModel> findAllFiles(String accountId, String stagingAreaId, Predicate predicate, Pageable pageable) throws FileNotFoundException;

    /**
     * Returns Stream of FileHierarchyModel for given filePath. If filePath is file itself, then file will be returned.
     * If filePath is a Folder path then all files under this path will be returned OR
     * will return empty Stream if no file exists under this path.
     * Stream result is ordered by originalPath.
     *
     * @param accountId
     * @param stagingAreaId
     * @param filePath
     *
     * @return Stream of FileHierarchyModel object.
     * @throws FileNotFoundException
     *         throws if path is invalid. Provided path doesn't exists.
     */
    Stream<FileHierarchyModel> findAllFiles(String accountId, String stagingAreaId, Path filePath) throws FileNotFoundException;

    /**
     * Returns all files under path /AccountId/StagingAreaId OR
     * will return empty Stream if no file exists under this path.
     * Stream result is ordered by originalPath.
     *
     * @param accountId
     * @param stagingAreaId
     *
     * @return Stream of FileHierarchyModel object.
     */
    Stream<FileHierarchyModel> findAllFiles(String accountId, String stagingAreaId);
}
