/*
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
 */
package uk.ac.ebi.ega.ukbb.temp.ingestion.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.stereotype.Service;
import uk.ac.ebi.ega.encryption.core.DecryptInputStream;
import uk.ac.ebi.ega.encryption.core.EncryptOutputStream;
import uk.ac.ebi.ega.encryption.core.EncryptionReport;
import uk.ac.ebi.ega.encryption.core.Md5Check;
import uk.ac.ebi.ega.encryption.core.encryption.AesCbcOpenSSL;
import uk.ac.ebi.ega.encryption.core.encryption.AesCtr256Ega;
import uk.ac.ebi.ega.encryption.core.encryption.EncryptionAlgorithm;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.exceptions.Md5CheckException;
import uk.ac.ebi.ega.encryption.core.utils.Hash;
import uk.ac.ebi.ega.encryption.core.utils.io.IOUtils;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.entity.UkBiobankFileEntity;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankFilesRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class ReEncryptService implements IReEncryptService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReEncryptService.class);

    private static final int BUFFER_SIZE = 8192;

    private UkBiobankFilesRepository ukBiobankFilesRepository;

    public ReEncryptService(final UkBiobankFilesRepository ukBiobankFilesRepository) {
        this.ukBiobankFilesRepository = ukBiobankFilesRepository;
    }

    @Override
    public Result reEncrypt(final Path inputFilePath,
                            final String inputPassword,
                            final Path outputFilePath,
                            final String outputPassword) {

        final LocalDateTime start = LocalDateTime.now();

        final File inputFile = inputFilePath.toFile();
        final File outputFile = outputFilePath.toFile();

        // This is the MessageDigest of the original, encrypted file:
        final MessageDigest messageDigestOfEncryptedFile = Hash.getMd5();

        final AesCbcOpenSSL decryptionAlgorithm = new AesCbcOpenSSL();
        decryptionAlgorithm.setUseMd5Salt(true);

        final EncryptionAlgorithm encryptionAlgorithm = new AesCtr256Ega();

        try (final InputStream encryptedInput = new FileInputStream(inputFile);
             final InputStream messageDigestedEncryptedInput = new DigestInputStream(encryptedInput, messageDigestOfEncryptedFile);
             final InputStream base64DecodedInput = Base64.getMimeDecoder().wrap(messageDigestedEncryptedInput);
             final DecryptInputStream decryptedInput = new DecryptInputStream(base64DecodedInput,
                     decryptionAlgorithm, inputPassword.toCharArray());

             final EncryptOutputStream reEncryptedOutput = new EncryptOutputStream(new FileOutputStream(outputFile),
                     encryptionAlgorithm, outputPassword.toCharArray())) {

            final long decryptedSize = IOUtils.bufferedPipe(decryptedInput, reEncryptedOutput, BUFFER_SIZE);
            reEncryptedOutput.flush();

            final String md5OfEncryptedInput = Hash.normalize(messageDigestOfEncryptedFile);
            final String md5OfDecryptedInput = decryptedInput.getUnencryptedMd5();
            final String md5OfReEncryptedOutput = reEncryptedOutput.getMd5();

            // This is the MD5 of the original, not-yet-encrypted file:
            final String expectedMd5OfOriginalFile = fetchMd5FromDatabaseFor(inputFilePath);

            final Md5Check md5Check = Md5Check.any(expectedMd5OfOriginalFile);
            md5Check.check(md5OfEncryptedInput, md5OfDecryptedInput);

            final EncryptionReport encryptionReport = new EncryptionReport(md5OfEncryptedInput,
                    md5OfDecryptedInput,
                    md5OfReEncryptedOutput,
                    decryptedSize);

        } catch (FileNotFoundException e) {
            return Result.failure("File could not be found on DOS", e, start);
        } catch (AlgorithmInitializationException e) {
            return Result.failure("Error while decrypting the file on DOS", e, start);
        } catch (Md5CheckException e) {
            return Result.failure("Mismatch of md5", e, start);
        } catch (IOException e) {
            return Result.abort("Unrecoverable error", e, start);
        } catch (ObjectRetrievalFailureException e) {
            return Result.failure("MD5 checksum was not found in database", e, start);
        }

        return Result.correct(start);
    }

    private String fetchMd5FromDatabaseFor(final Path inputFilePath) {
        final String inputFilePathAsString = inputFilePath.toString();
        return ukBiobankFilesRepository
                .findByFilePath(inputFilePathAsString)
                .map(UkBiobankFileEntity::getMd5Checksum)
                .orElseThrow(() -> new ObjectRetrievalFailureException(UkBiobankFileEntity.class, inputFilePathAsString));
    }

}
