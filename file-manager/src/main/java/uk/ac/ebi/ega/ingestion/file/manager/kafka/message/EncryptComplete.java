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
package uk.ac.ebi.ega.ingestion.file.manager.kafka.message;

import java.time.LocalDateTime;

public class EncryptComplete {

    private String fileName;
    private long plainSize;
    private String plainMd5;
    private long encryptedSize;
    private String encryptedMd5;
    private String encryptionPassword;
    private String status;
    private String message;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getPlainSize() {
        return plainSize;
    }

    public void setPlainSize(long plainSize) {
        this.plainSize = plainSize;
    }

    public String getPlainMd5() {
        return plainMd5;
    }

    public void setPlainMd5(String plainMd5) {
        this.plainMd5 = plainMd5;
    }

    public long getEncryptedSize() {
        return encryptedSize;
    }

    public void setEncryptedSize(long encryptedSize) {
        this.encryptedSize = encryptedSize;
    }

    public String getEncryptedMd5() {
        return encryptedMd5;
    }

    public void setEncryptedMd5(String encryptedMd5) {
        this.encryptedMd5 = encryptedMd5;
    }

    public String getEncryptionPassword() {
        return encryptionPassword;
    }

    public void setEncryptionPassword(String encryptionPassword) {
        this.encryptionPassword = encryptionPassword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
