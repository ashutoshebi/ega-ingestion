/*
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
 */
package uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.entity;

import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Class representing the ukbiobank.re_encrypted_files database table.
 */

@Entity
@Table(schema = "UKBIOBANK", name = "RE_ENCRYPTED_FILES")
@EntityListeners(AuditingEntityListener.class)
public class UkBiobankReEncryptedFileEntity implements Persistable<Long> {

    private transient boolean alreadyExistInDb = false;

    @Id
    private long re_encrypted_file_id;

    // The path of the original, encrypted file.
    // "original_file_path REFERENCES ukbiobank.files(file_path)"
    private String originalFilePath;

    // The path of the newly re-encrypted file.
    private String newReEncryptedFilePath;

    // The MD5 of the original, encrypted file.
    private String originalEncryptedMd5;

    // The MD5 of the original, encrypted file, after it has been decrypted.
    // "unencryptedMd5 REFERENCES ukbiobank.files(md5_checksum)"
    private String unencryptedMd5;

    // The MD5 of the newly re-encrypted file.
    private String newReEncryptedMd5;

    // TODO bjuhasz: use @OneToOne
    private int resultStatusId;

    private String resultStatusMessage;
    private String resultStatusException;
    private LocalDateTime startTime;
    private LocalDateTime endTime;


    UkBiobankReEncryptedFileEntity() {
    }

    public UkBiobankReEncryptedFileEntity(final String originalFilePath,
                                          final String newReEncryptedFilePath,
                                          final String originalEncryptedMd5,
                                          final String unencryptedMd5,
                                          final String newReEncryptedMd5,
                                          final Result.Status resultStatus,
                                          final String resultStatusMessage,
                                          final String resultStatusException,
                                          final LocalDateTime startTime,
                                          final LocalDateTime endTime) {
        this.originalFilePath = originalFilePath;
        this.newReEncryptedFilePath = newReEncryptedFilePath;
        this.originalEncryptedMd5 = originalEncryptedMd5;
        this.unencryptedMd5 = unencryptedMd5;
        this.newReEncryptedMd5 = newReEncryptedMd5;
        this.resultStatusId = resultStatus.getEnumValue();
        this.resultStatusMessage = resultStatusMessage;
        this.resultStatusException = resultStatusException;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public Long getId() {
        return re_encrypted_file_id;
    }

    @Override
    public boolean isNew() {
        return !alreadyExistInDb;
    }

    /**
     * Call this function with true, in order to signal
     * that the entity already exists in the DB.
     * @param alreadyExistInDb
     */
    public void setAlreadyExistInDb(boolean alreadyExistInDb) {
        this.alreadyExistInDb = alreadyExistInDb;
    }

    public String getOriginalFilePath() {
        return originalFilePath;
    }

    public void setOriginalFilePath(final String originalFilePath) {
        this.originalFilePath = originalFilePath;
    }

    public String getNewReEncryptedFilePath() {
        return newReEncryptedFilePath;
    }

    public void setNewReEncryptedFilePath(final String newReEncryptedFilePath) {
        this.newReEncryptedFilePath = newReEncryptedFilePath;
    }

    public String getUnencryptedMd5() {
        return unencryptedMd5;
    }

    public void setUnencryptedMd5(final String unencryptedMd5) {
        this.unencryptedMd5 = unencryptedMd5;
    }

    public String getOriginalEncryptedMd5() {
        return originalEncryptedMd5;
    }

    public void setOriginalEncryptedMd5(final String originalEncryptedMd5) {
        this.originalEncryptedMd5 = originalEncryptedMd5;
    }

    public String getNewReEncryptedMd5() {
        return newReEncryptedMd5;
    }

    public void setNewReEncryptedMd5(final String newReEncryptedMd5) {
        this.newReEncryptedMd5 = newReEncryptedMd5;
    }

    public Result.Status getResultStatus() {
        return Result.Status.getStatusBy(resultStatusId);
    }

    public void setResultStatus(final Result.Status resultStatus) {
        this.resultStatusId = resultStatus.getEnumValue();
    }

    public String getResultStatusMessage() {
        return resultStatusMessage;
    }

    public void setResultStatusMessage(final String resultStatusMessage) {
        this.resultStatusMessage = resultStatusMessage;
    }

    public String getResultStatusException() {
        return resultStatusException;
    }

    public void setResultStatusException(final String resultStatusException) {
        this.resultStatusException = resultStatusException;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(final LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "UkBiobankReEncryptedFileEntity{" +
                "originalFilePath='" + originalFilePath + '\'' +
                ", newReEncryptedFilePath='" + newReEncryptedFilePath + '\'' +
                ", unencryptedMd5='" + unencryptedMd5 + '\'' +
                ", originalEncryptedMd5='" + originalEncryptedMd5 + '\'' +
                ", newReEncryptedMd5='" + newReEncryptedMd5 + '\'' +
                ", resultStatusId=" + resultStatusId +
                ", resultStatusMessage='" + resultStatusMessage + '\'' +
                ", resultStatusException='" + resultStatusException + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
