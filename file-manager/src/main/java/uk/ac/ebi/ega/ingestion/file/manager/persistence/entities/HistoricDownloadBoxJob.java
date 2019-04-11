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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class HistoricDownloadBoxJob {

    @Id
    private Long id;

    @NotNull
    @Column(nullable = false)
    private String datasetId;

    @NotNull
    @Column(nullable = false)
    private String boxId;

    @NotNull
    @Column(nullable = false)
    private String userId;

    @NotNull
    @Column(nullable = false)
    private String boxPath;

    @NotNull
    @Column(nullable = false)
    private String ticketId;

    @Column(nullable = false)
    private String password;

    @NotNull
    @Column(nullable = false)
    private String generatedBy;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime startDate;

    @NotNull
    @Column(nullable = false)
    @CreatedDate
    private LocalDateTime endDate;

    HistoricDownloadBoxJob() {
    }

    public HistoricDownloadBoxJob(DownloadBoxJob downloadBoxJob) {
        this.id = downloadBoxJob.getId();
        this.datasetId = downloadBoxJob.getDatasetId();
        this.boxId = downloadBoxJob.getAssignedDownloadBox().getBoxId();
        this.userId = downloadBoxJob.getAssignedDownloadBox().getUserId();
        this.boxPath = downloadBoxJob.getAssignedDownloadBox().getDownloadBox().getPath();
        this.ticketId = downloadBoxJob.getTicketId();
        this.password = downloadBoxJob.getPassword();
        this.generatedBy = downloadBoxJob.getGeneratedBy();
        this.startDate = downloadBoxJob.getStartDate();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getBoxId() {
        return boxId;
    }

    public void setBoxId(String boxId) {
        this.boxId = boxId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getBoxPath() {
        return boxPath;
    }

    public void setBoxPath(String boxPath) {
        this.boxPath = boxPath;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }
}
