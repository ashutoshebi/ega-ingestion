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

import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.core.types.dsl.StringPath;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.querydsl.binding.SingleValueBinding;
import org.springframework.data.repository.PagingAndSortingRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.EncryptedObject;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileHierarchy;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.QFileHierarchy;
import uk.ac.ebi.ega.ingestion.file.manager.utils.FileStructureType;

import javax.persistence.QueryHint;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE;

public interface FileHierarchyRepository extends PagingAndSortingRepository<FileHierarchy, Long>,
        QuerydslPredicateExecutor<FileHierarchy>, QuerydslBinderCustomizer<QFileHierarchy> {

    String HINT_FETCH_SIZE_VALUE = "50";

    List<FileHierarchy> findAllByAccountIdAndStagingAreaIdAndParentPath(String accountId, String stagingAreaId,
                                                                          FileHierarchy parent);

    /**
     * Method is being called internally.
     *
     * @See FileHierarchyRepository#findAllFilesOrFoldersInRootPathNonRecursive(String, String, Long, FileStructureType)
     */
    @QueryHints(value = @QueryHint(name = HINT_FETCH_SIZE, value = HINT_FETCH_SIZE_VALUE))
    Stream<FileHierarchy> findAllByAccountIdAndStagingAreaIdAndParentPathIdAndFileTypeAllIgnoreCaseOrderByOriginalPath(String accountId, String stagingAreaId,
                                                                                                                       Long parentId, FileStructureType fileStructureType);

    /**
     * Returns Stream of FileHierarchy object. Condition checks for case insensitive equals AccountId, StagingAreaId,
     * ParentId & FileStructureType.
     * All children of given parent will be returned. Returns Files Or Folders based on FileStructureType.
     * Stream is ordered by originalPath.
     * It is a Non Recursive result.
     *
     * @param accountId         Account Id
     * @param stagingAreaId     Staging Area Id
     * @param parentId          Parent Id
     * @param fileStructureType FileStructureType File type FILE or FOLDER
     * @return Stream of FileHierarchy object
     */
    default Stream<FileHierarchy> findAllFilesOrFoldersInRootPathNonRecursive(final String accountId, final String stagingAreaId,
                                                                              final Long parentId, final FileStructureType fileStructureType) {
        return findAllByAccountIdAndStagingAreaIdAndParentPathIdAndFileTypeAllIgnoreCaseOrderByOriginalPath(accountId, stagingAreaId,
                parentId, fileStructureType);
    }

    /**
     * Method is being called internally.
     *
     * @See FileHierarchyRepository#findAllFilesOrFoldersInRootPathRecursive(String, String, FileStructureType)
     */
    @QueryHints(value = @QueryHint(name = HINT_FETCH_SIZE, value = HINT_FETCH_SIZE_VALUE))
    Stream<FileHierarchy> findAllByAccountIdAndStagingAreaIdAndFileTypeAllIgnoreCaseOrderByOriginalPath(String accountId, String stagingAreaId,
                                                                                                        FileStructureType fileStructureType);

    /**
     * Returns Stream of FileHierarchy object. Check for case insensitive equals AccountId and StagingAreaId
     * & FileStructureType. All children of root path will be returned recursively. Returns Files Or Folders based on FileStructureType.
     * Stream is ordered by originalPath.
     * It is a Recursive result.
     *
     * @param accountId         Account Id
     * @param stagingAreaId     Staging Area Id
     * @param fileStructureType FileStructureType File type FILE or FOLDER
     * @return Stream of FileHierarchy object
     */
    default Stream<FileHierarchy> findAllFilesOrFoldersInRootPathRecursive(final String accountId, final String stagingAreaId,
                                                                           final FileStructureType fileStructureType) {
        return findAllByAccountIdAndStagingAreaIdAndFileTypeAllIgnoreCaseOrderByOriginalPath(accountId, stagingAreaId, fileStructureType);
    }

    /**
     * Method is being called internally.
     *
     * @See FileHierarchyRepository#findAllFilesAndFoldersInPathNonRecursive(String, String)
     */
    List<FileHierarchy> findAllByAccountIdAndStagingAreaIdAndParentPathIsNullAllIgnoreCaseOrderByOriginalPath(String accountId, String stagingAreaId);

    /**
     * Returns List of FileHierarchy. Condition checks for case insensitive equals AccountId, StagingAreaId
     * & FilePath; Result contains both Files & Folders inside given filePath but no Children.
     * It Will return empty List if no data found.
     * List is ordered by originalPath.
     * It is a Non Recursive result.
     *
     * @param accountId     Account Id
     * @param stagingAreaId Staging Area Id
     * @return List of FileHierarchy object
     */
    default List<FileHierarchy> findAllFilesAndFoldersInPathNonRecursive(final String accountId, final String stagingAreaId) {
        return findAllByAccountIdAndStagingAreaIdAndParentPathIsNullAllIgnoreCaseOrderByOriginalPath(accountId, stagingAreaId);
    }

    /**
     * Method is being called internally.
     *
     * @See FileHierarchyRepository#findAllFilesInRootPathRecursive(String, String)
     */
    @QueryHints(value = @QueryHint(name = HINT_FETCH_SIZE, value = HINT_FETCH_SIZE_VALUE))
    Stream<FileHierarchy> findAllByAccountIdAndStagingAreaIdAndFileTypeAndParentPathIsNullAllIgnoreCaseOrderByOriginalPath(String accountId, String stagingAreaId,
                                                                                                                           FileStructureType fileType);

    /**
     * Returns List of FileHierarchy object. Check for case insensitive equals AccountId, StagingAreaId
     * & ParentId is null; All Files present at root path.
     * Stream is ordered by originalPath.
     * It is a Non Recursive result.
     *
     * @param accountId     Account Id
     * @param stagingAreaId Staging Area Id
     * @return Stream of FileHierarchy object
     */
    default Stream<FileHierarchy> findAllFilesInPathNonRecursive(final String accountId, final String stagingAreaId) {
        return findAllByAccountIdAndStagingAreaIdAndFileTypeAndParentPathIsNullAllIgnoreCaseOrderByOriginalPath(accountId, stagingAreaId, FileStructureType.FILE);
    }

    default Optional<FileHierarchy> findOne(final String filePath, final String accountId, final String stagingAreaId) {
        final Predicate predicate = Expressions.allOf(
                Expressions.predicate(Ops.EQ_IGNORE_CASE, QFileHierarchy.fileHierarchy.originalPath,
                        Expressions.constant(filePath)),
                Expressions.predicate(Ops.EQ_IGNORE_CASE, QFileHierarchy.fileHierarchy.accountId,
                        Expressions.constant(accountId)),
                Expressions.predicate(Ops.EQ_IGNORE_CASE, QFileHierarchy.fileHierarchy.stagingAreaId,
                        Expressions.constant(stagingAreaId)));
        return findOne(predicate);
    }

    default FileHierarchy saveNewFolder(final String accountId, final String stagingAreaId, final String name,
                                        final String path, final FileHierarchy parent) {
        return save(FileHierarchy.folder(accountId, stagingAreaId, name, path, parent));
    }

    default FileHierarchy saveNewFile(EncryptedObject encryptedObject) {
        return save(createHierarchy(encryptedObject));
    }

    default FileHierarchy createHierarchy(final EncryptedObject encryptedObject) {
        String originalPath = encryptedObject.getPath();
        final String path = originalPath.startsWith("/") ? originalPath : "/" + originalPath;
        final Path resolvedOriginalPath = Paths.get(path).normalize();

        final Optional<FileHierarchy> fileHierarchy = findOne(resolvedOriginalPath.toString(),
                encryptedObject.getAccountId(), encryptedObject.getStagingId());

        if (!fileHierarchy.isPresent()) {
            final FileHierarchy parentFileHierarchy = fileHierarchyRecursion(resolvedOriginalPath.getParent(),
                    encryptedObject.getAccountId(), encryptedObject.getStagingId());
            return FileHierarchy.file(encryptedObject.getAccountId(), encryptedObject.getStagingId(),
                    resolvedOriginalPath.getFileName().toString(), resolvedOriginalPath.toString(), parentFileHierarchy,
                    encryptedObject);
        }
        return fileHierarchy.get();
    }

    /**
     * @param path          Original file path
     * @param accountId     account id
     * @param stagingAreaId staging area id
     * @return FileHierarchy Parent FileHierarchy object
     */
    default FileHierarchy fileHierarchyRecursion(final Path path, final String accountId, final String stagingAreaId) {
        //TODO Need to make as private method. Supported in java 9

        if (path != null && path.getFileName() != null) {

            final Optional<FileHierarchy> fileHierarchy = findOne(path.toString(), accountId, stagingAreaId);

            if (!fileHierarchy.isPresent()) {
                final FileHierarchy parentFileHierarchy = fileHierarchyRecursion(path.getParent(), accountId, stagingAreaId);

                return saveNewFolder(accountId, stagingAreaId, path.getFileName().toString(),
                        path.toString(), parentFileHierarchy);
            }
            return fileHierarchy.get();
        }
        return null;
    }

    @Override
    default void customize(QuerydslBindings bindings, QFileHierarchy fileHierarchy) {
        bindings.bind(String.class)
                .first((SingleValueBinding<StringPath, String>) StringExpression::containsIgnoreCase);
    }
}
