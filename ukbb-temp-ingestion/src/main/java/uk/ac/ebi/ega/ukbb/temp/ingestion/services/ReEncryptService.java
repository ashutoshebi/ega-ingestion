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

    // TODO bjuhasz: what permissions are needed for this Spring Boot application
    //  to be able to write into the STAGING_PATH directory?
    // TODO bjuhasz: box_staging or staging?
    private static final Path STAGING_PATH = Paths.get("/nfs/ega/public/box_staging");
    private static final String RELATIVE_PATH_INSIDE_STAGING = "ukbb-temp-ingestion/re-encrypted-files";

    private static final int BUFFER_SIZE = 8192;

    private UkBiobankFilesRepository originalFilesRepository;
    private UkBiobankReEncryptedFilesRepository reEncryptedFilesRepository;
    private ProFilerService proFilerService;

    public ReEncryptService(final UkBiobankFilesRepository originalFilesRepository,
                            final UkBiobankReEncryptedFilesRepository reEncryptedFilesRepository,
                            final ProFilerService proFilerService) {
        this.originalFilesRepository = originalFilesRepository;
        this.reEncryptedFilesRepository = reEncryptedFilesRepository;
        this.proFilerService = proFilerService;
    }

    public Optional<Result> getReEncryptionResultFor(final Path originalFilePath) {
        return reEncryptedFilesRepository
                .findByOriginalFilePath(originalFilePath.toString())
                .map(this::reEncryptedFileEntityToResult);
    }

    public Result reEncryptAndStoreInProFiler(final Path inputFilePath,
                                              final String inputPassword,
                                              final String outputPassword) {

        final Path fileName = getFileName(inputFilePath);
        final Path outputFileAbsolutePath = STAGING_PATH.resolve(RELATIVE_PATH_INSIDE_STAGING).resolve(fileName);
        final Path outputFileRelativePathInsideStaging = Paths.get("/")
                .resolve(RELATIVE_PATH_INSIDE_STAGING).resolve(fileName);

        return reEncryptAndStoreInProFiler(inputFilePath, inputPassword,
                outputFileAbsolutePath, outputFileRelativePathInsideStaging, outputPassword);
    }

    /**
     * TODO bjuhasz: document this
     *
     * @param inputFilePath
     * @param inputPassword
     * @param outputFileAbsolutePath
     * @param outputFileRelativePathInsideStaging
     * @param outputPassword
     * @return
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

            final long proFilerId = storeReEncryptedFileInProFiler(reEncryptionResult.outputFile,
                    outputFileRelativePathInsideStaging, newReEncryptedMd5);

            LOGGER.debug("{} was re-encrypted into {}", inputFilePath, outputFileAbsolutePath);
            LOGGER.info("{} was re-encrypted and stored in pro-filer with ID: {}", inputFilePath, proFilerId);

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


        final Optional<UkBiobankReEncryptedFileEntity> optionalEntity = reEncryptedFilesRepository
                .findByOriginalFilePath(inputFilePath.toString());
        final UkBiobankReEncryptedFileEntity entity;

        if (optionalEntity.isPresent()) {
            entity = optionalEntity.get();
            LOGGER.debug("entity is already present in the DB: {}", entity);

            update(entity, outputFilePath, originalEncryptedMd5, unencryptedMd5, newReEncryptedMd5,
                    result, exceptionMessage);
        } else {
            entity = new UkBiobankReEncryptedFileEntity(
                    inputFilePath.toString(), outputFilePath.toString(),
                    originalEncryptedMd5, unencryptedMd5, newReEncryptedMd5,
                    result.getStatus(), result.getMessage(), exceptionMessage,
                    result.getStartTime(), result.getEndTime());
            LOGGER.debug("entity was not present in the DB, creating it: {}", entity);
        }

/*
TODO bjuhasz: if every test is green, then use the code below, instead of the one above:

        final UkBiobankReEncryptedFileEntity entity = reEncryptedFilesRepository
                .findByOriginalFilePath(inputFilePath.toString())
                .map(e -> update(e, outputFilePath, originalEncryptedMd5, unencryptedMd5, newReEncryptedMd5,
                        result, exceptionMessage))
                .orElse(new UkBiobankReEncryptedFileEntity(
                                inputFilePath.toString(), outputFilePath.toString(),
                                originalEncryptedMd5, unencryptedMd5, newReEncryptedMd5,
                                result.getStatus(), result.getMessage(), exceptionMessage,
                                result.getStartTime(), result.getEndTime()));
*/
        reEncryptedFilesRepository.save(entity);
    }

    // TODO bjuhasz: document this function
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
