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
package uk.ac.ebi.ega.file.encryption.processor.messages;

import java.util.Objects;

public class EncryptComplete {

    private final String fileName;
    private final long plainSize;
    private final String plainMd5;
    private final long encryptedSize;
    private final String encryptedMd5;
    private final String encryptionPassword;

    public EncryptComplete(final String fileName, final long plainSize,
                           final String plainMd5, final long encryptedSize, final String encryptedMd5,
                           final String encryptionPassword) {
        this.fileName = Objects.requireNonNull(fileName);
        this.plainSize = Objects.requireNonNull(plainSize);
        this.plainMd5 = Objects.requireNonNull(plainMd5);
        this.encryptedSize = Objects.requireNonNull(encryptedSize);
        this.encryptedMd5 = Objects.requireNonNull(encryptedMd5);
        this.encryptionPassword = Objects.requireNonNull(encryptionPassword);
    }

    public String getFileName() {
        return fileName;
    }

    public Long getPlainSize() {
        return plainSize;
    }

    public String getPlainMd5() {
        return plainMd5;
    }

    public String getEncryptedMd5() {
        return encryptedMd5;
    }

    public Long getEncryptedSize() {
        return encryptedSize;
    }

    public String getEncryptionPassword() {
        return encryptionPassword;
    }
}
