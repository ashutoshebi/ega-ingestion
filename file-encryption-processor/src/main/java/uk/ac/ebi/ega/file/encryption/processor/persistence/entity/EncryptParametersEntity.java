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
package uk.ac.ebi.ega.file.encryption.processor.persistence.entity;

import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ENCRYPT_PARAMETERS")
@EntityListeners(AuditingEntityListener.class)
public class EncryptParametersEntity implements Persistable<String> {

    private transient boolean persist = true;

    @Id
    private String jobId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String stagingId;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private Long size;

    @Column(nullable = false)
    private LocalDateTime lastUpdate;

    @Column(nullable = false)
    private String md5FilePath;

    EncryptParametersEntity() {
    }

    public EncryptParametersEntity(String jobId, String accountId, String stagingId, String filePath, LocalDateTime lastUpdate, String md5FilePath) {
        this.jobId = jobId;
        this.accountId = accountId;
        this.stagingId = stagingId;
        this.filePath = filePath;
        this.lastUpdate = lastUpdate;
        this.md5FilePath = md5FilePath;
    }

    @Override
    public String getId() {
        return jobId;
    }

    @Override
    public boolean isNew() {
        return persist;
    }

    public void setPersist(boolean persist) {
        this.persist = persist;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getStagingId() {
        return stagingId;
    }

    public String getFilePath() {
        return filePath;
    }

    public Long getSize() {
        return size;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public String getMd5FilePath() {
        return md5FilePath;
    }
}
