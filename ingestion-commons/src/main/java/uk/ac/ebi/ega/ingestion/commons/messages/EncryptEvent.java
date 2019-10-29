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

public class EncryptEvent {

    private URI uri;

    private Encryption currentEncryption;

    private String decryptionKey;

    private Encryption newEncryption;

    private String encryptionKey;

    private String encryptedMd5;

    private String plainMd5;

    public EncryptEvent() {
    }

    public EncryptEvent(URI uri, Encryption currentEncryption, String decryptionKey, Encryption newEncryption,
                        String encryptionKey, String encryptedMd5, String plainMd5) {
        this.uri = uri;
        this.currentEncryption = currentEncryption;
        this.decryptionKey = decryptionKey;
        this.newEncryption = newEncryption;
        this.encryptionKey = encryptionKey;
        this.encryptedMd5 = encryptedMd5;
        this.plainMd5 = plainMd5;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public Encryption getCurrentEncryption() {
        return currentEncryption;
    }

    public void setCurrentEncryption(Encryption currentEncryption) {
        this.currentEncryption = currentEncryption;
    }

    public String getDecryptionKey() {
        return decryptionKey;
    }

    public void setDecryptionKey(String decryptionKey) {
        this.decryptionKey = decryptionKey;
    }

    public Encryption getNewEncryption() {
        return newEncryption;
    }

    public void setNewEncryption(Encryption newEncryption) {
        this.newEncryption = newEncryption;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String getEncryptedMd5() {
        return encryptedMd5;
    }

    public void setEncryptedMd5(String encryptedMd5) {
        this.encryptedMd5 = encryptedMd5;
    }

    public String getPlainMd5() {
        return plainMd5;
    }

    public void setPlainMd5(String plainMd5) {
        this.plainMd5 = plainMd5;
    }

    public static EncryptEvent ingest(NewFileEvent event, String encryptionKey) {
        return new EncryptEvent(
                event.getPath().toUri(),
                Encryption.PGP,
                null,
                Encryption.EGA_AES,
                encryptionKey,
                event.getEncryptedMd5(),
                event.getPlainMd5());
    }

    @Override
    public String toString() {
        return "EncryptEvent{" +
                "uri=" + uri +
                ", currentEncryption=" + currentEncryption +
                ", decryptionKey='" + decryptionKey + '\'' +
                ", newEncryption=" + newEncryption +
                ", encryptionKey='*****'" +
                ", encryptedMd5='" + encryptedMd5 + '\'' +
                ", plainMd5='" + plainMd5 + '\'' +
                '}';
    }
}
