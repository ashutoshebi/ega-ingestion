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
package uk.ac.ebi.ega.ingestion.file.discovery.controller.dto.requests;

import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingArea;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class StagingAreaPostRequest implements StagingArea {

    @NotNull
    private String id;

    @NotNull
    private String path;

    private String ignorePathRegex;

    @NotNull
    private String account;

    @NotNull
    @Min(value = 1, message = "Discovery polling period needs to be greater than 0")
    private long discoveryPollingPeriod;

    @NotNull
    @Min(value = 1, message = "Ingestion polling period needs to be greater than 0")
    private long ingestionPollingPeriod;

    public String getId() {
        return id;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getIgnorePathRegex() {
        return ignorePathRegex;
    }

    @Override
    public boolean isDiscoveryEnabled() {
        return false;
    }

    @Override
    public boolean isIngestionEnabled() {
        return false;
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
        return null;
    }

    @Override
    public LocalDateTime getUpdateDate() {
        return null;
    }

}
