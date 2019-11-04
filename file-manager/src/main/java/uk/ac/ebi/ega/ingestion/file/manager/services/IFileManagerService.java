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
import uk.ac.ebi.ega.ingestion.commons.messages.FileEncryptionResult;
import uk.ac.ebi.ega.ingestion.commons.messages.NewFileEvent;
import uk.ac.ebi.ega.ingestion.commons.models.IFileDetails;
import uk.ac.ebi.ega.ingestion.file.manager.controller.exceptions.FileHierarchyException;
import uk.ac.ebi.ega.ingestion.file.manager.models.FileHierarchyModel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface IFileManagerService {

    /**
     * Adds a new file into the system. After adding into the database a new event to encrypt the file is sent. If
     * the same file / version is sent multiple times we will create new events if the file has not been processed yet.
     *
     * @param key
     * @param newFileEvent
     */
    void newFile(String key, NewFileEvent newFileEvent) throws FileHierarchyException;

    /**
     * Modifies file record of the system with the current file uri, completes the file information with the plain
     * encryption md5 and plain size, modifies current encrypted file size and sets the new encryption format.
     *
     * @param key
     * @param fileEncryptionData
     * @throws IOException
     */
    void archive(String key, FileEncryptionResult fileEncryptionData);

    /**
     * Returns List of FileHierarchyModel. Condition checks for case insensitive equals AccountId, StagingAreaId
     * & FilePath; Result contains both Files & Folders inside given filePath but no Children.
     * It Will return empty List if no data found.
     * List is ordered by originalPath.
     * It is a Non Recursive result.
     *
     * @param accountId     Account Id
     * @param stagingAreaId Staging Area Id
     * @param filePath      File path (optional)
     * @return List of FileHierarchyModel.
     * @throws FileNotFoundException
     */
    List<FileHierarchyModel> findAllFilesAndFoldersInPath(String accountId, String stagingAreaId,
                                                          Optional<Path> filePath) throws FileNotFoundException;

    /**
     * Returns Page of file details on accountId and stagingAreaId filtered with predicate
     * Page will have no records if no data found.
     *
     * @param accountId     Account Id
     * @param stagingAreaId Staging Area Id
     * @param predicate     Predicate
     * @param pageable      Pageable
     * @return Page object of FileHierarchyModel.
     */
    Page<? extends IFileDetails> findAllFiles(String accountId, String stagingAreaId, Predicate predicate,
                                              Pageable pageable);

    /**
     * Returns all files in root path OR
     * will return empty Stream if no file exists under this path.
     * Stream is ordered by originalPath.
     *
     * @param accountId     Account Id
     * @param stagingAreaId Staging Area Id
     * @return Stream of FileHierarchyModel object.
     */
    Stream<? extends IFileDetails> findAllFiles(String accountId, String stagingAreaId, Optional<String> optionalPath);

    /**
     * Returns parent directory if exists. If parent is the root of accountId, stagingAreaId then returns empty
     *
     * @param accountId
     * @param locationId
     * @param path
     * @return Optional FileHierarchyModel
     */
    Optional<FileHierarchyModel> findParentOfPath(String accountId, String locationId, Path path);

}

