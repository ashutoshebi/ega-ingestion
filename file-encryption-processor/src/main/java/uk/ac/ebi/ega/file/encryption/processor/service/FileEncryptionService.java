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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;
import uk.ac.ebi.ega.file.encryption.processor.exception.Md5Mismatch;
import uk.ac.ebi.ega.file.encryption.processor.pipeline.DefaultEncryptionPipeline;
import uk.ac.ebi.ega.file.encryption.processor.pipeline.IngestionPipelineResult;
import uk.ac.ebi.ega.ingestion.commons.messages.EncryptEvent;
import uk.ac.ebi.ega.ingestion.commons.messages.FileEncryptionData;
import uk.ac.ebi.ega.ingestion.commons.messages.FileEncryptionResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

public class FileEncryptionService implements IFileEncryptionService {

    private final Logger logger = LoggerFactory.getLogger(FileEncryptionService.class);

    private final IPasswordEncryptionService encryptedKeyService;

    private final File secretRing;

    private final File secretRingPassphrase;

    private final Path outputFolderPath;

    public FileEncryptionService(final IPasswordEncryptionService encryptedKeyService, final File secretRing,
                                 final File secretRingPassphrase, final Path outputFolderPath) {
        this.encryptedKeyService = encryptedKeyService;
        this.secretRing = secretRing;
        this.secretRingPassphrase = secretRingPassphrase;
        this.outputFolderPath = outputFolderPath;
    }

    @Override
    public FileEncryptionResult encrypt(final EncryptEvent data) {
        logger.info("Starting process for file {}", data.getUri());
        try {
            return doEncrypt(data);
        } catch (IOException | AlgorithmInitializationException e) {
            logger.info("Process for file {} failed unexpectedly, reason {}.", data.getUri(), e.getMessage());
            return FileEncryptionResult.failure(e.getMessage(), e);
        } catch (Md5Mismatch md5Mismatch) {
            logger.info("Process for file {} has been finished but an md5 mismatch has been found", data.getUri());
            return FileEncryptionResult.md5Failure(md5Mismatch.getMessage(), md5Mismatch);
        }
    }

    private FileEncryptionResult doEncrypt(final EncryptEvent data)
            throws AlgorithmInitializationException, IOException, Md5Mismatch {
        File outputFile = outputFolderPath.resolve(UUID.randomUUID().toString() + ".cip").toFile();

        final IngestionPipelineResult result;
        result = new DefaultEncryptionPipeline(new File(data.getUri()), secretRing, secretRingPassphrase,
                outputFile, encryptedKeyService.decrypt(data.getEncryptionKey())).process();

        assertChecksum(data, result);

        logger.info("Process for file {} has been finished successfully", data.getUri());
        return FileEncryptionResult.success(new FileEncryptionData(
                result.getPlainSize(),
                result.getEncryptedFile().getFile().toURI(),
                result.getEncryptedFile().getMd5(),
                result.getEncryptedFile().getFileSize(),
                data.getEncryptionKey(),
                data.getNewEncryption()));
    }

    private void assertChecksum(final EncryptEvent data, IngestionPipelineResult result) throws Md5Mismatch {
        String userEncryptedMd5 = data.getEncryptedMd5();
        String calculatedEncryptedMd5 = result.getOriginalFile().getMd5();
        assertChecksum("Encrypted file md5 mismatch", userEncryptedMd5, calculatedEncryptedMd5);
        String userPlainMd5 = data.getPlainMd5();
        String calculatedPlainMd5 = result.getPlainMd5();
        assertChecksum("Decrypted file md5 mismatch", userPlainMd5, calculatedPlainMd5);
        logger.info("File {} gpgMd5:{} plainMd5:{} cipMd5:{}", data.getUri(), userEncryptedMd5, userPlainMd5,
                result.getEncryptedFile().getMd5());
    }

    private void assertChecksum(String text, String expected, String actual)
            throws Md5Mismatch {
        if (expected == null || !expected.equalsIgnoreCase(actual)) {
            logger.info("{} expected:{} actual:{}", text, expected, actual);
            throw new Md5Mismatch(text, expected, actual);
        }
    }
}
