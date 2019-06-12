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
import uk.ac.ebi.ega.file.encryption.processor.messages.EncryptComplete;
import uk.ac.ebi.ega.file.encryption.processor.models.EncryptJobParameters;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.IngestionPipelineFile;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.IngestionPipelineResult;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.UserErrorException;
import uk.ac.ebi.ega.file.encryption.processor.services.PipelineService;
import uk.ac.ebi.ega.file.encryption.processor.utils.FileToProcess;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Job;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

public class EncryptJob implements Job<EncryptJobParameters> {

    private final static Logger logger = LoggerFactory.getLogger(EncryptJob.class);

    private PipelineService pipelineService;
    private String clientId;
    private Path stagingRoot;
    //  private EncryptionAlgorithm encryptionAlgorithm; //TODO Not sure about purpose for sending encrypted password to kafka

    public EncryptJob(PipelineService pipelineService, String clientId, Path stagingRoot/*,
                      EncryptionAlgorithm encryptionAlgorithm*/) {
        this.pipelineService = pipelineService;
        this.clientId = clientId;
        this.stagingRoot = stagingRoot;
        //   this.encryptionAlgorithm = encryptionAlgorithm;
    }

    @Override
    public Result execute(EncryptJobParameters jobParameters) {

        final LocalDateTime start = LocalDateTime.now();

        final Path filePath = jobParameters.getFilePath();
        final long size = jobParameters.getSize();
        final LocalDateTime lastUpdate = jobParameters.getLastUpdate();
        final Path md5FilePath = jobParameters.getMd5FilePath();

        String generatedName = generateName();
        logger.info("Starting process for file {} - id {}", filePath.toString(), generatedName);
        FileToProcess file = new FileToProcess(filePath, size, lastUpdate, stagingRoot, generatedName + ".gpg");
        FileToProcess fileMd5 = new FileToProcess(md5FilePath, size, lastUpdate, stagingRoot, generatedName + ".md5");

        try {
            file.moveFileToStaging();
            fileMd5.moveFileToStaging();
            final IngestionPipelineResult ingestionPipelineResult = pipelineService.getPipeline(file.getStagingFile())
                    .process();

            final LocalDateTime end = LocalDateTime.now();

            final IngestionPipelineFile originalIngestionPipelineFile = ingestionPipelineResult.getOriginalFile();
            final IngestionPipelineFile encryptedIngestionPipelineFile = ingestionPipelineResult.getEncryptedFile();

            final EncryptComplete encryptComplete = new EncryptComplete(generatedName, originalIngestionPipelineFile.getFileSize(),
                    originalIngestionPipelineFile.getMd5(), encryptedIngestionPipelineFile.getFileSize(), encryptedIngestionPipelineFile.getMd5(),
                    "", Result.Status.SUCCESS, "Encryption process has been completed successfully", start, end);//TODO Need to Encrypt the password & pass here. Currently not sure about the purpose.

            return Result.success(encryptComplete);//TODO need to check purpose of sending start time. removed for the time being.
        } catch (SystemErrorException | IOException e) { //TODO need to check can be clubbed with SystemErrorException
            file.rollbackFileToStaging();
            fileMd5.rollbackFileToStaging();
            return Result.abort("System is unable to execute the task at the moment", e, start);
        } catch (UserErrorException e) {
            file.rollbackFileToStaging();
            fileMd5.deleteStagingFile();
            return Result.abort("User has provided invalid input", e, start);
        } catch (SkipIngestionException e) {
            logger.error("Skipping process for file {} - id {}", filePath.toString(), generatedName);
            file.rollbackFileToStaging();
            fileMd5.rollbackFileToStaging();
            return Result.abort("File not found", e, start);
        }//TODO Need to revisit error messages/constant if not done appropriately
    }

    private String generateName() {
        return clientId + "-" + UUID.randomUUID();
    }
}
