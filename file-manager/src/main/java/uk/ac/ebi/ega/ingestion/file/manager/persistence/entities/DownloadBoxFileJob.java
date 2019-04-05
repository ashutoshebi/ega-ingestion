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

import org.hibernate.annotations.Type;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class DownloadBoxFileJob {

    @Id
    private String id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "JOB_ID", nullable = false)
    private DownloadBoxJob downloadBoxJob;

    @Column(nullable = false)
    private String fileId;

    @Column(nullable = false)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Type(type = "uk.ac.ebi.ega.ingestion.file.manager.utils.PGEnumUserType", parameters = {
            @org.hibernate.annotations.Parameter(name = "enumClassName",
                    value = "uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.JobStatus")})
    private JobStatus status;

    private String errorMessage;

    private LocalDateTime processStart;

    private LocalDateTime processEnd;

    DownloadBoxFileJob() {
    }

    public DownloadBoxFileJob(DownloadBoxJob downloadBoxJob, String fileId, String filePath) {
        this.id = downloadBoxJob.getId() + "_" + fileId;
        this.downloadBoxJob = downloadBoxJob;
        this.fileId = fileId;
        this.filePath = filePath;
        this.status = JobStatus.PENDING;
    }

    public String getId() {
        return id;
    }

    public DownloadBoxJob getDownloadBoxJob() {
        return downloadBoxJob;
    }

    public String getFileId() {
        return fileId;
    }

    public String getFilePath() {
        return filePath;
    }

    public LocalDateTime getProcessStart() {
        return processStart;
    }

    public LocalDateTime getProcessEnd() {
        return processEnd;
    }

    public void setProcessStart(LocalDateTime processStart) {
        this.processStart = processStart;
    }

    public void setProcessEnd(LocalDateTime processEnd) {
        this.processEnd = processEnd;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void finish(LocalDateTime processStart, LocalDateTime processEnd) {
        status = JobStatus.COMPLETED;
        this.processStart = processStart;
        this.processEnd = processEnd;
    }

    public void error(String error, LocalDateTime processStart, LocalDateTime processEnd) {
        status = JobStatus.ERROR;
        this.processStart = processStart;
        this.processEnd = processEnd;
    }

}
