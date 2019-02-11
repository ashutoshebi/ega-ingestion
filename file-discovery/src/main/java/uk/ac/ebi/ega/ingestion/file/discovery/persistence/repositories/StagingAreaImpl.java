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

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingArea;

@Table("STAGING_AREAS")
public class StagingAreaImpl implements StagingArea {

    @Id
    private String id;

    private String path;

    private boolean enabled;

    private String account;

    private long pollingPeriod;

    private long filesPerPoll;

    public StagingAreaImpl(String id, String path, boolean enabled, String account, long pollingPeriod,
                           long filesPerPoll) {
        this.id = id;
        this.path = path;
        this.enabled = enabled;
        this.account = account;
        this.pollingPeriod = pollingPeriod;
        this.filesPerPoll = filesPerPoll;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String getAccount() {
        return account;
    }

    @Override
    public long getPollingPeriod() {
        return pollingPeriod;
    }

    @Override
    public long getFilesPerPoll() {
        return filesPerPoll;
    }
}
