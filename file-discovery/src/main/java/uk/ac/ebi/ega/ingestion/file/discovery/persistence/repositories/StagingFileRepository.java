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
package uk.ac.ebi.ega.ingestion.file.discovery.persistence.repositories;

import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.PagingAndSortingRepository;
import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingFile;

import java.time.LocalDateTime;

public interface StagingFileRepository extends PagingAndSortingRepository<StagingFileImpl, String>,
        QuerydslPredicateExecutor<StagingFileImpl>, QuerydslBinderCustomizer<QStagingFileImpl> {

    default Page<StagingFileImpl> findAllByStagingAreaId(String stagingAreaId, Predicate predicate,
                                                         Pageable pageable) {

        Predicate predicateWithStagingArea = Expressions.predicate(Ops.EQ,
                QStagingFileImpl.stagingFileImpl.stagingAreaId, Expressions.constant(stagingAreaId)).and(predicate);
        return findAll(predicateWithStagingArea, pageable);
    }

    default Iterable<StagingFileImpl> findAllByStagingAreaId(String stagingAreaId) {
        Predicate predicateWithStagingArea = Expressions.predicate(Ops.EQ,
                QStagingFileImpl.stagingFileImpl.stagingAreaId, Expressions.constant(stagingAreaId));
        return findAll(predicateWithStagingArea);
    }

    default Iterable<? extends StagingFile> findAllByStagingAreaIdOlderThan(String stagingAreaId,
                                                                            LocalDateTime cutOffDate) {
        Predicate predicateWithStagingArea = Expressions.allOf(
                Expressions.predicate(Ops.EQ, QStagingFileImpl.stagingFileImpl.stagingAreaId,
                        Expressions.constant(stagingAreaId)),
                Expressions.predicate(Ops.LT, QStagingFileImpl.stagingFileImpl.updateDate,
                        Expressions.asDateTime(cutOffDate))
        );
        return findAll(predicateWithStagingArea);
    }

    @Override
    default void customize(QuerydslBindings bindings, QStagingFileImpl stagingArea) {
        bindings.bind(stagingArea.relativePath).first((path, value) -> path.containsIgnoreCase(value));
    }
}
