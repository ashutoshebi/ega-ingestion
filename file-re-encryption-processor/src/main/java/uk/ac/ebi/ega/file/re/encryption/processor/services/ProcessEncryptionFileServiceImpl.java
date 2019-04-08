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
package uk.ac.ebi.ega.file.re.encryption.processor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.file.encryption.processor.exceptions.SkipIngestionException;
import uk.ac.ebi.ega.file.encryption.processor.models.FileEncryptionJob;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.UserErrorException;
import uk.ac.ebi.ega.file.encryption.processor.utils.FileToProcess;
import uk.ac.ebi.ega.file.re.encryption.processor.exceptions.SkipIngestionException;
import uk.ac.ebi.ega.file.re.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.file.re.encryption.processor.pipelines.exceptions.UserErrorException;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

public class ProcessEncryptionFileServiceImpl implements ProcessEncryptionFileService {

    private final static Logger logger = LoggerFactory.getLogger(ProcessEncryptionFileServiceImpl.class);

    private FileEncryptionJobService jobService;

    private PipelineService pipelineService;

    private String clientId;

    private Path stagingRoot;

    public ProcessEncryptionFileServiceImpl(FileEncryptionJobService jobService, PipelineService pipelineService,
                                            String clientId, Path stagingRoot) {
        this.jobService = jobService;
        this.pipelineService = pipelineService;
        this.clientId = clientId;
        this.stagingRoot = stagingRoot;
    }

    @Override
    public void processFile(String accountId, String stagingId, Path filePath, long size, LocalDateTime lastUpdate,
                            Path md5FilePath, long md5Size, LocalDateTime md5LastUpdate) throws SystemErrorException, UserErrorException {
        String generatedName = generateName();
        logger.info("Starting process for file {} - id {}", filePath.toString(), generatedName);
        FileToProcess file = new FileToProcess(filePath, size, lastUpdate, stagingRoot, generatedName + ".gpg");
        FileToProcess fileMd5 = new FileToProcess(md5FilePath, size, lastUpdate, stagingRoot, generatedName + ".md5");


        FileEncryptionJob job = jobService.startJob(clientId, accountId, stagingId, file.getStagingFile(),
                fileMd5.getStagingFile());

        try {
            file.moveFileToStaging();
            fileMd5.moveFileToStaging();
            pipelineService.getPipeline(file.getStagingFile()).process();
        } catch (SystemErrorException e) {
            jobService.endJob(job, e);
            file.rollbackFileToStaging();
            fileMd5.rollbackFileToStaging();
            throw e;
        } catch (UserErrorException e) {
            jobService.endJob(job, e);
            file.rollbackFileToStaging();
            fileMd5.deleteStagingFile();
            throw e;
        } catch (SkipIngestionException e) {
            logger.info("Skipping process for file {} - id {}", filePath.toString(), generatedName);
            file.rollbackFileToStaging();
            fileMd5.rollbackFileToStaging();
            return;
        }
    }

    private String generateName() {
        return clientId + "-" + UUID.randomUUID();
    }

}
