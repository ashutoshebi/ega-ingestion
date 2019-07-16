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

import uk.ac.ebi.ega.ingestion.commons.models.FileStatic;

import java.nio.file.Path;
import java.time.LocalDateTime;

public class IngestionEvent implements Comparable<IngestionEvent> {

    private String accountId;

    private String locationId;

    private Path rootPath;

    private FileStatic encryptedFile;

    private FileStatic plainMd5File;

    private FileStatic encryptedMd5File;

    private LocalDateTime timestamp;

    public IngestionEvent() {
    }

    public IngestionEvent(String accountId, String locationId, Path rootPath, FileStatic encryptedFile,
                          FileStatic plainMd5File, FileStatic encryptedMd5File) {
        this.accountId = accountId;
        this.locationId = locationId;
        this.rootPath = rootPath;
        this.encryptedFile = encryptedFile;
        this.plainMd5File = plainMd5File;
        this.encryptedMd5File = encryptedMd5File;
        this.timestamp = LocalDateTime.now();
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

    public FileStatic getEncryptedFile() {
        return encryptedFile;
    }

    public FileStatic getPlainMd5File() {
        return plainMd5File;
    }

    public FileStatic getEncryptedMd5File() {
        return encryptedMd5File;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public void setRootPath(Path rootPath) {
        this.rootPath = rootPath;
    }

    public void setEncryptedFile(FileStatic encryptedFile) {
        this.encryptedFile = encryptedFile;
    }

    public void setPlainMd5File(FileStatic plainMd5File) {
        this.plainMd5File = plainMd5File;
    }

    public void setEncryptedMd5File(FileStatic encryptedMd5File) {
        this.encryptedMd5File = encryptedMd5File;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(IngestionEvent ingestionEvent) {
        int account = accountId.compareTo(ingestionEvent.accountId);
        if (account != 0) {
            return account;
        }
        int location = locationId.compareTo(ingestionEvent.locationId);
        if (location != 0) {
            return location;
        }
        return encryptedFile.compareTo(ingestionEvent.encryptedFile);
    }

}
