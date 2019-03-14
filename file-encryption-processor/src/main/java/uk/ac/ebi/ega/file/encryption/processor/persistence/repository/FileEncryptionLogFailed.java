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
package uk.ac.ebi.ega.file.encryption.processor.persistence.repository;

import org.springframework.data.annotation.CreatedDate;
import uk.ac.ebi.ega.file.encryption.processor.models.FileEncryptionJob;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "FILE_ENCRYPTION_JOB_FAILED")
public class FileEncryptionLogFailed {

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
    private LocalDateTime processStart;

    @CreatedDate
    private LocalDateTime processEnd;

    private String errorMessage;

    FileEncryptionLogFailed() {
    }

    public FileEncryptionLogFailed(long id, String instanceId, String account, String stagingAreaId, String filePath,
                                   String filePathMd5, LocalDateTime processStart, String errorMessage) {
        this.id = id;
        this.instanceId = instanceId;
        this.account = account;
        this.stagingAreaId = stagingAreaId;
        this.filePath = filePath;
        this.filePathMd5 = filePathMd5;
        this.processStart = processStart;
        this.errorMessage = errorMessage;
    }

    public FileEncryptionLogFailed(FileEncryptionJob job, Exception e) {
        this(job.getId(), job.getInstanceId(), job.getAccount(), job.getStagingAreaId(), job.getFilePath(),
                job.getFilePathMd5(), job.getCreateDate(), e.getMessage());
    }

}