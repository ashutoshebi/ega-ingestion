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
import uk.ac.ebi.ega.file.encryption.processor.exception.Md5Mismatch;
import uk.ac.ebi.ega.file.encryption.processor.model.IIngestionEventData;
import uk.ac.ebi.ega.file.encryption.processor.model.Result;
import uk.ac.ebi.ega.file.encryption.processor.pipeline.DefaultIngestionPipeline;
import uk.ac.ebi.ega.file.encryption.processor.pipeline.IngestionPipelineResult;

import java.io.File;

public class FileEncryptionProcessor implements IFileEncryptionProcessor<IIngestionEventData> {

    private final Logger logger = LoggerFactory.getLogger(FileEncryptionProcessor.class);

    private final File secretRing;
    private final File secretRingPassphrase;

    public FileEncryptionProcessor(final File secretRing, final File secretRingPassphrase) {
        this.secretRing = secretRing;
        this.secretRingPassphrase = secretRingPassphrase;
    }

    @Override
    public Result encrypt(final IIngestionEventData data) {
        logger.info("Starting process for file {}", data.getEncryptedFile().getAbsolutePath());
        try {
            return doEncrypt(data);
        } catch (Exception e) {
            logger.info("Process for file {} is unsuccessful", data.getEncryptedFile().getAbsolutePath());
            return Result.failure(e.getMessage(), e);
        }
    }

    private Result doEncrypt(final IIngestionEventData data) throws Exception {
        final IngestionPipelineResult result =
                new DefaultIngestionPipeline(data.getEncryptedFile(),
                        secretRing, secretRingPassphrase, data.getOutputFile(), data.getEncryptionKey())
                        .process();

        assertChecksum(data, result);

        logger.info("Process for file {} has been finished successfully", data.getEncryptedFile().getAbsolutePath());
        return Result.success(result.toArchiveEvent());
    }

    private void assertChecksum(final IIngestionEventData data, IngestionPipelineResult result) throws Md5Mismatch {
        String userEncryptedMd5 = data.getEncryptedMD5();
        String calculatedEncryptedMd5 = result.getOriginalFile().getMd5();
        assertChecksum(data, "Encrypted file md5 mismatch", userEncryptedMd5, calculatedEncryptedMd5);
        String userPlainMd5 = data.getPlainMD5();
        String calculatedPlainMd5 = result.getMd5();
        assertChecksum(data, "Decrypted file md5 mismatch", userPlainMd5, calculatedPlainMd5);
        logger.info("File {} gpgMd5:{} plainMd5:{} cipMd5:{}", data.getEncryptedFile().getAbsolutePath(),
                userEncryptedMd5, userPlainMd5, result.getEncryptedFile().getMd5());
    }

    private void assertChecksum(final IIngestionEventData data, String text, String expected, String actual)
            throws Md5Mismatch {
        if (expected == null || !expected.equalsIgnoreCase(actual)) {
            logger.info("File {} expected:{} actual:{}", data.getEncryptedFile().getAbsolutePath(),
                    expected, actual);
            throw new Md5Mismatch(text, expected, actual);
        }
    }
}
