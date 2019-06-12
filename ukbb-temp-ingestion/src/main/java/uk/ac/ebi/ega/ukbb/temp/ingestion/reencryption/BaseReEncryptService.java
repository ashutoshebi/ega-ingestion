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
package uk.ac.ebi.ega.ukbb.temp.ingestion.reencryption;

import org.apache.commons.codec.binary.Base64InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.encryption.core.DecryptInputStream;
import uk.ac.ebi.ega.encryption.core.EncryptOutputStream;
import uk.ac.ebi.ega.encryption.core.encryption.AesCbcOpenSSL;
import uk.ac.ebi.ega.encryption.core.encryption.AesCtr256Ega;
import uk.ac.ebi.ega.encryption.core.encryption.EncryptionAlgorithm;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.utils.Hash;
import uk.ac.ebi.ega.encryption.core.utils.io.IOUtils;
import uk.ac.ebi.ega.ukbb.temp.ingestion.services.IReEncryptService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Base64;

public class BaseReEncryptService implements IReEncryptService {

    private static final int BUFFER_SIZE = 8192;

    private static final Logger logger = LoggerFactory.getLogger(BaseReEncryptService.class);

    @Override
    public ReEncryptionResult reEncrypt(char[] inputPassword, char[] outputPassword, File inputFile, Path outputFilePath)
            throws IOException, AlgorithmInitializationException {
        final File outputFile = createDirectoriesInPath(outputFilePath);

        // This is the MessageDigest of the encrypted file:
        final MessageDigest messageDigestOfEncryptedFile = Hash.getMd5();

        final AesCbcOpenSSL decryptionAlgorithm = new AesCbcOpenSSL();
        decryptionAlgorithm.setUseMd5Salt(true);

        final EncryptionAlgorithm encryptionAlgorithm = new AesCtr256Ega();

        try (final InputStream encryptedInput = new FileInputStream(inputFile);
             final InputStream messageDigestedEncryptedInput = new DigestInputStream(encryptedInput, messageDigestOfEncryptedFile);
             final InputStream base64DecodedInput = new Base64InputStream(messageDigestedEncryptedInput);
             final DecryptInputStream decryptedInput = new DecryptInputStream(base64DecodedInput,
                     decryptionAlgorithm, inputPassword);

             final EncryptOutputStream reEncryptedOutput = new EncryptOutputStream(new FileOutputStream(outputFile),
                     encryptionAlgorithm, outputPassword)) {

            final long unencryptedSize = IOUtils.bufferedPipe(decryptedInput, reEncryptedOutput, BUFFER_SIZE);
            reEncryptedOutput.flush();
            logger.info("The size of the unencrypted file is: {}", unencryptedSize);

            final String originalEncryptedMd5 = Hash.normalize(messageDigestOfEncryptedFile);
            final String unencryptedMd5 = decryptedInput.getUnencryptedMd5();
            final String newReEncryptedMd5 = reEncryptedOutput.getMd5();

            return new ReEncryptionResult(originalEncryptedMd5, unencryptedMd5, newReEncryptedMd5,
                    outputFile, unencryptedSize);
        }

    }

    private File createDirectoriesInPath(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        File outputFile;
        try {
            outputFile = Files.createFile(path).toFile();
        } catch (FileAlreadyExistsException e) {
            outputFile = path.toFile();
            logger.warn("File {} already exists, process will overwrite the file", outputFile);
        }
        return outputFile;
    }

}
