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
package uk.ac.ebi.ega.file.encryption.processor.pipeline;

public class IngestionPipelineResult {

    private IngestionPipelineFile originalFile;

    private String plainMd5;

    private long plainSize;

    private IngestionPipelineFile encryptedFile;

    public IngestionPipelineResult(IngestionPipelineFile originalFile, String plainMd5, long plainSize,
                                   IngestionPipelineFile encryptedFile) {
        this.originalFile = originalFile;
        this.plainMd5 = plainMd5;
        this.plainSize = plainSize;
        this.encryptedFile = encryptedFile;
    }

    public IngestionPipelineFile getOriginalFile() {
        return originalFile;
    }

    public String getPlainMd5() {
        return plainMd5;
    }

    public long getPlainSize() {
        return plainSize;
    }

    public IngestionPipelineFile getEncryptedFile() {
        return encryptedFile;
    }

}
