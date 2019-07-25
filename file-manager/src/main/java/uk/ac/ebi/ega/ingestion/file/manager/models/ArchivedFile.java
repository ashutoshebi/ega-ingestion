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

public class ArchivedFile {

    private final String accountId;

    private final String stagingAreaId;

    private final Long fireId;

    private final String dosPath;

    private final String path;

    private final long plainSize;

    private final String plainMd5;

    private final long encryptedSize;

    private final String encryptedMd5;

    private final char[] key;

    public ArchivedFile(String accountId, String stagingAreaId, long fireId, String dosPath, String path,
                        long plainSize, String plainMd5, long encryptedSize, String encryptedMd5, char[] key) {
        this.accountId = accountId;
        this.stagingAreaId = stagingAreaId;
        this.fireId = fireId;
        this.dosPath = dosPath;
        this.path = path;
        this.plainSize = plainSize;
        this.plainMd5 = plainMd5;
        this.encryptedSize = encryptedSize;
        this.encryptedMd5 = encryptedMd5;
        this.key = key;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getStagingAreaId() {
        return stagingAreaId;
    }

    public long getFireId() {
        return fireId;
    }

    public String getDosPath() {
        return dosPath;
    }

    public String getPath() {
        return path;
    }

    public long getPlainSize() {
        return plainSize;
    }

    public String getPlainMd5() {
        return plainMd5;
    }

    public long getEncryptedSize() {
        return encryptedSize;
    }

    public String getEncryptedMd5() {
        return encryptedMd5;
    }

    public char[] getKey() {
        return key;
    }

}
