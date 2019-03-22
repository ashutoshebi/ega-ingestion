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
import uk.ac.ebi.ega.encryption.core.utils.io.ReportingOutputStream;

import java.io.IOException;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

public class EncryptOutputStream extends OutputStream {

    private final MessageDigest messageDigest;

    private final OutputStream encryptedOutput;

    public EncryptOutputStream(OutputStream output, EncryptionAlgorithm algorithm, char[] password)
            throws AlgorithmInitializationException {
        this.messageDigest = Hash.getMd5();
        this.encryptedOutput = algorithm.encrypt(password,
                new DigestOutputStream(new ReportingOutputStream(output), messageDigest));
    }

    @Override
    public void write(int i) throws IOException {
        encryptedOutput.write(i);
    }

    @Override
    public void close() throws IOException {
        encryptedOutput.close();
    }

    @Override
    public void flush() throws IOException {
        encryptedOutput.flush();
    }

    public String getMd5() {
        return Hash.normalize(messageDigest);
    }

}
