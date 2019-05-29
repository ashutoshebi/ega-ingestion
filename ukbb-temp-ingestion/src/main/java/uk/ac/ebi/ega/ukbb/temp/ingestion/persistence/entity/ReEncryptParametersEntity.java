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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "RE_ENCRYPT_PARAMETERS")
@EntityListeners(AuditingEntityListener.class)
public class ReEncryptParametersEntity implements Persistable<String> {

    private transient boolean persist = true;

    @Id
    private String jobId;

    @Column(nullable = false)
    private String resultPath;

    @Column(nullable = false)
    private String dosId;

    @Column(nullable = false)
    private String encryptedPassword;

    ReEncryptParametersEntity() {
    }

    public ReEncryptParametersEntity(String jobId, String resultPath, String dosId, String encryptedPassword) {
        this.jobId = jobId;
        this.resultPath = resultPath;
        this.dosId = dosId;
        this.encryptedPassword = encryptedPassword;
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

    public String getResultPath() {
        return resultPath;
    }

    public String getDosId() {
        return dosId;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }
}
