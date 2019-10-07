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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import uk.ac.ebi.ega.ingestion.commons.models.FileStatus;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.EncryptedObject;

import java.util.Optional;
import java.util.stream.Stream;

public interface EncryptedObjectRepository extends PagingAndSortingRepository<EncryptedObject, Long>,
        QuerydslPredicateExecutor<EncryptedObject> {

    Optional<EncryptedObject> findByPathAndVersion(String path, long version);

    Stream<EncryptedObject> findAllByAccountIdAndStagingIdOrderByPath(String accountId, String stagingId);

    Stream<EncryptedObject> findAllByAccountIdAndStagingIdAndPathStartingWithOrderByPath(String accountId,
                                                                                         String stagingId,
                                                                                         String pathExpresion);

    Page<EncryptedObject> findByStatus(FileStatus archiveInProgress, Pageable pageRequest);

}
