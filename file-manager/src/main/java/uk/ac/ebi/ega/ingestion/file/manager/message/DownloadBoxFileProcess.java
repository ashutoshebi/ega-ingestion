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
package uk.ac.ebi.ega.ingestion.file.manager.message;

import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.DownloadBoxFileJob;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.DownloadBoxJob;

public class DownloadBoxFileProcess {

    private String boxPath;

    private String datasetId;

    private String filePath;

    private String password;

    public DownloadBoxFileProcess(DownloadBoxJob boxJob, DownloadBoxFileJob boxFileJob) {
        this.boxPath = boxJob.getAssignedDownloadBox().getDownloadBox().getPath();
        this.datasetId = boxJob.getDatasetId();
        this.filePath = boxFileJob.getFilePath();
        this.password = boxJob.getPassword();
    }

    public String getBoxPath() {
        return boxPath;
    }

    public void setBoxPath(String boxPath) {
        this.boxPath = boxPath;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
