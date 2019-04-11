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
package uk.ac.ebi.ega.ingestion.file.manager.models;

public class EgaFile {

    private String id;

    private String dosId;

    private String fileExtension;

    public EgaFile(String id, String dosId, String fileExtension) {
        this.id = id;
        this.dosId = dosId;
        this.fileExtension = fileExtension;
    }

    public String getId() {
        return id;
    }

    public String getDosId() {
        return dosId;
    }

    public String getFileExtension() {
        return fileExtension;
    }

}
