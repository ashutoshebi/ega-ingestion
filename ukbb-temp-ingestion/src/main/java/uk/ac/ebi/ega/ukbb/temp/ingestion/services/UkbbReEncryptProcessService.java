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
package uk.ac.ebi.ega.ukbb.temp.ingestion.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.ukbb.temp.ingestion.exceptions.TerminateProgramException;
import uk.ac.ebi.ega.ukbb.temp.ingestion.properties.ReEncryptProperties;
import uk.ac.ebi.ega.ukbb.temp.ingestion.reencryption.ReEncryptionResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class UkbbReEncryptProcessService implements IUkbbReEncryptProcessService {

    private static final Logger logger = LoggerFactory.getLogger(UkbbReEncryptProcessService.class);

    private IReEncryptService reEncryptService;

    private IUkbbJobService ukbbJobService;

    private ProFilerService proFilerService;

    private ReEncryptProperties reEncryptProperties;

    public UkbbReEncryptProcessService(IReEncryptService reEncryptService, IUkbbJobService ukbbJobService,
                                       ProFilerService proFilerService, ReEncryptProperties reEncryptProperties) {
        this.reEncryptService = reEncryptService;
        this.ukbbJobService = ukbbJobService;
        this.proFilerService = proFilerService;
        this.reEncryptProperties = reEncryptProperties;
    }

    @Override
    public void reEncrypt(Path srcKeyFile, Path dstKeyFile, Path filePath) throws TerminateProgramException {
        try {
            doReEncrypt(srcKeyFile, dstKeyFile, filePath);
        } catch (TerminateProgramException e) {
            storeErrorResult(filePath, e);
            throw e;
        }
    }

    private void doReEncrypt(Path srcKeyFile, Path dstKeyFile, Path filePath) throws TerminateProgramException {
        if (ukbbJobService.isJobFinishedSuccessfully(filePath)) {
            logger.info("Skipping reEncryption for file {} has been successfully completed.", filePath);
            return;
        }
        final long jobId = ukbbJobService.startJob(filePath);
        char[] srcKey = doReadPassword(srcKeyFile);
        char[] dstKey = doReadPassword(dstKeyFile);
        final File file = getSourceFileIfExists(filePath);

        Path outputFilePath = createOutputFilePath(file);
        try {
            String plainFileMd5 = ukbbJobService.getSummaryFileMd5(file);
            final ReEncryptionResult result = doReEncrypt(srcKey, dstKey, file, outputFilePath);
            checkChecksum(outputFilePath, plainFileMd5, result);
            Long fireId = storeFileInFire(jobId, outputFilePath, result);
            ukbbJobService.finishJob(file, outputFilePath, result.getOriginalEncryptedMd5(),
                    result.getUnencryptedMd5(), result.getNewReEncryptedMd5(), result.getUnencryptedSize(), fireId);
            deleteOriginalFireIfSavingOnFire(file);

            logger.info("ReEncryption for file {} finished successfully", file.getAbsolutePath());
        } catch (IOException | AlgorithmInitializationException | RuntimeException e) {
            logger.error(e.getMessage(), e);
            final File outputFile = outputFilePath.toFile();
            if (outputFile.exists()) {
                outputFile.delete();
            }
            throw TerminateProgramException.unexpectedException(e);
        }
    }

    private ReEncryptionResult doReEncrypt(char[] srcKey, char[] dstKey, File file, Path outputFilePath) throws IOException, AlgorithmInitializationException {
        logger.info("ReEncryption for file {} started, output file {}", file.getAbsolutePath(), outputFilePath);
        final ReEncryptionResult result = reEncryptService.reEncrypt(srcKey, dstKey, file, outputFilePath);
        logger.info("ReEncryption for file {} finished.", file.getAbsolutePath());
        logger.info("Md5 file {}, plain {}, encrypted {}, size {}", result.getOriginalEncryptedMd5(),
                result.getUnencryptedMd5(), result.getNewReEncryptedMd5(), result.getUnencryptedSize());
        return result;
    }

    private void deleteOriginalFireIfSavingOnFire(File file) {
        if (reEncryptProperties.isStoreFileInFire()) {
            file.delete();
        }
    }

    private Long storeFileInFire(long jobId, Path outputFilePath, ReEncryptionResult reEncryptionResult) {
        if (reEncryptProperties.isStoreFileInFire()) {
            long fileId = proFilerService.insertFile("UKBB_" + jobId, outputFilePath.toFile(),
                    reEncryptionResult.getNewReEncryptedMd5());
            return proFilerService.insertArchive(fileId, relativePathToFire(outputFilePath), outputFilePath.toFile(),
                    reEncryptionResult.getNewReEncryptedMd5());
        } else {
            logger.info("Ingestion into fire skipped");
            return null;
        }
    }

    private String relativePathToFire(Path outputFilePath) {
        // TODO
        return null;
    }

    private void checkChecksum(Path outputFilePath, String plainFileMd5, ReEncryptionResult reEncryptionResult)
            throws TerminateProgramException {
        if (!Objects.equals(plainFileMd5, reEncryptionResult.getUnencryptedMd5())) {
            logger.error("File md5 missmatch manifest {}, calculated {}.",
                    plainFileMd5, reEncryptionResult.getUnencryptedMd5());
            outputFilePath.toFile().delete();
            throw TerminateProgramException.checksumMissmatch();
        }
    }

    private File getSourceFileIfExists(Path filePath) throws TerminateProgramException {
        final File file = filePath.toFile();
        if (!file.exists()) {
            throw TerminateProgramException.fileNotFound(filePath);
        }
        return file;
    }

    private char[] doReadPassword(Path file) throws TerminateProgramException {
        try {
            return FileUtils.readPasswordFile(file);
        } catch (IOException e) {
            throw TerminateProgramException.fileNotFound(file);
        }
    }

    private Path createOutputFilePath(File file) {
        final Path ukbbPath = Paths.get(reEncryptProperties.getUkbbPath());
        final Path relativePath = ukbbPath.relativize(file.toPath().getParent());
        return Paths.get(reEncryptProperties.getStagingPath())
                .resolve(reEncryptProperties.getRelativePathInsideStaging())
                .resolve(relativePath).resolve(file.getName().concat(".cip"));
    }

    private void storeErrorResult(Path filePath, TerminateProgramException e) {
        ukbbJobService.finishJob(filePath, e);
    }

}
