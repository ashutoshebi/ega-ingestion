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
import uk.ac.ebi.ega.ingestion.commons.models.Encryption;

import java.nio.file.Path;

public class NewFileEvent {

    private String accountId;

    private String locationId;

    private String userPath;

    @JsonProperty
    private long lastModified;

    private Path path;

    private String plainMd5;

    private String encryptedMd5;

    private Encryption encryption;

    public NewFileEvent() {
    }

    public NewFileEvent(String accountId, String locationId, String userPath, long lastModified, Path path,
                        String plainMd5, String encryptedMd5, Encryption encryption) {
        this.accountId = accountId;
        this.locationId = locationId;
        this.userPath = userPath;
        this.lastModified = lastModified;
        this.path = path;
        this.plainMd5 = plainMd5;
        this.encryptedMd5 = encryptedMd5;
        this.encryption = encryption;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getUserPath() {
        return userPath;
    }

    public void setUserPath(String userPath) {
        this.userPath = userPath;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public String getPlainMd5() {
        return plainMd5;
    }

    public void setPlainMd5(String plainMd5) {
        this.plainMd5 = plainMd5;
    }

    public String getEncryptedMd5() {
        return encryptedMd5;
    }

    public void setEncryptedMd5(String encryptedMd5) {
        this.encryptedMd5 = encryptedMd5;
    }

    public Encryption getEncryption() {
        return encryption;
    }

    public void setEncryption(Encryption encryption) {
        this.encryption = encryption;
    }

    @Override
    public String toString() {
        return "NewFileEvent{" +
                "accountId='" + accountId + '\'' +
                ", locationId='" + locationId + '\'' +
                ", userPath='" + userPath + '\'' +
                ", lastModified=" + lastModified +
                ", path=" + path +
                ", plainMd5='" + plainMd5 + '\'' +
                ", encryptedMd5='" + encryptedMd5 + '\'' +
                ", encryption=" + encryption +
                '}';
    }
}
