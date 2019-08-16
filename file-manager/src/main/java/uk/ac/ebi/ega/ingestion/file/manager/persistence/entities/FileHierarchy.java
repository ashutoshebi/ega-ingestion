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
import uk.ac.ebi.ega.ingestion.file.manager.models.FileDetailsModel;
import uk.ac.ebi.ega.ingestion.file.manager.models.FileHierarchyModel;
import uk.ac.ebi.ega.ingestion.file.manager.utils.FileStructureType;

import javax.persistence.CascadeType;
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
import javax.persistence.OrderBy;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;

@Table(indexes = {@Index(name = "FILEPATH_INDEX", columnList = "originalPath")})
@Entity
@EntityListeners(AuditingEntityListener.class)
public class FileHierarchy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FileHierarchy parentPath;

    @OrderBy("originalPath ASC")
    @OneToMany(mappedBy = "parentPath", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FileHierarchy> childPaths;

    @Column(nullable = false, length = 4096, unique = true)
    private String originalPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileStructureType fileType;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String stagingAreaId;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "file_details_id")
    private FileDetails fileDetails;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;

    protected FileHierarchy() {
    }

    protected FileHierarchy(final String accountId, final String stagingAreaId, final String name,
                            final String originalPath, final FileHierarchy parentPath, final FileStructureType fileType,
                            final FileDetails fileDetails) {
        this.accountId = accountId;
        this.stagingAreaId = stagingAreaId;
        this.name = name;
        this.parentPath = parentPath;
        this.originalPath = originalPath;
        this.fileType = fileType;
        this.fileDetails = fileDetails;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public FileHierarchy getParentPath() {
        return parentPath;
    }

    public List<FileHierarchy> getChildPaths() {
        return childPaths;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public FileStructureType getFileType() {
        return fileType;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getStagingAreaId() {
        return stagingAreaId;
    }

    public FileDetails getFileDetails() {
        return fileDetails;
    }

    public static FileHierarchy folder(String accountId, String stagingAreaId, String name, String path,
                                       FileHierarchy parent) {
        return new FileHierarchy(accountId, stagingAreaId, name, path, parent, FileStructureType.FOLDER, null);
    }

    public static FileHierarchy file(String accountId, String stagingAreaId, String name, String path,
                                     FileHierarchy parent, FileDetails fileDetails) {
        return new FileHierarchy(accountId, stagingAreaId, name, path, parent, FileStructureType.FILE, fileDetails);
    }

    public FileHierarchyModel toFile() {
        return FileHierarchyModel.file(
                getId(),
                getAccountId(),
                getStagingAreaId(),
                getName(),
                getOriginalPath(),
                getFileType(),
                getCreatedDate(),
                getUpdatedDate(),
                newFileDetailsModel());
    }

    public FileHierarchyModel toFolder() {
        return FileHierarchyModel.folder(
                getId(),
                getAccountId(),
                getStagingAreaId(),
                getName(),
                getOriginalPath(),
                getFileType(),
                getCreatedDate(),
                getUpdatedDate()
        );
    }

    private FileDetailsModel newFileDetailsModel() {
        return new FileDetailsModel(
                getFileDetails().getId(),
                getFileDetails().getDosPath(),
                getFileDetails().getPlainSize(),
                getFileDetails().getPlainMd5(),
                getFileDetails().getEncryptedSize(),
                getFileDetails().getEncryptedMd5(),
                getFileDetails().getKey(),
                getFileDetails().getStatus(),
                getFileDetails().getCreatedDate(),
                getFileDetails().getUpdatedDate()
        );
    }
}

