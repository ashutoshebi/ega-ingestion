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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uk.ac.ebi.ega.ingestion.file.manager.kafka.message.EncryptComplete;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Table(name = "ENCRYPT_JOB")
@Entity
@EntityListeners(AuditingEntityListener.class)
public class EncryptJob {

    @Id
    private String jobId;

    @NotNull
    @Column(nullable = false)
    private String fileName;

    @NotNull
    @Column(nullable = false)
    private Long plainSize;

    @NotNull
    @Column(nullable = false)
    private String plainMd5;

    @NotNull
    @Column(nullable = false)
    private Long encryptedSize;

    @NotNull
    @Column(nullable = false)
    private String encryptedMd5;

    @NotNull
    @Column(nullable = false)
    private String encryptionPassword;

    @NotNull
    @Column(nullable = false)
    private String status;

    @NotNull
    @Column(nullable = false)
    private String message;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime startTime;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime endTime;

    @NotNull
    @Column(nullable = false)
    @CreatedDate
    private LocalDateTime createdDate;

    EncryptJob() {
    }

    private EncryptJob(final String jobId, final EncryptComplete encryptComplete) {
        this.jobId = jobId;
        plainSize = encryptComplete.getPlainSize();
        plainMd5 = encryptComplete.getPlainMd5();
        encryptedSize = encryptComplete.getEncryptedSize();
        encryptedMd5 = encryptComplete.getEncryptedMd5();
        encryptionPassword = encryptComplete.getEncryptionPassword();
        status = encryptComplete.getStatus();
        message = encryptComplete.getMessage();
        startTime = encryptComplete.getStartTime();
        endTime = encryptComplete.getEndTime();
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getPlainSize() {
        return plainSize;
    }

    public void setPlainSize(Long plainSize) {
        this.plainSize = plainSize;
    }

    public String getPlainMd5() {
        return plainMd5;
    }

    public void setPlainMd5(String plainMd5) {
        this.plainMd5 = plainMd5;
    }

    public Long getEncryptedSize() {
        return encryptedSize;
    }

    public void setEncryptedSize(Long encryptedSize) {
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

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public static EncryptJob newInstance(final String jobId, final EncryptComplete encryptComplete) {
        return new EncryptJob(jobId, encryptComplete);
    }
}
