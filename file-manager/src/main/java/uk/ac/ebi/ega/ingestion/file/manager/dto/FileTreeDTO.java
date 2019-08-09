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
package uk.ac.ebi.ega.ingestion.file.manager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.ResourceSupport;
import uk.ac.ebi.ega.ingestion.file.manager.models.FileHierarchyModel;

import java.time.LocalDateTime;

@JsonInclude(Include.NON_NULL)
public class FileTreeDTO extends ResourceSupport {

    private String accountId;
    private String locationId;
    private String name;
    private String md5PlainTextFile;
    private Long plainTextFileSize;
    private Long encryptedFileSize;
    private String status;
    private String message;
    private LocalDateTime modifiedDate;

    private FileTreeDTO() {
        super();
    }

    private FileTreeDTO(final String accountId, final String locationId, final Long plainTextFileSize,
                        final Long encryptedFileSize, final String name, final String md5PlainTextFile,
                        final LocalDateTime modifiedDate, final String status) {
        super();
        this.accountId = accountId;
        this.locationId = locationId;
        this.plainTextFileSize = plainTextFileSize;
        this.encryptedFileSize = encryptedFileSize;
        this.name = name;
        this.md5PlainTextFile = md5PlainTextFile;
        this.modifiedDate = modifiedDate;
        this.status = status;
    }

    private FileTreeDTO(final String accountId, final String locationId, final String name) {
        super();
        this.accountId = accountId;
        this.locationId = locationId;
        this.name = name;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getName() {
        return name;
    }

    @JsonProperty("md5")
    public String getMd5PlainTextFile() {
        return md5PlainTextFile;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public String getLocationId() {
        return locationId;
    }

    public Long getPlainTextFileSize() {
        return plainTextFileSize;
    }

    public Long getEncryptedFileSize() {
        return encryptedFileSize;
    }

    public static FileTreeDTO file(final FileHierarchyModel fileHierarchyModel) {
        return new FileTreeDTO(
                fileHierarchyModel.getAccountId(),
                fileHierarchyModel.getStagingAreaId(),
                fileHierarchyModel.getFileDetails().getPlainSize(),
                fileHierarchyModel.getFileDetails().getEncryptedSize(),
                fileHierarchyModel.getName(),
                fileHierarchyModel.getFileDetails().getPlainMd5(),
                fileHierarchyModel.getUpdatedDate(),
                fileHierarchyModel.getFileDetails().getStatus());
    }

    public static FileTreeDTO folder(final FileHierarchyModel fileHierarchyModel) {
        return new FileTreeDTO(
                fileHierarchyModel.getAccountId(),
                fileHierarchyModel.getStagingAreaId(),
                fileHierarchyModel.getName());
    }
}
