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

import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uk.ac.ebi.ega.file.encryption.processor.models.IngestionProcess;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "ENCRYPT_PARAMETERS")
@EntityListeners(AuditingEntityListener.class)
public class EncryptParametersEntity {

    @Id
    private String jobId;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String stagingId;

    @Column(nullable = false)
    private String gpgPath;

    @Column(nullable = false)
    private String gpgStagingPath;

    private long gpgSize;

    private long gpgLastModified;

    @Column(name = "md5_path", nullable = false)
    private String md5Path;

    @Column(name = "md5_staging_path", nullable = false)
    private String md5StagingPath;

    @Column(name = "md5_size")
    private long md5Size;

    @Column(name = "md5_last_modified")
    private long md5LastModified;

    @Column(name = "gpg_md5_path", nullable = false)
    private String gpgMd5Path;

    @Column(name = "gpg_md5_staging_path", nullable = false)
    private String gpgMd5StagingPath;

    @Column(name = "gpg_md5_size")
    private long gpgMd5Size;

    @Column(name = "gpg_md5_last_modified")
    private long gpgMd5LastModified;

    @Column(nullable = false)
    private String resultPath;

    private LocalDateTime createDate;

    public EncryptParametersEntity() {
    }

    public EncryptParametersEntity(String jobId, String accountId, String stagingId,
                                   String gpgPath, String gpgStagingPath, long gpgSize, long gpgLastModified,
                                   String md5Path, String md5StagingPath, long md5Size, long md5LastModified,
                                   String gpgMd5Path, String gpgMd5StagingPath, long gpgMd5Size,
                                   long gpgMd5LastModified,
                                   String resultPath) {
        this.jobId = jobId;
        this.accountId = accountId;
        this.stagingId = stagingId;
        this.gpgPath = gpgPath;
        this.gpgStagingPath = gpgStagingPath;
        this.gpgSize = gpgSize;
        this.gpgLastModified = gpgLastModified;
        this.md5Path = md5Path;
        this.md5StagingPath = md5StagingPath;
        this.md5Size = md5Size;
        this.md5LastModified = md5LastModified;
        this.gpgMd5Path = gpgMd5Path;
        this.gpgMd5StagingPath = gpgMd5StagingPath;
        this.gpgMd5Size = gpgMd5Size;
        this.gpgMd5LastModified = gpgMd5LastModified;
        this.resultPath = resultPath;
        this.createDate = LocalDateTime.now();
    }

    public EncryptParametersEntity(String jobId, IngestionProcess jobParameters) {
        this(
                jobId,
                jobParameters.getAccountId(),
                jobParameters.getLocationId(),
                jobParameters.getEncryptedFile().getFile().getAbsolutePath(),
                jobParameters.getEncryptedFile().getStagingFile().getAbsolutePath(),
                jobParameters.getEncryptedFile().getSize(),
                jobParameters.getEncryptedFile().getLastModified(),
                jobParameters.getPlainMd5File().getFile().getAbsolutePath(),
                jobParameters.getPlainMd5File().getStagingFile().getAbsolutePath(),
                jobParameters.getPlainMd5File().getSize(),
                jobParameters.getPlainMd5File().getLastModified(),
                jobParameters.getEncryptedMd5File().getFile().getAbsolutePath(),
                jobParameters.getEncryptedMd5File().getStagingFile().getAbsolutePath(),
                jobParameters.getEncryptedMd5File().getSize(),
                jobParameters.getEncryptedMd5File().getLastModified(),
                jobParameters.getOutputFile().getAbsolutePath()
        );
    }

    public String getJobId() {
        return jobId;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getStagingId() {
        return stagingId;
    }

    public String getGpgPath() {
        return gpgPath;
    }

    public String getGpgStagingPath() {
        return gpgStagingPath;
    }

    public long getGpgSize() {
        return gpgSize;
    }

    public long getGpgLastModified() {
        return gpgLastModified;
    }

    public String getMd5Path() {
        return md5Path;
    }

    public String getMd5StagingPath() {
        return md5StagingPath;
    }

    public long getMd5Size() {
        return md5Size;
    }

    public long getMd5LastModified() {
        return md5LastModified;
    }

    public String getGpgMd5Path() {
        return gpgMd5Path;
    }

    public String getGpgMd5StagingPath() {
        return gpgMd5StagingPath;
    }

    public long getGpgMd5Size() {
        return gpgMd5Size;
    }

    public long getGpgMd5LastModified() {
        return gpgMd5LastModified;
    }

    public String getResultPath() {
        return resultPath;
    }

}
