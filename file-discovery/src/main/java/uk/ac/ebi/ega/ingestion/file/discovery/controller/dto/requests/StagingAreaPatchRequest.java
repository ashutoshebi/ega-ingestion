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

import javax.validation.constraints.Min;

public class StagingAreaPatchRequest {

    private Boolean discoveryEnabled;

    private Boolean ingestionEnabled;

    @Min(value = 1, message = "Discovery polling period needs to be greater than 0")
    private Long discoveryPollingPeriod;

    @Min(value = 1, message = "Ingestion polling period needs to be greater than 0")
    private Long ingestionPollingPeriod;

    public Boolean getDiscoveryEnabled() {
        return discoveryEnabled;
    }

    public Boolean getIngestionEnabled() {
        return ingestionEnabled;
    }

    public Long getDiscoveryPollingPeriod() {
        return discoveryPollingPeriod;
    }

    public Long getIngestionPollingPeriod() {
        return ingestionPollingPeriod;
    }

}
