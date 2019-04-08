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
@Table(name = "FILE_ENCRYPTION_JOB")
public class FileEncryptionJobImpl implements FileEncryptionJob {

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

    @CreatedDate
    private LocalDateTime createDate;

    FileEncryptionJobImpl() {
    }

    public FileEncryptionJobImpl(String instanceId, String account, String stagingAreaId, String filePath,
                                 String filePathMd5) {
        this.instanceId = instanceId;
        this.account = account;
        this.stagingAreaId = stagingAreaId;
        this.filePath = filePath;
        this.filePathMd5 = filePathMd5;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public String getAccount() {
        return account;
    }

    @Override
    public String getStagingAreaId() {
        return stagingAreaId;
    }

    @Override
    public String getFilePath() {
        return filePath;
    }

    @Override
    public String getFilePathMd5() {
        return filePathMd5;
    }

    @Override
    public LocalDateTime getCreateDate() {
        return createDate;
    }

}
