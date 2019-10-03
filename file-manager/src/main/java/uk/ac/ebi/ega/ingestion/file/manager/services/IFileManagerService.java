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

    void archive(ArchiveEvent archiveEvent) throws IOException, FileHierarchyException;

    /**
     * Returns List of FileHierarchyModel. Condition checks for case insensitive equals AccountId, StagingAreaId
     * & FilePath; Result contains both Files & Folders inside given filePath but no Children.
     * It Will return empty List if no data found.
     * List is ordered by originalPath.
     * It is a Non Recursive result.
     *
     * @param accountId     Account Id
     * @param stagingAreaId Staging Area Id
     * @param filePath      File path
     * @return List of FileHierarchyModel.
     * @throws FileNotFoundException
     */
    List<FileHierarchyModel> findAllFilesAndFoldersInPathNonRecursive(String accountId, String stagingAreaId,
                                                                      Optional<Path> filePath) throws FileNotFoundException;

    /**
     * Returns Page object of FileHierarchyModel. Condition checks for case insensitive equals AccountId, StagingAreaId
     * and Predicate given. Result contains all Files inside root path & not Folders.
     * Page will have no records if no data found.
     * It is a Recursive result.
     *
     * @param accountId     Account Id
     * @param stagingAreaId Staging Area Id
     * @param predicate     Predicate
     * @param pageable      Pageable
     * @return Page object of FileHierarchyModel.
     * @throws FileNotFoundException
     */
    Page<? extends IFileDetails> findAllFiles(String accountId, String stagingAreaId, Predicate predicate, Pageable pageable) throws FileNotFoundException;
    //TODO
//
//    /**
//     * Returns Stream of FileHierarchyModel for given filePath. If filePath is a file, then file will be returned.
//     * If filePath is a Folder path then all files under this folder will be returned. In case if Path is null then files at
//     * in root path will be returned whereas if no file present will return empty Stream.
//     * Stream is ordered by originalPath.
//     * It is a Non Recursive result.
//     *
//     * @param accountId     Account Id
//     * @param stagingAreaId Staging Area Id
//     * @param filePath      File path
//     * @return Stream of FileHierarchyModel object.
//     * @throws FileNotFoundException throws if path is invalid; Provided path doesn't exists.
//     */
//    Stream<FileHierarchyModel> findAllFilesInPathNonRecursive(String accountId, String stagingAreaId, Path filePath) throws FileNotFoundException;

    Optional<FileHierarchyModel> findParentOfPath(String accountId, String locationId, Path path);

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
}

