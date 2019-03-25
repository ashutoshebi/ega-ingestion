/*
 *
 * Copyright 2018 EMBL - European Bioinformatics Institute
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
package uk.ac.ebi.ega.ingestion.file.discovery.persistence.repositories;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingArea;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity(name = "STAGING_AREAS")
@EntityListeners(AuditingEntityListener.class)
public class StagingAreaImpl implements StagingArea, Persistable<String> {

    private transient boolean forceInsert;

    @Id
    private String id;

    private String path;

    private String ignorePathRegex;

    private boolean discoveryEnabled;

    private boolean ingestionEnabled;

    private String account;

    private long discoveryPollingPeriod;

    private long ingestionPollingPeriod;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createDate;

    @LastModifiedDate
    private LocalDateTime updateDate;

    public StagingAreaImpl() {
        this(false);
    }

    public StagingAreaImpl(boolean forceInsert) {
        forceInsert = true;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return forceInsert;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isDiscoveryEnabled() {
        return discoveryEnabled;
    }

    @Override
    public boolean isIngestionEnabled() {
        return ingestionEnabled;
    }

    @Override
    public String getAccount() {
        return account;
    }

    @Override
    public long getDiscoveryPollingPeriod() {
        return discoveryPollingPeriod;
    }

    @Override
    public long getIngestionPollingPeriod() {
        return ingestionPollingPeriod;
    }

    @Override
    public LocalDateTime getCreateDate() {
        return createDate;
    }

    @Override
    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    @Override
    public String getIgnorePathRegex() {
        return ignorePathRegex;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setIgnorePathRegex(String ignorePathRegex) {
        this.ignorePathRegex = ignorePathRegex;
    }

    public void setDiscoveryEnabled(boolean discoveryEnabled) {
        this.discoveryEnabled = discoveryEnabled;
    }

    public void setIngestionEnabled(boolean ingestionEnabled) {
        this.ingestionEnabled = ingestionEnabled;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setDiscoveryPollingPeriod(long discoveryPollingPeriod) {
        this.discoveryPollingPeriod = discoveryPollingPeriod;
    }

    public void setIngestionPollingPeriod(long ingestionPollingPeriod) {
        this.ingestionPollingPeriod = ingestionPollingPeriod;
    }

    public void setCreateDate(LocalDateTime createDate) {
        this.createDate = createDate;
    }

    public void setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
    }

}
