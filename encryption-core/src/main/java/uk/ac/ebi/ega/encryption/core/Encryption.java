/*
 *
 * Copyright 2018 EMBL - European Bioinformatics Institute
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
import uk.ac.ebi.ega.encryption.core.utils.Hash;
import uk.ac.ebi.ega.encryption.core.utils.io.IOUtils;
import uk.ac.ebi.ega.encryption.core.utils.io.ReportingOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

public class Encryption {

    private static final Logger logger = LoggerFactory.getLogger(Encryption.class);

    public static final int BUFFER_SIZE = 8192;

    public static EncryptionReport encrypt(InputStream input, char[] passwordInput, EncryptionAlgorithm decryption,
                                           OutputStream output, char[] passwordOutput, EncryptionAlgorithm encryption)
            throws IOException, AlgorithmInitializationException {
        MessageDigest messageDigestEncrypted = Hash.getMd5();
        MessageDigest messageDigest = Hash.getMd5();
        MessageDigest messageDigestReEncrypted = Hash.getMd5();

        long unencryptedSize;
        try (
                InputStream digestedInputStream = new DigestInputStream(input, messageDigestEncrypted);
                InputStream decryptedStream = decryption.decrypt(digestedInputStream, passwordInput);
                InputStream digestedDecryptedStream = new DigestInputStream(decryptedStream, messageDigest);
                OutputStream digestedOutputStream = new DigestOutputStream(output, messageDigestReEncrypted);
                OutputStream cypherOutputStream = encryption.encrypt(passwordOutput, digestedOutputStream)
        ) {
            unencryptedSize = IOUtils.bufferedPipe(digestedDecryptedStream, cypherOutputStream, BUFFER_SIZE);
        }
        String encryptedMd5 = Hash.normalize(messageDigestEncrypted);
        String unencryptedMd5 = Hash.normalize(messageDigest);
        String reEncryptedMd5 = Hash.normalize(messageDigestReEncrypted);
        logger.info("EncryptedMd5 {}, Unencrypted Md5 {}, Re encrypted Md5 {}, unencrypted file size {} bytes",
                encryptedMd5, unencryptedMd5, reEncryptedMd5, unencryptedSize);
        return new EncryptionReport(encryptedMd5, unencryptedMd5, reEncryptedMd5, unencryptedSize);
    }

    public static EncryptionReport loggedEncrypt(InputStream input, char[] passwordInput,
                                                 EncryptionAlgorithm decryption, OutputStream output,
                                                 char[] passwordOutput, EncryptionAlgorithm encryption)
            throws IOException, AlgorithmInitializationException {
        try (
                OutputStream reportingOutputStream = new ReportingOutputStream(output)
        ) {
            return encrypt(input, passwordInput, decryption, reportingOutputStream, passwordOutput, encryption);
        }
    }

}
