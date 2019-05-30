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
import uk.ac.ebi.ega.fire.IFireService;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.entity.UkBiobankFileEntity;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.entity.UkBiobankReEncryptedFileEntity;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankFilesRepository;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankReEncryptedFilesRepository;

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
import java.util.Optional;

@Service
public class ReEncryptService implements IReEncryptService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReEncryptService.class);

    private static final String FAILURE_MESSAGE = "Failure during the re-encryption";
    private static final int BUFFER_SIZE = 8192;

    private UkBiobankFilesRepository originalFilesRepository;
    private UkBiobankReEncryptedFilesRepository reEncryptedFilesRepository;
    private IFireService fireService;

    public ReEncryptService(final UkBiobankFilesRepository originalFilesRepository,
                            final UkBiobankReEncryptedFilesRepository reEncryptedFilesRepository,
                            final IFireService fireService) {
        this.originalFilesRepository = originalFilesRepository;
        this.reEncryptedFilesRepository = reEncryptedFilesRepository;
        this.fireService = fireService;
    }

    @Override
    public Optional<Result> getReEncryptionResultFor(final Path originalFilePath) {
        return reEncryptedFilesRepository
                .findByOriginalFilePath(originalFilePath.toString())
                .map(this::reEncryptedFileEntityToResult);
    }

    // TODO bjuhasz: modularize this function: split it into smaller pieces
    @Override
    public Result reEncrypt(final Path inputFilePath,
                            final String inputPassword,
                            final Path outputFilePath,
                            final String outputPassword) {

        final LocalDateTime start = LocalDateTime.now();

        final File inputFile = inputFilePath.toFile();
        final File outputFile = outputFilePath.toFile();

        // This is the MessageDigest of the encrypted file:
        final MessageDigest messageDigestOfEncryptedFile = Hash.getMd5();

        final AesCbcOpenSSL decryptionAlgorithm = new AesCbcOpenSSL();
        decryptionAlgorithm.setUseMd5Salt(true);

        final EncryptionAlgorithm encryptionAlgorithm = new AesCtr256Ega();

        EncryptionReport reEncryptionReport = new EncryptionReport(FAILURE_MESSAGE, FAILURE_MESSAGE,
                FAILURE_MESSAGE, -1);
        Result result;

        try (final InputStream encryptedInput = new FileInputStream(inputFile);
             final InputStream messageDigestedEncryptedInput = new DigestInputStream(encryptedInput, messageDigestOfEncryptedFile);
             final InputStream base64DecodedInput = Base64.getMimeDecoder().wrap(messageDigestedEncryptedInput);
             final DecryptInputStream decryptedInput = new DecryptInputStream(base64DecodedInput,
                     decryptionAlgorithm, inputPassword.toCharArray());

             final EncryptOutputStream reEncryptedOutput = new EncryptOutputStream(new FileOutputStream(outputFile),
                     encryptionAlgorithm, outputPassword.toCharArray())) {

            final long unencryptedSize = IOUtils.bufferedPipe(decryptedInput, reEncryptedOutput, BUFFER_SIZE);
            reEncryptedOutput.flush();

            final String originalEncryptedMd5 = Hash.normalize(messageDigestOfEncryptedFile);
            final String unencryptedMd5 = decryptedInput.getUnencryptedMd5();
            final String newReEncryptedMd5 = reEncryptedOutput.getMd5();

            // This is the MD5 of the original, not-yet-encrypted (or unencrypted) file:
            final String expectedUnencryptedMd5 = fetchMd5FromDatabaseFor(inputFilePath);

            final Md5Check md5Check = Md5Check.any(expectedUnencryptedMd5);
            md5Check.check(originalEncryptedMd5, unencryptedMd5);

            reEncryptionReport = new EncryptionReport(originalEncryptedMd5,
                    unencryptedMd5,
                    newReEncryptedMd5,
                    unencryptedSize);

            storeReEncryptedFileInFire(outputFile);

            result = Result.correct(start);

        } catch (FileNotFoundException e) {
            result = Result.failure("File could not be found on DOS", e, start);
        } catch (AlgorithmInitializationException e) {
            result = Result.failure("Error while decrypting the file on DOS", e, start);
        } catch (Md5CheckException e) {
            result = Result.failure("Mismatch of md5", e, start);
        } catch (ObjectRetrievalFailureException e) {
            result = Result.failure("MD5 checksum of the original, unencrypted file was not found in database", e, start);
        } catch (IOException e) {
            result = Result.abort("Unrecoverable error", e, start);
        }

        saveReEncryptionOutcomeIntoDatabase(reEncryptionReport, result, inputFilePath, outputFilePath);

        return result;
    }

    private Result reEncryptedFileEntityToResult(final UkBiobankReEncryptedFileEntity reEncryptedFileEntity) {
        return new Result(reEncryptedFileEntity.getResultStatus(),
                reEncryptedFileEntity.getResultStatusMessage(),
                new RuntimeException(reEncryptedFileEntity.getResultStatusException()),
                reEncryptedFileEntity.getStartTime());
    }

    // TODO bjuhasz: throw the necessary exceptions
    private void storeReEncryptedFileInFire(final File outputFile) {
        // TODO bjuhasz: implement this function (in the next ticket (EE-749))
    }

    private String fetchMd5FromDatabaseFor(final Path inputFilePath) {
        final String inputFilePathAsString = inputFilePath.toString();

        final String md5 = originalFilesRepository
                .findByFilePath(inputFilePathAsString)
                .map(UkBiobankFileEntity::getMd5Checksum)
                .orElseThrow(() -> new ObjectRetrievalFailureException(UkBiobankFileEntity.class, inputFilePathAsString));

        LOGGER.debug("The MD5 checksum of the original, unencrypted file ({}) is: {}",
                inputFilePath, md5);

        return md5;
    }

    private void saveReEncryptionOutcomeIntoDatabase(final EncryptionReport reEncryptionReport,
                                                     final Result result,
                                                     final Path inputFilePath,
                                                     final Path outputFilePath) {

        final String exceptionMessage = result.getException() != null ? result.getException().getMessage() : "";

        final UkBiobankReEncryptedFileEntity entity = new UkBiobankReEncryptedFileEntity(
                inputFilePath.toString(),
                outputFilePath.toString(),
                reEncryptionReport.getUnencryptedMd5(),
                reEncryptionReport.getOriginalMd5(),
                reEncryptionReport.getEncryptedMd5(),
                result.getStatus(),
                result.getMessage(),
                exceptionMessage,
                result.getStartTime(),
                result.getEndTime());

        reEncryptedFilesRepository.save(entity);
    }

}
