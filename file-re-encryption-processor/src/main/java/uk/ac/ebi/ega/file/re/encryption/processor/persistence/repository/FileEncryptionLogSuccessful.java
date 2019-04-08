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
package uk.ac.ebi.ega.file.re.encryption.processor.persistence.repository;

import org.springframework.data.annotation.CreatedDate;
import uk.ac.ebi.ega.file.encryption.processor.models.FileEncryptionJob;
import uk.ac.ebi.ega.file.re.encryption.processor.models.FileEncryptionJob;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "FILE_ENCRYPTION_JOB_SUCCESSFUL")
public class FileEncryptionLogSuccessful {

    @Id
    private long id;

    @Column(nullable = false)
    private String instanceId;

    @Column(nullable = false)
    private String account;

    @Column(nullable = false)
    private String stagingAreaId;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String filePathMd5;

    @Column(nullable = false)
    private String md5;

    @Column(nullable = false)
    private String encryptedMd5;

    @Column(nullable = false)
    private LocalDateTime processStart;

    @CreatedDate
    private LocalDateTime processEnd;

    FileEncryptionLogSuccessful() {
    }

    public FileEncryptionLogSuccessful(long id, String instanceId, String account, String stagingAreaId, String filePath,
                                       String filePathMd5, String md5, String encryptedMd5,
                                       LocalDateTime processStart) {
        this.id = id;
        this.instanceId = instanceId;
        this.account = account;
        this.stagingAreaId = stagingAreaId;
        this.filePath = filePath;
        this.filePathMd5 = filePathMd5;
        this.md5 = md5;
        this.encryptedMd5 = encryptedMd5;
        this.processStart = processStart;
    }

    public FileEncryptionLogSuccessful(FileEncryptionJob job, String md5, String encryptedMd5) {
        this(job.getId(), job.getInstanceId(), job.getAccount(), job.getStagingAreaId(), job.getFilePath(),
                job.getFilePathMd5(), md5, encryptedMd5, job.getCreateDate());
    }

}