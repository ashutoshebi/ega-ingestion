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
package uk.ac.ebi.ega.file.re.encryption.processor.persistence.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
@EntityListeners(AuditingEntityListener.class)
public class HistoricProcessDownloadBoxFile {

    private transient boolean persist = true;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String messageId;

    @Column(nullable = false)
    private String instanceId;

    @Column(nullable = false)
    private String resultPath;

    @Column(nullable = false)
    private String dosId;

    private String message;

    public LocalDateTime startTime;

    @CreatedDate
    public LocalDateTime endTime;

    public HistoricProcessDownloadBoxFile() {
    }

    public HistoricProcessDownloadBoxFile(String messageId, String instanceId, String resultPath, String dosId,
                                          LocalDateTime startTime) {
        this(messageId, instanceId, resultPath, dosId, null, startTime);
    }

    public HistoricProcessDownloadBoxFile(String messageId, String instanceId, String resultPath, String dosId,
                                          String message, LocalDateTime startTime) {
        this.messageId = messageId;
        this.instanceId = instanceId;
        this.resultPath = resultPath;
        this.dosId = dosId;
        this.message = message;
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

}
