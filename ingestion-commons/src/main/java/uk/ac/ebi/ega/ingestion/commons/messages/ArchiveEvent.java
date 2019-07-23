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
package uk.ac.ebi.ega.ingestion.commons.messages;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

public class ArchiveEvent {

    private final String accountId;

    private final String stagingAreaId;

    private final String originalPath;

    private final String stagingPath;

    @JsonProperty
    private final long plainSize;

    private final String plainMd5;

    @JsonProperty
    private final long encryptedSize;

    private final String encryptedMd5;

    private final String keyPath;

    private final LocalDateTime startDateTime;

    private final LocalDateTime endDateTime;

    public ArchiveEvent(final String accountId, final String stagingAreaId, final String originalPath,
                        final String stagingPath, final long plainSize, final String plainMd5,
                        final long encryptedSize, final String encryptedMd5, String keyPath,
                        final LocalDateTime startDateTime, final LocalDateTime endDateTime) {
        this.accountId = Objects.requireNonNull(accountId);
        this.stagingAreaId = Objects.requireNonNull(stagingAreaId);
        this.originalPath = Objects.requireNonNull(originalPath);
        this.stagingPath = Objects.requireNonNull(stagingPath);
        this.plainSize = Objects.requireNonNull(plainSize);
        this.plainMd5 = Objects.requireNonNull(plainMd5);
        this.encryptedSize = Objects.requireNonNull(encryptedSize);
        this.encryptedMd5 = Objects.requireNonNull(encryptedMd5);
        this.keyPath = Objects.requireNonNull(keyPath);
        this.startDateTime = Objects.requireNonNull(startDateTime);
        this.endDateTime = Objects.requireNonNull(endDateTime);
    }

    public String getAccountId() {
        return accountId;
    }

    public String getStagingAreaId() {
        return stagingAreaId;
    }

    public String getOriginalPath() {
        return originalPath;
    }

    public String getStagingPath() {
        return stagingPath;
    }

    public long getPlainSize() {
        return plainSize;
    }

    public String getPlainMd5() {
        return plainMd5;
    }

    public long getEncryptedSize() {
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
}
