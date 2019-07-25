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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class FileDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String dosPath;

    @Column(nullable = false)
    private Long plainSize;

    @Column(nullable = false)
    private String plainMd5;

    @Column(nullable = false)
    private Long encryptedSize;

    @Column(nullable = false)
    private String encryptedMd5;

    @Column(nullable = false)
    private String key;

    @Column(nullable = false)
    private String status;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updateDate;

    protected FileDetails() {
    }

    public FileDetails(final String dosPath, final Long plainSize, final String plainMd5,
                       final Long encryptedSize, final String encryptedMd5,
                       final String key, final String status) {
        this.dosPath = dosPath;
        this.plainSize = plainSize;
        this.plainMd5 = plainMd5;
        this.encryptedSize = encryptedSize;
        this.encryptedMd5 = encryptedMd5;
        this.key = key;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getDosPath() {
        return dosPath;
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

    public String getKey() {
        return key;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }
}
