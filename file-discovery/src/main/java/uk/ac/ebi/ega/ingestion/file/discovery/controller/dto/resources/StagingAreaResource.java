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
package uk.ac.ebi.ega.ingestion.file.discovery.controller.dto.resources;

import org.springframework.hateoas.ResourceSupport;

import java.time.LocalDateTime;

public class StagingAreaResource extends ResourceSupport {

    public String path;

    public boolean discoveryEnabled;

    public boolean ingestionEnabled;

    public String account;

    public long discoveryPollingPeriod;

    public long ingestionPollingPeriod;

    public LocalDateTime createDate;

    public LocalDateTime updateDate;

    public void setPath(String path) {
        this.path = path;
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
