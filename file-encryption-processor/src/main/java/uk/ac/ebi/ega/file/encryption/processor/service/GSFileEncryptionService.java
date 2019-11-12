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
package uk.ac.ebi.ega.file.encryption.processor.service;

import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.StringUtils;
import uk.ac.ebi.ega.encryption.core.EncryptOutputStream;
import uk.ac.ebi.ega.encryption.core.encryption.AesCtr256Ega;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;
import uk.ac.ebi.ega.encryption.core.utils.Hash;
import uk.ac.ebi.ega.file.encryption.processor.exception.GoogleStorageException;
import uk.ac.ebi.ega.file.encryption.processor.exception.Md5Mismatch;
import uk.ac.ebi.ega.ingestion.commons.messages.EncryptEvent;
import uk.ac.ebi.ega.ingestion.commons.messages.FileEncryptionData;
import uk.ac.ebi.ega.ingestion.commons.messages.FileEncryptionResult;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.security.DigestOutputStream;
import java.util.UUID;

public class GSFileEncryptionService implements IFileEncryptionService {

    private final Logger LOGGER = LoggerFactory.getLogger(GSFileEncryptionService.class);

    private final IPasswordEncryptionService encryptedKeyService;
    private final Path outputFolderPath;

    public GSFileEncryptionService(final IPasswordEncryptionService encryptedKeyService, final Path outputFolderPath) {
        this.encryptedKeyService = encryptedKeyService;
        this.outputFolderPath = outputFolderPath;
    }

    @Retryable(maxAttemptsExpression = "#{${request.google.storage.attempts.max}}",
            backoff = @Backoff(delayExpression = "#{${request.google.storage.attempts.delay}}",
                    multiplierExpression = "#{${request.google.storage.attempts.multiplier}}"),
            value = {GoogleStorageException.class}
    )
    @Override
    public FileEncryptionResult encrypt(final EncryptEvent data) {
        LOGGER.info("Process has been started for file {}", data.getUri());
        try {
            return doEncrypt(data);
        } catch (IOException | AlgorithmInitializationException | IllegalArgumentException e) {
            LOGGER.error("Process for file {} failed unexpectedly, reason {}.", data.getUri(), e.getMessage());
            return FileEncryptionResult.failure(e.getMessage(), e);
        } catch (Md5Mismatch md5Mismatch) {
            LOGGER.info("Process for file {} has been finished but an md5 mismatch has been found", data.getUri());
            return FileEncryptionResult.md5Failure(md5Mismatch.getMessage(), md5Mismatch);
        } catch (GoogleStorageException gse) {
            LOGGER.error("Error while downloading file. Retrying request", gse);
            throw gse;
        }
    }

    @Recover
    protected FileEncryptionResult recoverFromFixedRetry(final GoogleStorageException gce, final EncryptEvent data) {
        LOGGER.info("Recovering from fixed retries.\n Process has been failed for file {}, reason {}.", data.getUri(), gce.getMessage());
        return FileEncryptionResult.failure(gce.getMessage(), gce);
    }

    private FileEncryptionResult doEncrypt(final EncryptEvent data) throws IOException, AlgorithmInitializationException, Md5Mismatch {
        final File outputFile = outputFolderPath.resolve(UUID.randomUUID().toString() + ".cip").toFile();
        final char[] decryptedGSCredentials = encryptedKeyService.decrypt(data.getCredentials());

        try (final EncryptOutputStream encrypted = new EncryptOutputStream(
                new FileOutputStream(outputFile),
                new AesCtr256Ega(),
                encryptedKeyService.decrypt(data.getEncryptionKey()));
             final DigestOutputStream downloadStream = new DigestOutputStream(encrypted, Hash.getMd5())
        ) {
            final Credentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(new String(decryptedGSCredentials).getBytes()));
            final Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
            Blob blob;
            try {
                final URI normalizedURI = data.getUri().normalize();
                assertGoogleStorageURI(normalizedURI);
                //Unable to get Blob using URI. Need to get bucket & file path.
                blob = storage.get(BlobId.of(data.getUri().getHost(), data.getUri().getPath().substring(1)));
                blob.downloadTo(downloadStream);
                downloadStream.flush();
            } catch (Exception e) {
                //Catch all exceptions to retry request. Expected to have a network related issues.
                throw new GoogleStorageException(e.getMessage(), e);
            }
            final String plainMd5 = Hash.normalize(downloadStream.getMessageDigest());
            final String encryptedMd5 = encrypted.getMd5();

            assertChecksum("Md5 mismatch", data.getPlainMd5(), plainMd5);

            LOGGER.info("Process for file {} has been finished successfully", data.getUri());
            return FileEncryptionResult.success(new FileEncryptionData(
                    blob.getSize(),
                    outputFile.toURI(),
                    encryptedMd5,
                    outputFile.length(),
                    data.getEncryptionKey(),
                    data.getNewEncryption()));
        }
    }

    private void assertChecksum(final String message, final String expected, final String actual) throws Md5Mismatch {
        if (expected == null || !expected.equalsIgnoreCase(actual)) {
            LOGGER.info("{} expected:{} actual:{}", message, expected, actual);
            throw new Md5Mismatch(message, expected, actual);
        }
    }

    private void assertGoogleStorageURI(final URI uri) {
        if (!uri.toString().startsWith("gs://") || StringUtils.isEmpty(uri.getHost()) || StringUtils.isEmpty(uri.getPath())) {
            throw new IllegalArgumentException("Invalid URI " + uri.toString());
        }
    }
}
