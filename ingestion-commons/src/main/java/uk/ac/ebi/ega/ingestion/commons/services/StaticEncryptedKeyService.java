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
package uk.ac.ebi.ega.ingestion.commons.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;

public class StaticEncryptedKeyService implements IEncryptedKeyService {

    private final static Logger logger = LoggerFactory.getLogger(StaticEncryptedKeyService.class);

    private IPasswordEncryptionService passwordEncryptionService;

    private char[] key;

    public StaticEncryptedKeyService(IPasswordEncryptionService passwordEncryptionService, char[] key)
            throws AlgorithmInitializationException {
        this.passwordEncryptionService = passwordEncryptionService;
        this.key = key;
        // We execute a test encryption. If it runs properly then we can safely ignore exceptions in future executions.
        passwordEncryptionService.encrypt(key);
    }

    public StaticEncryptedKeyService(IPasswordEncryptionService passwordEncryptionService, Path keyFile)
            throws IOException, AlgorithmInitializationException {
        this(passwordEncryptionService, FileUtils.readPasswordFile(keyFile));
    }

    @Override
    public String generateNewEncryptedKey() {
        try {
            return passwordEncryptionService.encrypt(key);
        } catch (AlgorithmInitializationException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

    @Override
    public char[] decryptKey(String encryptedKey) {
        try {
            return passwordEncryptionService.decrypt(encryptedKey);
        } catch (AlgorithmInitializationException e) {
            logger.error(e.getMessage(), e);
            return null;
        }
    }

}
