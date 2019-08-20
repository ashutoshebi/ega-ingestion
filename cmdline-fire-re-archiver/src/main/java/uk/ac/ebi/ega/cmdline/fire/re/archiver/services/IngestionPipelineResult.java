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
package uk.ac.ebi.ega.cmdline.fire.re.archiver.services;

public class IngestionPipelineResult {

    private IngestionPipelineFile originalFile;

    private String md5;

    private long bytesTransferred;

    private char[] key;

    private IngestionPipelineFile encryptedFile;

    private IngestionPipelineFile encryptedIndexFile;

    public IngestionPipelineResult(IngestionPipelineFile originalFile, String md5, long bytesTransferred, char[] key,
                                   IngestionPipelineFile encryptedFile) {
        this(originalFile, md5, bytesTransferred, key, encryptedFile, null);
    }

    public IngestionPipelineResult(IngestionPipelineFile originalFile, String md5, long bytesTransferred, char[] key,
                                   IngestionPipelineFile encryptedFile, IngestionPipelineFile encryptedIndexFile) {
        this.originalFile = originalFile;
        this.md5 = md5;
        this.bytesTransferred = bytesTransferred;
        this.key = key;
        this.encryptedFile = encryptedFile;
        this.encryptedIndexFile = encryptedIndexFile;
    }

    public IngestionPipelineFile getOriginalFile() {
        return originalFile;
    }

    public String getMd5() {
        return md5;
    }

    public long getBytesTransferred() {
        return bytesTransferred;
    }

    public char[] getKey() {
        return key;
    }

    public IngestionPipelineFile getEncryptedFile() {
        return encryptedFile;
    }

    public IngestionPipelineFile getEncryptedIndexFile() {
        return encryptedIndexFile;
    }

    @Override
    public String toString() {
        return "IngestionPipelineResult{" +
                "originalFile=" + originalFile +
                ", md5='" + md5 + '\'' +
                ", bytesTransferred=" + bytesTransferred +
                ", encryptedFile=" + encryptedFile +
                ", encryptedIndexFile=" + encryptedIndexFile +
                '}';
    }
}
