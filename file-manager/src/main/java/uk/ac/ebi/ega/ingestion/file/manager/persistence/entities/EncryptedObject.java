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
import uk.ac.ebi.ega.ingestion.commons.models.Encryption;
import uk.ac.ebi.ega.ingestion.commons.models.FileStatus;
import uk.ac.ebi.ega.ingestion.commons.models.IFileDetails;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class EncryptedObject implements IFileDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String stagingId;

    @Column(nullable = false)
    private String path;

    private long version;

    @Column(nullable = false)
    private String uri;

    private Long plainSize;

    @Column(nullable = false)
    private String plainMd5;

    @Column(nullable = false)
    private long encryptedSize;

    @Column(nullable = false)
    private String encryptedMd5;

    @Column(nullable = false)
    private Encryption encryptionType;

    @Column(nullable = false)
    private String encryptionKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileStatus status;

    private String fireId;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;

    public EncryptedObject() {
    }

    public EncryptedObject(String accountId, String stagingId, String path, long version, String uri,
                           String plainMd5, long encryptedSize, String encryptedMd5) {
        this.accountId = accountId;
        this.stagingId = stagingId;
        this.path = path;
        this.version = version;
        this.uri = uri;
        this.plainMd5 = plainMd5;
        this.encryptedSize = encryptedSize;
        this.encryptedMd5 = encryptedMd5;
        this.encryptionType = Encryption.PGP;
        this.encryptionKey = "EGA_PGP_ENCRYPTION_KEY";
        this.status = FileStatus.PROCESSING;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public String getStagingId() {
        return stagingId;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public String getPlainMd5() {
        return plainMd5;
    }

    @Override
    public Long getPlainSize() {
        return plainSize;
    }

    @Override
    public FileStatus getStatus() {
        return status;
    }

    @Override
    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    @Override
    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public String getFireId() {
        return fireId;
    }

    public void setFireId(String fireId) {
        this.fireId = fireId;
    }

    public String getUri() {
        return uri;
    }

    public String toFirePath() {
        return Paths.get(stagingId + "/" + path + "." + version).normalize().toString();
    }

    public void archive(String newUri, String encryptedMD5, long plainSize, long encryptedSize,
                        Encryption encryptionType, String encryptionKey) {
        this.uri = newUri;
        this.encryptedMd5 = encryptedMD5;
        this.plainSize = plainSize;
        this.encryptedSize = encryptedSize;
        this.encryptionType = encryptionType;
        this.encryptionKey = encryptionKey;
        this.status = FileStatus.ARCHIVE_IN_PROGRESS;
    }

    public void archived(String fireUri) {
        this.uri = fireUri;
        this.status = FileStatus.ARCHIVED_SUCCESSFULLY;
    }

    public void archived(final String fireId, final String fireURI) {
        this.fireId = fireId;
        this.uri = fireURI;
        this.status = FileStatus.ARCHIVED_SUCCESSFULLY;
    }

    public void error() {
        this.status = FileStatus.ERROR;
    }
}
