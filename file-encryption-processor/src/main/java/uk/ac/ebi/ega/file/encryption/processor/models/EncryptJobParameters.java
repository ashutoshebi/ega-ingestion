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
package uk.ac.ebi.ega.file.encryption.processor.models;

import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.JobParameters;

import java.nio.file.Path;
import java.time.LocalDateTime;

public class EncryptJobParameters implements JobParameters { //TODO JobParameters is empty interface. Need to check why it has been created.

    private final String accountId;
    private final String stagingId;
    private final Path filePath;
    private final long size;
    private final LocalDateTime lastUpdate;
    private final Path md5FilePath;

    public EncryptJobParameters(final String accountId, final String stagingId, final Path filePath, final long size, final LocalDateTime lastUpdate,
                                final Path md5FilePath) {
        this.accountId = accountId;
        this.stagingId = stagingId;
        this.filePath = filePath;
        this.size = size;
        this.lastUpdate = lastUpdate;
        this.md5FilePath = md5FilePath;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getStagingId() {
        return stagingId;
    }

    public Path getFilePath() {
        return filePath;
    }

    public long getSize() {
        return size;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public Path getMd5FilePath() {
        return md5FilePath;
    }
}
