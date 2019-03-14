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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.encryption.core.encryption.EncryptionAlgorithm;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.exceptions.Md5CheckException;
import uk.ac.ebi.ega.encryption.core.utils.Hash;
import uk.ac.ebi.ega.encryption.core.utils.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class BaseEncryptionService implements EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(Encryption.class);

    public static final int BUFFER_SIZE = 8192;

    @Override
    public String encrypt(InputStream input, Output output) throws AlgorithmInitializationException, IOException {
        try (
                EncryptionOutputStream encryptedOutput = new EncryptionOutputStream(output.getOutputStream(),
                        output.getEncryptionAlgorithm(), output.getPassword());
        ) {
            IOUtils.bufferedPipe(input, encryptedOutput, BUFFER_SIZE);
            encryptedOutput.flush();
            return encryptedOutput.getEncryptedMd5();
        }
    }

    @Override
    public EncryptionReport encrypt(Input input, Md5Check md5Check, Output output) throws IOException,
            Md5CheckException, AlgorithmInitializationException {
        MessageDigest messageDigestEncrypted = Hash.getMd5();
        EncryptionAlgorithm decryptionAlgorithm = input.getEncryptionAlgorithm();
        EncryptionAlgorithm encryptionAlgorithm = output.getEncryptionAlgorithm();

        try (
                InputStream digestInput = new DigestInputStream(input.getInputStream(), messageDigestEncrypted);
                DecryptionInputStream decryptedInput = new DecryptionInputStream(digestInput, decryptionAlgorithm,
                        input.getPassword());
                EncryptionOutputStream encryptedOutput = new EncryptionOutputStream(output.getOutputStream(),
                        encryptionAlgorithm, output.getPassword());
        ) {
            long unencryptedSize = IOUtils.bufferedPipe(decryptedInput, encryptedOutput, BUFFER_SIZE);
            encryptedOutput.flush();

            String originalMd5 = Hash.normalize(messageDigestEncrypted);
            String unencryptedMd5 = decryptedInput.getUnencryptedMd5();
            md5Check.check(originalMd5, unencryptedMd5);
            return new EncryptionReport(originalMd5, unencryptedMd5, encryptedOutput.getEncryptedMd5(), unencryptedSize);
        } catch (Exception e) {
            output.onFailure();
            throw e;
        }
    }

}
