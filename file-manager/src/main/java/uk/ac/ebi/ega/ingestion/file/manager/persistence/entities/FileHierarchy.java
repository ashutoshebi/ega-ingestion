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
package uk.ac.ebi.ega.ingestion.file.manager.persistence.entities;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uk.ac.ebi.ega.ingestion.file.manager.utils.FileStructureType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;

@Table(name = "FILE_HIERARCHY", schema = "file_ingestion",
        indexes = {@Index(name = "FILEPATH_INDEX", columnList = "originalPath")})
@Entity
@EntityListeners(AuditingEntityListener.class)
public class FileHierarchy { //TODO More fields to be added

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filedetails;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FileHierarchy parentPath;

    @OneToMany(mappedBy = "parentPath", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FileHierarchy> childPaths;

    @Column(nullable = false, length = 4096, unique = true)
    private String originalPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileStructureType fileType;

    @Column(nullable = false)
    private String accountId;

    private String stagingAreaId;
    private String stagingPath;
    private Long plainSize;
    private String plainMd5;
    private Long encryptedSize;
    private String encryptedMd5;
    private String keyPath;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String status;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updateDate;

    public FileHierarchy() {
    }

    public FileHierarchy(final String filedetails, final FileHierarchy parentPath, final String originalPath,
                         final FileStructureType fileType, final String accountId, final String stagingAreaId, final String stagingPath,
                         final Long plainSize, final String plainMd5, final Long encryptedSize, final String encryptedMd5,
                         final String keyPath, final LocalDateTime startDateTime, final LocalDateTime endDateTime, final String status) {
        this.filedetails = filedetails;
        this.parentPath = parentPath;
        this.originalPath = originalPath;
        this.fileType = fileType;
        this.accountId = accountId;
        this.stagingAreaId = stagingAreaId;
        this.stagingPath = stagingPath;
        this.plainSize = plainSize;
        this.plainMd5 = plainMd5;
        this.encryptedSize = encryptedSize;
        this.encryptedMd5 = encryptedMd5;
        this.keyPath = keyPath;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.status = status;
    }

    public FileHierarchy(final String accountId, final String stagingAreaId, final String filedetails,
                         final String originalPath, final FileHierarchy parentPath,
                         final FileStructureType fileType) {
        this.accountId = accountId;
        this.stagingAreaId = stagingAreaId;
        this.filedetails = filedetails;
        this.originalPath = originalPath;
        this.parentPath = parentPath;
        this.fileType = fileType;
    }

    public Long getId() {
        return id;
    }

    public String getFiledetails() {
        return filedetails;
    }

    /*public void setFiledetails(String filedetails) {
        this.filedetails = filedetails;
    }*/

    public FileHierarchy getParentPath() {
        return parentPath;
    }

    /*public void setParentPath(FileHierarchy parentPath) {
        this.parentPath = parentPath;
    }*/

    public List<FileHierarchy> getChildPaths() {
        return childPaths;
    }

    public void setChildPaths(List<FileHierarchy> childPaths) {
        this.childPaths = childPaths;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public void setOriginalPath(String originalPath) {
        this.originalPath = originalPath;
    }

    public FileStructureType getFileType() {
        return fileType;
    }

    public void setFileType(FileStructureType fileType) {
        this.fileType = fileType;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getStagingAreaId() {
        return stagingAreaId;
    }

    public String getStagingPath() {
        return stagingPath;
    }

    public Long getPlainSize() {
        return plainSize;
    }

    public String getPlainMd5() {
        return plainMd5;
    }

    public Long getEncryptedSize() {
        return encryptedSize;
    }

    public String getEncryptedMd5() {
        return encryptedMd5;
    }

    public String getKeyPath() {
        return keyPath;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public String getStatus() {
        return status;
    }
}

