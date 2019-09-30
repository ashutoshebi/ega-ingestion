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
package uk.ac.ebi.ega.ingestion.commons.messages;

import java.util.Objects;

public class ArchiveEventSimplify {

    //Temporary class. Will be replaced with ArchiveEvent.

    private long plainSize;
    private String encryptedFilePath;
    private String encryptedMD5;
    private long encryptedSize;

    protected ArchiveEventSimplify() {
    }

    public ArchiveEventSimplify(final long plainSize, final String encryptedFilePath,
                                final String encryptedMD5, final long encryptedSize) {
        this.plainSize = plainSize;
        this.encryptedFilePath = Objects.requireNonNull(encryptedFilePath);
        this.encryptedMD5 = Objects.requireNonNull(encryptedMD5);
        this.encryptedSize = encryptedSize;
    }

    public long getPlainSize() {
        return plainSize;
    }

    public String getEncryptedFilePath() {
        return encryptedFilePath;
    }

    public String getEncryptedMD5() {
        return encryptedMD5;
    }

    public long getEncryptedSize() {
        return encryptedSize;
    }

    @Override
    public String toString() {
        return "ArchiveEventSimplify{" +
                "plainSize=" + plainSize +
                ", encryptedFilePath='" + encryptedFilePath + '\'' +
                ", encryptedMD5='" + encryptedMD5 + '\'' +
                ", encryptedSize=" + encryptedSize +
                '}';
    }
}
