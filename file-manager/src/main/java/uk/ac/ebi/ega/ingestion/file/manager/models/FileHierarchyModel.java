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

import uk.ac.ebi.ega.ingestion.file.manager.utils.FileStructureType;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.StringJoiner;

public class FileHierarchyModel {

    private Long id;
    private String name;
    private String originalPath;
    private FileStructureType fileType;
    private String accountId;
    private String stagingAreaId;
    private FileDetailsModel fileDetails;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    private static final String COLUMN_NAMES = buildColumnNames();

    private FileHierarchyModel(final Long id, final String accountId, final String stagingAreaId, final String name,
                               final String originalPath, final FileStructureType fileType, final LocalDateTime createdDate,
                               final LocalDateTime updatedDate, final FileDetailsModel fileDetails) {
        this.id = Objects.requireNonNull(id);
        this.accountId = Objects.requireNonNull(accountId);
        this.stagingAreaId = Objects.requireNonNull(stagingAreaId);
        this.name = Objects.requireNonNull(name);
        this.originalPath = Objects.requireNonNull(originalPath);
        this.fileType = Objects.requireNonNull(fileType);
        this.createdDate = Objects.requireNonNull(createdDate);
        this.updatedDate = Objects.requireNonNull(updatedDate);
        this.fileDetails = fileDetails;
    }

    public Long getId() {
        return id;
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

    public FileDetailsModel getFileDetails() {
        return fileDetails;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public static FileHierarchyModel folder(final Long id, final String accountId, final String stagingAreaId, final String name,
                                            final String originalPath, final FileStructureType fileType, final LocalDateTime createdDate,
                                            final LocalDateTime updatedDate) {
        return new FileHierarchyModel(id, accountId, stagingAreaId, name, originalPath, fileType, createdDate, updatedDate, null);
    }

    public static FileHierarchyModel file(final Long id, final String accountId, final String stagingAreaId, final String name,
                                          final String originalPath, final FileStructureType fileType, final LocalDateTime createdDate,
                                          final LocalDateTime updatedDate, final FileDetailsModel fileDetails) {
        return new FileHierarchyModel(id, accountId, stagingAreaId, name, originalPath, fileType, createdDate, updatedDate, Objects.requireNonNull(fileDetails));
    }

    /**
     * @return String of Tab separated property values.
     * This method returns values for type File.
     * @See FileHierarchyModel#getColumnNames()
     */
    public String toStringFileDetails() {
        return new StringJoiner("\t").
                add(getAccountId()).
                add(getStagingAreaId()).
                add(getName()).
                add(getFileDetails().getPlainMd5()).
                add(getFileDetails().getPlainSize().toString()).
                add(getFileDetails().getEncryptedSize().toString()).
                add(getFileDetails().getStatus().toString()).
                add(getFileDetails().getUpdatedDate().toString()).
                add("\n").
                toString();
    }

    /**
     * @return String of Tab separated column names for property values
     * @See FileHierarchyModel#toStringFileDetails()
     */
    public static String getColumnNames() {
        return COLUMN_NAMES;
    }

    private static String buildColumnNames() {
        return new StringJoiner("\t").
                add("Account Id").
                add("Staging Area Id").
                add("File Name").
                add("Plain MD5").
                add("Plain MD5 Size").
                add("Encrypted Size").
                add("Status").
                add("Modified Date").
                add("\n").
                toString().
                intern();
    }
}
