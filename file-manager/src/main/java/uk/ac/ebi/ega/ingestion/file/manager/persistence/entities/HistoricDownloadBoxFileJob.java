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

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class HistoricDownloadBoxFileJob {

    @Id
    private String id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "JOB_ID", nullable = false)
    private HistoricDownloadBoxJob downloadBoxJob;

    @Column(nullable = false)
    private String fileId;

    @Column(nullable = false)
    private String fileExtension;

    @Column(nullable = false)
    private String dosId;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime processStart;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime processEnd;

    HistoricDownloadBoxFileJob() {
    }

    public HistoricDownloadBoxFileJob(HistoricDownloadBoxJob historicDownloadBoxJob,
                                      DownloadBoxFileJob downloadBoxFileJob) {
        this.id = downloadBoxFileJob.getId();
        this.downloadBoxJob = historicDownloadBoxJob;
        this.fileId = downloadBoxFileJob.getFileId();
        this.fileExtension = downloadBoxFileJob.getFileExtension();
        this.dosId = downloadBoxFileJob.getDosId();
        this.processStart = downloadBoxFileJob.getProcessStart();
        this.processEnd = downloadBoxFileJob.getProcessEnd();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public HistoricDownloadBoxJob getDownloadBoxJob() {
        return downloadBoxJob;
    }

    public void setDownloadBoxJob(HistoricDownloadBoxJob downloadBoxJob) {
        this.downloadBoxJob = downloadBoxJob;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getDosId() {
        return dosId;
    }

    public void setDosId(String dosId) {
        this.dosId = dosId;
    }

    public LocalDateTime getProcessStart() {
        return processStart;
    }

    public void setProcessStart(LocalDateTime processStart) {
        this.processStart = processStart;
    }

    public LocalDateTime getProcessEnd() {
        return processEnd;
    }

    public void setProcessEnd(LocalDateTime processEnd) {
        this.processEnd = processEnd;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

}
