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

import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.stereotype.Service;
import uk.ac.ebi.ega.encryption.core.DecryptInputStream;
import uk.ac.ebi.ega.encryption.core.EncryptOutputStream;
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
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.entity.UkBiobankReEncryptedFileEntity;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankFilesRepository;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankReEncryptedFilesRepository;
import uk.ac.ebi.ega.ukbb.temp.ingestion.properties.ReEncryptProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

@Service
public class ReEncryptService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReEncryptService.class);

    private static final int BUFFER_SIZE = 8192;

    private UkBiobankFilesRepository originalFilesRepository;
    private UkBiobankReEncryptedFilesRepository reEncryptedFilesRepository;
    private ProFilerService proFilerService;
    private ReEncryptProperties reEncryptProperties;

    @Autowired
    public ReEncryptService(final UkBiobankFilesRepository originalFilesRepository,
                            final UkBiobankReEncryptedFilesRepository reEncryptedFilesRepository,
                            final ProFilerService proFilerService,
                            final ReEncryptProperties reEncryptProperties) {
        this.originalFilesRepository = originalFilesRepository;
        this.reEncryptedFilesRepository = reEncryptedFilesRepository;
        this.proFilerService = proFilerService;
        this.reEncryptProperties = reEncryptProperties;
    }

    public Optional<Result> getReEncryptionResultFor(final Path originalFilePath) {
        return reEncryptedFilesRepository
                .findByOriginalFilePath(originalFilePath.toString())
                .map(this::reEncryptedFileEntityToResult);
    }

    public Result reEncryptAndStoreInProFiler(final Path inputFilePath,
                                              final String inputPassword,
                                              final String outputPassword) {

        final Path stagingPath = Paths.get(reEncryptProperties.getStagingPath());
        final String relativePathInsideStaging = reEncryptProperties.getRelativePathInsideStaging();

        final Path fileName = getFileName(inputFilePath);
        final Path outputFileAbsolutePath = stagingPath.resolve(relativePathInsideStaging).resolve(fileName);
        final Path outputFileRelativePathInsideStaging = Paths.get("/")
                .resolve(relativePathInsideStaging).resolve(fileName);

        return reEncryptAndStoreInProFiler(inputFilePath, inputPassword,
                outputFileAbsolutePath, outputFileRelativePathInsideStaging, outputPassword);
    }

    /**
     * The input file is first decrypted using the AES-256-CBC algorithm and with the given inputPassword,
     * then it's re-encrypted with the AES-256-CTR algorithm and the given outputPassword.
     * Finally, the re-encrypted file is stored in the Fire Data Object Store.
     *
     * @param inputFilePath an already encrypted file which has to be re-encrypted
     * @param inputPassword the password for the already encrypted input file
     * @param outputFileAbsolutePath the absolute path of the resulting re-encrypted file
     * @param outputFileRelativePathInsideStaging a trailing part of the outputFileAbsolutePath,
     *                                            which doesn't contain the path-of-staging
     * @param outputPassword the password to be used during the re-encryption
     * @return an object holding information about the re-encryption process
     */
    Result reEncryptAndStoreInProFiler(final Path inputFilePath,
                                       final String inputPassword,
                                       final Path outputFileAbsolutePath,
                                       final Path outputFileRelativePathInsideStaging,
                                       final String outputPassword) {

        final LocalDateTime start = LocalDateTime.now();
        String originalEncryptedMd5 = "";
        String unencryptedMd5 = "";
        String newReEncryptedMd5 = "";
        Result finalResult;

        try {
            final ReEncryptionResult reEncryptionResult = reEncrypt(inputFilePath, inputPassword,
                    outputFileAbsolutePath, outputPassword);

            originalEncryptedMd5 = reEncryptionResult.originalEncryptedMd5;
            unencryptedMd5 = reEncryptionResult.unencryptedMd5;
            newReEncryptedMd5 = reEncryptionResult.newReEncryptedMd5;

            // This is the MD5 of the original, not-yet-encrypted (or unencrypted) file:
            final String expectedUnencryptedMd5 = fetchMd5FromDatabaseFor(inputFilePath);

            checkMd5(expectedUnencryptedMd5, originalEncryptedMd5, unencryptedMd5);

            LOGGER.debug("{} was re-encrypted into {}", inputFilePath, outputFileAbsolutePath);

            if (reEncryptProperties.shouldStoreFileInFire()) {
                final long proFilerId = storeReEncryptedFileInProFiler(reEncryptionResult.outputFile,
                        outputFileRelativePathInsideStaging, newReEncryptedMd5);
                LOGGER.info("{} was re-encrypted and stored in pro-filer with ID: {}", inputFilePath, proFilerId);
            } else {
                LOGGER.info("{} was re-encrypted but it was NOT stored in pro-filer " +
                        "because the \"store-file-in-fire\" property is set to false.", inputFilePath);
            }

            finalResult = Result.correct(start);

        } catch (FileNotFoundException e) {
            finalResult = Result.failure("File could not be found on DOS", e, start);
        } catch (AlgorithmInitializationException e) {
            finalResult = Result.failure("Error while decrypting the file on DOS", e, start);
        } catch (IOException e) {
            finalResult = Result.abort("Unrecoverable error", e, start);
        } catch (Md5CheckException e) {
            finalResult = Result.failure("Mismatch of md5", e, start);
        } catch (ObjectRetrievalFailureException e) {
            finalResult = Result.failure("MD5 checksum of the original, unencrypted file was not found in database", e, start);
        } catch (Exception e) {
            finalResult = Result.abort("Generic error", e, start);
        }

        try {
            upsertReEncryptionResultInDatabase(inputFilePath, outputFileAbsolutePath,
                    originalEncryptedMd5, unencryptedMd5, newReEncryptedMd5,
                    finalResult);
        } catch (DataAccessException e) {
            finalResult = Result.failure("Error while saving the result to the DB", e, start);
        }

        return finalResult;
    }

    private ReEncryptionResult reEncrypt(final Path inputFilePath,
                                         final String inputPassword,
                                         final Path outputFilePath,
                                         final String outputPassword) throws IOException, AlgorithmInitializationException {

        final File inputFile = inputFilePath.toFile();
        final File outputFile = createDirectoriesInPath(outputFilePath);

        // This is the MessageDigest of the encrypted file:
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

            final long unencryptedSize = IOUtils.bufferedPipe(decryptedInput, reEncryptedOutput, BUFFER_SIZE);
            reEncryptedOutput.flush();

            final String originalEncryptedMd5 = Hash.normalize(messageDigestOfEncryptedFile);
            final String unencryptedMd5 = decryptedInput.getUnencryptedMd5();
            final String newReEncryptedMd5 = reEncryptedOutput.getMd5();

            return new ReEncryptionResult(originalEncryptedMd5, unencryptedMd5, newReEncryptedMd5, outputFile);
        }
    }

    private void checkMd5(final String expectedUnencryptedMd5,
                          final String originalEncryptedMd5,
                          final String unencryptedMd5) throws Md5CheckException {
        final Md5Check md5Check = Md5Check.any(expectedUnencryptedMd5);
        md5Check.check(originalEncryptedMd5, unencryptedMd5);
    }

    private Result reEncryptedFileEntityToResult(final UkBiobankReEncryptedFileEntity reEncryptedFileEntity) {
        final Exception exception = Strings.isBlank(reEncryptedFileEntity.getResultStatusException()) ?
                null : new Exception(reEncryptedFileEntity.getResultStatusException());

        return new Result(reEncryptedFileEntity.getResultStatus(),
                reEncryptedFileEntity.getResultStatusMessage(),
                exception,
                reEncryptedFileEntity.getStartTime());
    }

    private long storeReEncryptedFileInProFiler(final File file, final Path relativePath, final String md5) {
        final long fileId = proFilerService.insertFile(null, file, md5);
        final long proFilerId = proFilerService.insertArchive(fileId, relativePath.toString(), file, md5);

        LOGGER.info("File {} has been inserted into pro-filer. fileId: {}, proFilerId: {}",
                relativePath, fileId, proFilerId);

        return proFilerId;
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

    private void upsertReEncryptionResultInDatabase(final Path inputFilePath, final Path outputFilePath,
                                                    final String originalEncryptedMd5,
                                                    final String unencryptedMd5, final String newReEncryptedMd5,
                                                    final Result result) {
        final String exceptionMessage = result.getException() != null ? result.getException().getMessage() : "";

        final UkBiobankReEncryptedFileEntity entity = reEncryptedFilesRepository
                .findByOriginalFilePath(inputFilePath.toString())
                .map(alreadyExistingEntity -> update(alreadyExistingEntity,
                        outputFilePath, originalEncryptedMd5, unencryptedMd5, newReEncryptedMd5,
                        result, exceptionMessage))
                .orElse(new UkBiobankReEncryptedFileEntity(
                                inputFilePath.toString(), outputFilePath.toString(),
                                originalEncryptedMd5, unencryptedMd5, newReEncryptedMd5,
                                result.getStatus(), result.getMessage(), exceptionMessage,
                                result.getStartTime(), result.getEndTime()));

        reEncryptedFilesRepository.save(entity);
    }

    // Updates the fields of the given UkBiobankReEncryptedFileEntity with the given values.
    private UkBiobankReEncryptedFileEntity update(final UkBiobankReEncryptedFileEntity entity,
                                                  final Path outputFilePath,
                                                  final String originalEncryptedMd5,
                                                  final String unencryptedMd5,
                                                  final String newReEncryptedMd5,
                                                  final Result result,
                                                  final String exceptionMessage) {
        entity.setNewReEncryptedFilePath(outputFilePath.toString());
        entity.setOriginalEncryptedMd5(originalEncryptedMd5);
        entity.setUnencryptedMd5(unencryptedMd5);
        entity.setNewReEncryptedMd5(newReEncryptedMd5);
        entity.setResultStatus(result.getStatus());
        entity.setResultStatusMessage(result.getMessage());
        entity.setResultStatusException(exceptionMessage);
        entity.setStartTime(result.getStartTime());
        entity.setEndTime(result.getEndTime());
        entity.setAlreadyExistInDb(true);
        return entity;
    }

    private Path getFileName(final Path inputFilePath) {
        final Path fileName = inputFilePath.getFileName();
        final String message = String.format("%s should contain a filename", inputFilePath);
        return Objects.requireNonNull(fileName, message);
    }

    private File createDirectoriesInPath(final Path path) throws IOException {
        Files.createDirectories(path.getParent());
        File outputFile;
        try {
            outputFile = Files.createFile(path).toFile();
        } catch (FileAlreadyExistsException e) {
            outputFile = path.toFile();
            LOGGER.warn("File {} already exists, process will overwrite the file", outputFile);
        }
        return outputFile;
    }

    private class ReEncryptionResult {
        String originalEncryptedMd5;
        String unencryptedMd5;
        String newReEncryptedMd5;
        File outputFile;

        ReEncryptionResult(final String originalEncryptedMd5, final String unencryptedMd5,
                           final String newReEncryptedMd5, final File outputFile) {
            this.originalEncryptedMd5 = originalEncryptedMd5;
            this.unencryptedMd5 = unencryptedMd5;
            this.newReEncryptedMd5 = newReEncryptedMd5;
            this.outputFile = outputFile;
        }
    }
}
