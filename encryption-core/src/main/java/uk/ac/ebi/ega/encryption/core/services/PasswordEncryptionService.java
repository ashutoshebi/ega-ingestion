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
package uk.ac.ebi.ega.encryption.core.services;

import org.bouncycastle.util.encoders.Base64;
import uk.ac.ebi.ega.encryption.core.encryption.AesCbcOpenSSL;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.utils.io.IOUtils;

import static java.nio.charset.StandardCharsets.UTF_8;

public class PasswordEncryptionService implements IPasswordEncryptionService {

    private char[] passwordKey;

    private AesCbcOpenSSL aesCbcOpenSSL;

    public PasswordEncryptionService(char[] passwordKey) {
        this.passwordKey = passwordKey;
        this.aesCbcOpenSSL = new AesCbcOpenSSL();
    }

    @Override
    public String encrypt(byte[] password) throws AlgorithmInitializationException {
        return Base64.toBase64String(aesCbcOpenSSL.encrypt(passwordKey, password));
    }

    @Override
    public String encrypt(char[] password) throws AlgorithmInitializationException {
        return encrypt(IOUtils.convertToBytes(password));
    }

    @Override
    public char[] decrypt(String encryptedPassword) throws AlgorithmInitializationException {
        return aesCbcOpenSSL.decrypt(passwordKey, Base64.decode(encryptedPassword), UTF_8);
    }

}
