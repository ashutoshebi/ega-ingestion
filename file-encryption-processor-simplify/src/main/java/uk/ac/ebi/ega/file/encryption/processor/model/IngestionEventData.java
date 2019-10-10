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
package uk.ac.ebi.ega.file.encryption.processor.model;

import uk.ac.ebi.ega.ingestion.commons.messages.EncryptEvent;
import uk.ac.ebi.ega.ingestion.commons.models.Encryption;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class IngestionEventData implements IIngestionEventData {

    private final Path encryptedFilePath;
    private final Encryption currentEncryption;
    private final String plainMd5;
    private final String encryptedMd5;
    private final char[] decryptionKey;
    private final Encryption newEncryption;
    private final char[] encryptionKey;
    private final Path outputFolderPath;

    public IngestionEventData(final EncryptEvent encryptEvent, final Path outputFolderPath) {
        this.encryptedFilePath = Paths.get(encryptEvent.getUri());
        this.currentEncryption = encryptEvent.getCurrentEncryption();
        this.plainMd5 = encryptEvent.getPlainMd5();
        this.encryptedMd5 = encryptEvent.getEncryptedMd5();
        this.decryptionKey = encryptEvent.getDecryptionKey().toCharArray();
        this.newEncryption = encryptEvent.getNewEncryption();
        this.encryptionKey = encryptEvent.getEncryptionKey().toCharArray();
        this.outputFolderPath = outputFolderPath;
    }

    @Override
    public File getEncryptedFile() {
        return encryptedFilePath.toFile();
    }

    @Override
    public Encryption getCurrentEncryption() {
        return currentEncryption;
    }

    @Override
    public String getPlainMd5() {
        return plainMd5;
    }

    @Override
    public String getEncryptedMd5() {
        return encryptedMd5;
    }

    @Override
    public char[] getDecryptionKey() {
        return decryptionKey;
    }

    @Override
    public Encryption getNewEncryption() {
        return newEncryption;
    }

    @Override
    public char[] getEncryptionKey() {
        return encryptionKey;
    }

    @Override
    public File getOutputFile() {
        return outputFolderPath.resolve(UUID.randomUUID().toString()).toFile();
    }
}
