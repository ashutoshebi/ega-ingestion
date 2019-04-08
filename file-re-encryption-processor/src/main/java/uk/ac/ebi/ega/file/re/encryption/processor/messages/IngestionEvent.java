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
package uk.ac.ebi.ega.file.re.encryption.processor.messages;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class IngestionEvent implements Comparable<IngestionEvent> {

    private String accountId;

    private String locationId;

    private Path rootPath;

    private String relativePathFile;

    private String relativePathMd5File;

    private long size;

    private long md5Size;

    private LocalDateTime lastModified;

    private LocalDateTime md5LastModified;

    IngestionEvent() {
    }

    public IngestionEvent(String accountId, String locationId, Path rootPath, String relativePathFile,
                          String relativePathMd5File, long size, long md5Size, LocalDateTime lastModified,
                          LocalDateTime md5LastModified) {
        this.accountId = accountId;
        this.locationId = locationId;
        this.rootPath = rootPath;
        this.relativePathFile = relativePathFile;
        this.relativePathMd5File = relativePathMd5File;
        this.size = size;
        this.md5Size = md5Size;
        this.lastModified = lastModified;
        this.md5LastModified = md5LastModified;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getLocationId() {
        return locationId;
    }

    public Path getRootPath() {
        return rootPath;
    }

    public String getRelativePathFile() {
        return relativePathFile;
    }

    public String getRelativePathMd5File() {
        return relativePathMd5File;
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
        return relativePathFile.compareTo(ingestionEvent.relativePathFile);
    }

    public Path getAbsolutePathFile() {
        return rootPath.resolve(relativePathFile);
    }

    public Path getAbsolutePathMd5File() {
        return rootPath.resolve(relativePathMd5File);
    }

}
