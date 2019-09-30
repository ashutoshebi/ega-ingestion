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

import java.net.URI;

public class IngestionEventSimplify {

    //Temporary class. Will be replaced with IngestionEvent.

    private URI uri;
    private String currentEncryption;
    private String plainMD5;
    private String encryptedMD5;
    private String decryptionKey;
    private String newEncryption;
    private String encryptionKey;

    public IngestionEventSimplify() {
    }

    public IngestionEventSimplify(final URI uri, final String currentEncryption, final String plainMD5,
                                  final String encryptedMD5, final String decryptionKey, final String newEncryption,
                                  final String encryptionKey) {
        this.uri = uri;
        this.currentEncryption = currentEncryption;
        this.plainMD5 = plainMD5;
        this.encryptedMD5 = encryptedMD5;
        this.decryptionKey = decryptionKey;
        this.newEncryption = newEncryption;
        this.encryptionKey = encryptionKey;
    }

    public URI getUri() {
        return uri;
    }

    public String getCurrentEncryption() {
        return currentEncryption;
    }

    public String getPlainMD5() {
        return plainMD5;
    }

    public String getEncryptedMD5() {
        return encryptedMD5;
    }

    public String getDecryptionKey() {
        return decryptionKey;
    }

    public String getNewEncryption() {
        return newEncryption;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }
}
