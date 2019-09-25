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

import uk.ac.ebi.ega.ingestion.commons.messages.IngestionEventSimplify;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class IngestionEventData implements IIngestionEventData {

    private final Path encryptedFilePath;
    private final String currentEncryption;
    private final String plainMD5;
    private final String encryptedMD5;
    private final char[] decryptionKey;
    private final String newEncryption;
    private final char[] encryptionKey;
    private final Path outputFolderPath;

    public IngestionEventData(final IngestionEventSimplify ingestionEventData, final Path outputFolderPath) {
        this.encryptedFilePath = Paths.get(ingestionEventData.getUri());
        this.currentEncryption = ingestionEventData.getCurrentEncryption();
        this.plainMD5 = ingestionEventData.getPlainMD5();
        this.encryptedMD5 = ingestionEventData.getEncryptedMD5();
        this.decryptionKey = ingestionEventData.getDecryptionKey().toCharArray();
        this.newEncryption = ingestionEventData.getNewEncryption();
        this.encryptionKey = ingestionEventData.getEncryptionKey().toCharArray();
        this.outputFolderPath = outputFolderPath;
    }

    @Override
    public File getEncryptedFile() {
        return encryptedFilePath.toFile();
    }

    @Override
    public String getCurrentEncryption() {
        return currentEncryption;
    }

    @Override
    public String getPlainMD5() {
        return plainMD5;
    }

    @Override
    public String getEncryptedMD5() {
        return encryptedMD5;
    }

    @Override
    public char[] getDecryptionKey() {
        return decryptionKey;
    }

    @Override
    public String getNewEncryption() {
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
