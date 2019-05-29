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

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "FILES")
@EntityListeners(AuditingEntityListener.class)
public class UkBiobankFileEntity implements Persistable<String> {

    private transient boolean persist = true;

    @Id
    private String filePath;

    private String fileName;
    private long fileSize;
    private String md5Checksum;
    private String project;
    private String sampleId;
    private String sex;
    private String ethnicity;

    UkBiobankFileEntity() {
    }

    public UkBiobankFileEntity(final String filePath,
                               final String fileName,
                               final long fileSize,
                               final String md5Checksum,
                               final String project,
                               final String sampleId,
                               final String sex,
                               final String ethnicity) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.md5Checksum = md5Checksum;
        this.project = project;
        this.sampleId = sampleId;
        this.sex = sex;
        this.ethnicity = ethnicity;
    }

    @Override
    public String getId() {
        return filePath;
    }

    @Override
    public boolean isNew() {
        return persist;
    }

    public void setPersist(boolean persist) {
        this.persist = persist;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getMd5Checksum() {
        return md5Checksum;
    }

    public String getProject() {
        return project;
    }

    public String getSampleId() {
        return sampleId;
    }

    public String getSex() {
        return sex;
    }

    public String getEthnicity() {
        return ethnicity;
    }
}
