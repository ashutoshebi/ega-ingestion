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

import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileStatus;

import java.time.LocalDateTime;
import java.util.Objects;

public class FileDetailsModel {

    private Long id;
    private String dosPath;
    private Long plainSize;
    private String plainMd5;
    private Long encryptedSize;
    private String encryptedMd5;
    private String key;
    private FileStatus status;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;

    public FileDetailsModel(final Long id, final String dosPath, final Long plainSize, final String plainMd5,
                            final Long encryptedSize, final String encryptedMd5, final String key,
                            final FileStatus status, final LocalDateTime createdDate, final LocalDateTime updatedDate) {
        this.id = Objects.requireNonNull(id);
        this.dosPath = Objects.requireNonNull(dosPath);
        this.plainSize = Objects.requireNonNull(plainSize);
        this.plainMd5 = Objects.requireNonNull(plainMd5);
        this.encryptedSize = Objects.requireNonNull(encryptedSize);
        this.encryptedMd5 = Objects.requireNonNull(encryptedMd5);
        this.key = Objects.requireNonNull(key);
        this.status = Objects.requireNonNull(status);
        this.createdDate = Objects.requireNonNull(createdDate);
        this.updatedDate = Objects.requireNonNull(updatedDate);
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

    public FileStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }
}
