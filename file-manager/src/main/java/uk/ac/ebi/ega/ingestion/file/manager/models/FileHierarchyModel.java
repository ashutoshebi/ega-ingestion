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
package uk.ac.ebi.ega.ingestion.file.manager.models;

import uk.ac.ebi.ega.ingestion.commons.models.IFileDetails;
import uk.ac.ebi.ega.ingestion.file.manager.utils.FileStructureType;

import java.time.LocalDateTime;
import java.util.Objects;

public class FileHierarchyModel {

    private String name;
    private String originalPath;
    private FileStructureType fileType;
    private String accountId;
    private String stagingAreaId;
    private IFileDetails fileDetails;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    public FileHierarchyModel(final String accountId, final String stagingAreaId, final String name,
                              final String originalPath, final FileStructureType fileType, final LocalDateTime createdDate,
                              final LocalDateTime updatedDate, final IFileDetails fileDetails) {
        this.accountId = Objects.requireNonNull(accountId);
        this.stagingAreaId = Objects.requireNonNull(stagingAreaId);
        this.name = Objects.requireNonNull(name);
        this.originalPath = Objects.requireNonNull(originalPath);
        this.fileType = Objects.requireNonNull(fileType);
        this.createdDate = Objects.requireNonNull(createdDate);
        this.updatedDate = Objects.requireNonNull(updatedDate);
        this.fileDetails = fileDetails;
    }

    public String getName() {
        return name;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public FileStructureType getFileType() {
        return fileType;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getStagingAreaId() {
        return stagingAreaId;
    }

    public IFileDetails getFileDetails() {
        return fileDetails;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

}
