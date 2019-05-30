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
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "RE_ENCRYPTED_FILES")
@EntityListeners(AuditingEntityListener.class)
public class UkBiobankReEncryptedFileEntity implements Persistable<String> {

    private transient boolean persist = true;

    // TODO bjuhasz: which column should be the primary key?
    //@Id
    private String originalFilePath;
    private String newReEncryptedFilePath;

    private String unencryptedMd5;
    private String originalEncryptedMd5;
    private String newReEncryptedMd5;

    private Result.Status resultStatus;
    private String resultStatusMessage;
    private String resultStatusException;
    private LocalDateTime startTime;
    private LocalDateTime endTime;


    UkBiobankReEncryptedFileEntity() {
    }

    public UkBiobankReEncryptedFileEntity(final String originalFilePath,
                                          final String newReEncryptedFilePath,
                                          final String unencryptedMd5,
                                          final String originalEncryptedMd5,
                                          final String newReEncryptedMd5,
                                          final Result.Status resultStatus,
                                          final String resultStatusMessage,
                                          final String resultStatusException,
                                          final LocalDateTime startTime,
                                          final LocalDateTime endTime) {
        this.originalFilePath = originalFilePath;
        this.newReEncryptedFilePath = newReEncryptedFilePath;
        this.unencryptedMd5 = unencryptedMd5;
        this.originalEncryptedMd5 = originalEncryptedMd5;
        this.newReEncryptedMd5 = newReEncryptedMd5;
        this.resultStatus = resultStatus;
        this.resultStatusMessage = resultStatusMessage;
        this.resultStatusException = resultStatusException;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Override
    public String getId() {
        return originalFilePath;
    }

    @Override
    public boolean isNew() {
        return persist;
    }

    public void setPersist(boolean persist) {
        this.persist = persist;
    }

    public String getOriginalFilePath() {
        return originalFilePath;
    }

    public String getNewReEncryptedFilePath() {
        return newReEncryptedFilePath;
    }

    public String getUnencryptedMd5() {
        return unencryptedMd5;
    }

    public String getOriginalEncryptedMd5() {
        return originalEncryptedMd5;
    }

    public String getNewReEncryptedMd5() {
        return newReEncryptedMd5;
    }

    public Result.Status getResultStatus() {
        return resultStatus;
    }

    public String getResultStatusMessage() {
        return resultStatusMessage;
    }

    public String getResultStatusException() {
        return resultStatusException;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}
