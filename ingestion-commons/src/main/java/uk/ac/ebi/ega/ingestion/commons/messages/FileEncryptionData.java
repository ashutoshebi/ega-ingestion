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

import uk.ac.ebi.ega.ingestion.commons.models.Encryption;

import java.net.URI;
import java.util.Objects;

public class FileEncryptionData {

    private URI uri;

    private long plainSize;

    private String encryptedMD5;

    private long encryptedSize;

    private Encryption encryptionType;

    private String encryptionKey;

    FileEncryptionData() {
    }

    public FileEncryptionData(final long plainSize, final URI uri, final String encryptedMD5,
                              final long encryptedSize, final String encryptionKey, final Encryption encryptionType) {
        this.plainSize = plainSize;
        this.uri = Objects.requireNonNull(uri);
        this.encryptedMD5 = Objects.requireNonNull(encryptedMD5);
        this.encryptedSize = encryptedSize;
        this.encryptionKey = encryptionKey;
        this.encryptionType = encryptionType;
    }

    public long getPlainSize() {
        return plainSize;
    }

    public URI getUri() {
        return uri;
    }

    public String getEncryptedMD5() {
        return encryptedMD5;
    }

    public long getEncryptedSize() {
        return encryptedSize;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public Encryption getEncryptionType() {
        return encryptionType;
    }

    @Override
    public String toString() {
        return "ArchiveEventSimplify{" +
                "uri=" + uri +
                ", plainSize=" + plainSize +
                ", encryptedMD5='" + encryptedMD5 + '\'' +
                ", encryptedSize=" + encryptedSize +
                ", encryptionKey='**********'" +
                ", newEncryption=" + encryptionType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FileEncryptionData that = (FileEncryptionData) o;
        return plainSize == that.plainSize &&
                encryptedSize == that.encryptedSize &&
                uri.equals(that.uri) &&
                encryptedMD5.equals(that.encryptedMD5) &&
                encryptionType == that.encryptionType &&
                encryptionKey.equals(that.encryptionKey);
    }
}
