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


import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingAreaFile;

import java.time.LocalDateTime;

@Table("STAGING_AREA_FILES")
public class StagingAreaFileImpl implements StagingAreaFile {

    @Id
    private String id;

    private String stagingAreaId;

    private String relativePath;

    private String name;

    private Long size;

    private LocalDateTime createDate;

    private LocalDateTime updateDate;

    public StagingAreaFileImpl(String id, String stagingAreaId, String relativePath, String name, Long size,
                               LocalDateTime createDate, LocalDateTime updateDate) {
        this.id = id;
        this.stagingAreaId = stagingAreaId;
        this.relativePath = relativePath;
        this.name = name;
        this.size = size;
        this.createDate = createDate;
        this.updateDate = updateDate;
    }

    public String getId() {
        return id;
    }

    public String getStagingAreaId() {
        return stagingAreaId;
    }

    @Override
    public String getRelativePath() {
        return relativePath;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Long getSize() {
        return size;
    }

    @Override
    public LocalDateTime getCreateDate() {
        return createDate;
    }

    @Override
    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

}