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
package uk.ac.ebi.ega.file.encryption.processor.jobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.file.encryption.processor.exceptions.SkipIngestionException;
import uk.ac.ebi.ega.file.encryption.processor.jobs.exceptions.Md5Mismatch;
import uk.ac.ebi.ega.ingestion.commons.messages.ArchiveEvent;
import uk.ac.ebi.ega.file.encryption.processor.models.IngestionProcess;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.DefaultIngestionPipeline;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.IngestionPipelineResult;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.UserErrorException;
import uk.ac.ebi.ega.file.encryption.processor.services.IPasswordGeneratorService;
import uk.ac.ebi.ega.jobs.core.Job;
import uk.ac.ebi.ega.jobs.core.Result;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class EncryptJob implements Job<IngestionProcess> {

    private final static Logger logger = LoggerFactory.getLogger(EncryptJob.class);

    private File secretRing;
    private File secretRingPassphrase;
    private IPasswordGeneratorService encryptPasswordService;

    public EncryptJob(File secretRing, File secretRingPassphrase, IPasswordGeneratorService encryptPasswordService) {
        this.secretRing = secretRing;
        this.secretRingPassphrase = secretRingPassphrase;
        this.encryptPasswordService = encryptPasswordService;
    }

    @Override
    public Result execute(IngestionProcess event) {
        final LocalDateTime start = LocalDateTime.now();
        logger.info("Starting process for file {}:/{}", event.getLocationId(),
                event.getEncryptedFile().getFile().getAbsolutePath());

        try {
            event.moveFilesToStaging();

            File keyFile = new File(event.getOutputFile().getAbsolutePath() + ".key");
            char[] key = generateKeyAndStore(keyFile);

            final IngestionPipelineResult result =
                    new DefaultIngestionPipeline(event.getEncryptedFile().getStagingFile(),
                            secretRing, secretRingPassphrase, event.getOutputFile(), key).process();

            assertChecksum(event, result);

            final ArchiveEvent archiveEvent = buildCompleteMessage(event, result, keyFile, start);
            logger.info("Process for file {}:/{} finished successfully", event.getLocationId(),
                    event.getEncryptedFile().getFile().getAbsolutePath());
            return Result.success(archiveEvent, start);
        } catch (SystemErrorException | IOException e) {
            event.rollback();
            return Result.abort("System is unable to execute the task at the moment", e, start);
        } catch (Md5Mismatch e) {
            event.rollbackEncryptedFileDeleteMd5s();
            return Result.failure(e.getMessage(), e, start);
        } catch (UserErrorException e) {
            event.rollbackEncryptedFileDeleteMd5s();
            return Result.failure(e.getMessage(), e, start);
        } catch (SkipIngestionException e) {
            logger.info("Skipping process for file {}:/{}", event.getLocationId(),
                    event.getEncryptedFile().getFile().getAbsolutePath());
            event.rollback();
            return Result.failure("Process skipped, files not found or modified", e, start);
        }
    }

    private ArchiveEvent buildCompleteMessage(IngestionProcess event, IngestionPipelineResult result, File keyFile,
                                              LocalDateTime start) {
        String encryptedOriginalPath = event.getEncryptedFile().getFile().getAbsolutePath();
        String plainOriginalPath = encryptedOriginalPath.substring(0,encryptedOriginalPath.length()-4);
        return new ArchiveEvent(
                event.getAccountId(),
                event.getLocationId(),
                plainOriginalPath,
                result.getEncryptedFile().getFile().getAbsolutePath(),
                result.getBytesTransferred(),
                result.getMd5(),
                result.getEncryptedFile().getFileSize(),
                result.getEncryptedFile().getMd5(),
                keyFile.getAbsolutePath(),
                start,
                LocalDateTime.now()
        );
    }

    private char[] generateKeyAndStore(File keyFile) throws IOException {
        final char[] key = encryptPasswordService.generate();
        try (FileWriter fw = new FileWriter(keyFile)) {
            fw.write(key);
        }
        return key;
    }

    private void assertChecksum(IngestionProcess event, IngestionPipelineResult result) throws IOException, Md5Mismatch {
        String userEncryptedMd5 = event.getEncryptedMd5();
        String calculatedEncryptedMd5 = result.getOriginalFile().getMd5();
        assertChecksum(event, "Encrypted file md5 mismatch", userEncryptedMd5, calculatedEncryptedMd5);
        String userPlainMd5 = event.getPlainMd5();
        String calculatedPlainMd5 = result.getMd5();
        assertChecksum(event, "Decrypted file md5 mismatch", userPlainMd5, calculatedPlainMd5);
        logger.info("File {}://{} gpgMd5:{} plainMd5:{} cipMd5:{}", event.getLocationId(),
                event.getEncryptedFile().getFile().getAbsolutePath(),
                userEncryptedMd5, userPlainMd5, result.getEncryptedFile().getMd5());
    }

    private void assertChecksum(IngestionProcess event, String text, String expected, String actual)
            throws Md5Mismatch {
        if (expected == null || !expected.equalsIgnoreCase(actual)) {
            logger.info("File {}://{} {} expected:{} actual:{}", event.getLocationId(),
                    event.getEncryptedFile().getFile().getAbsolutePath(), expected, actual);
            throw new Md5Mismatch(text, expected, actual);
        }
    }

}
