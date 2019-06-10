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
package uk.ac.ebi.ega.ukbb.temp.ingestion.reencryption;

import java.io.File;

public class ReEncryptionResult {

    private String originalEncryptedMd5;
    private String unencryptedMd5;
    private String newReEncryptedMd5;
    private File outputFile;
    private long unencryptedSize;

    public ReEncryptionResult(String originalEncryptedMd5, String unencryptedMd5,
                              String newReEncryptedMd5, File outputFile, long unencryptedSize) {
        this.originalEncryptedMd5 = originalEncryptedMd5;
        this.unencryptedMd5 = unencryptedMd5;
        this.newReEncryptedMd5 = newReEncryptedMd5;
        this.outputFile = outputFile;
        this.unencryptedSize = unencryptedSize;
    }

    public String getOriginalEncryptedMd5() {
        return originalEncryptedMd5;
    }

    public String getUnencryptedMd5() {
        return unencryptedMd5;
    }

    public String getNewReEncryptedMd5() {
        return newReEncryptedMd5;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public long getUnencryptedSize() {
        return unencryptedSize;
    }

}
