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
package uk.ac.ebi.ega.ingestion.file.discovery.message;

import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingFile;

import java.nio.file.Path;
import java.time.LocalDateTime;

public class IngestionEvent implements Comparable<IngestionEvent> {

    private String accountId;

    private String locationId;

    private Path absolutePathFile;

    private Path absolutePathMd5File;

    private long size;

    private long md5Size;

    private LocalDateTime lastModified;

    private LocalDateTime md5LastModified;

    public IngestionEvent(String accountId, String locationId, Path absolutePathFile, Path absolutePathMd5File,
                          long size, long md5Size, LocalDateTime lastModified, LocalDateTime md5LastModified) {
        this.accountId = accountId;
        this.locationId = locationId;
        this.absolutePathFile = absolutePathFile;
        this.absolutePathMd5File = absolutePathMd5File;
        this.size = size;
        this.md5Size = md5Size;
        this.lastModified = lastModified;
        this.md5LastModified = md5LastModified;
    }

    public IngestionEvent(String accountId, String locationId, Path rootPath, StagingFile stagingFile,
                          StagingFile stagingFileMd5) {
        this(accountId, locationId, rootPath.resolve(stagingFile.getRelativePath()),
                rootPath.resolve(stagingFileMd5.getRelativePath()), stagingFile.getFileSize(),
                stagingFileMd5.getFileSize(), stagingFile.getUpdateDate(), stagingFileMd5.getUpdateDate());
    }

    public String getAccountId() {
        return accountId;
    }

    public String getLocationId() {
        return locationId;
    }

    public Path getAbsolutePathFile() {
        return absolutePathFile;
    }

    public Path getAbsolutePathMd5File() {
        return absolutePathMd5File;
    }

    public long getSize() {
        return size;
    }

    public long getMd5Size() {
        return md5Size;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public LocalDateTime getMd5LastModified() {
        return md5LastModified;
    }

    @Override
    public String toString() {
        return "IngestionEvent{" +
                "accountId='" + accountId + '\'' +
                ", locationId='" + locationId + '\'' +
                ", absolutePathFile=" + absolutePathFile +
                ", absolutePathMd5File=" + absolutePathMd5File +
                ", size=" + size +
                ", md5Size=" + md5Size +
                ", lastModified=" + lastModified +
                ", md5LastModified=" + md5LastModified +
                '}';
    }

    @Override
    public int compareTo(IngestionEvent ingestionEvent) {
        int older = lastModified.compareTo(ingestionEvent.lastModified);
        if (older != 0) {
            return older;
        }
        int account = accountId.compareTo(ingestionEvent.accountId);
        if (account != 0) {
            return account;
        }
        int location = locationId.compareTo(ingestionEvent.locationId);
        if (location != 0) {
            return location;
        }
        return absolutePathFile.compareTo(ingestionEvent.absolutePathFile);
    }
}
