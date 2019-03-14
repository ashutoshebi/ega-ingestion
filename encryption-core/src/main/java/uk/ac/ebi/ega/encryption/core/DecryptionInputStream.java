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
package uk.ac.ebi.ega.encryption.core;

import uk.ac.ebi.ega.encryption.core.encryption.EncryptionAlgorithm;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.utils.Hash;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class DecryptionInputStream extends InputStream {

    private final MessageDigest messageDigest;

    private final DigestInputStream digestedDecryptedInput;

    public DecryptionInputStream(InputStream input, EncryptionAlgorithm algorithm, char[] password)
            throws AlgorithmInitializationException {
        this.messageDigest = Hash.getMd5();
        this.digestedDecryptedInput = new DigestInputStream(algorithm.decrypt(input, password), messageDigest);
    }

    @Override
    public int read() throws IOException {
        return digestedDecryptedInput.read();
    }

    @Override
    public void close() throws IOException {
        digestedDecryptedInput.close();
    }

    public String getUnencryptedMd5() {
        return Hash.normalize(messageDigest);
    }

}
