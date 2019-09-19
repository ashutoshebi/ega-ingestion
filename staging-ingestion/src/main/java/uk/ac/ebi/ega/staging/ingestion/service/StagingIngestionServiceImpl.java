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
package uk.ac.ebi.ega.staging.ingestion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.ingestion.commons.messages.IngestionEvent;
import uk.ac.ebi.ega.ingestion.commons.messages.NewFileEvent;
import uk.ac.ebi.ega.ingestion.commons.models.FileStatic;
import uk.ac.ebi.ega.staging.ingestion.service.exceptions.FileModified;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static uk.ac.ebi.ega.ingestion.commons.models.Encryption.PGP;

public class StagingIngestionServiceImpl implements StagingIngestionService {

    private final static Logger logger = LoggerFactory.getLogger(StagingIngestionService.class);

    private Path internalArea;

    public StagingIngestionServiceImpl(Path path) {
        this.internalArea = path;
    }

    @Override
    public Optional<NewFileEvent> ingest(String key, IngestionEvent ingestionEvent) throws IOException {
        try {
            String plainMd5 = readMd5(ingestionEvent.getRootPath(), ingestionEvent.getPlainMd5File());
            String encryptedMd5 = readMd5(ingestionEvent.getRootPath(), ingestionEvent.getEncryptedMd5File());
            final Path path = moveToInternalArea(ingestionEvent.getRootPath(), ingestionEvent.getEncryptedFile(), key);
            return Optional.of(new NewFileEvent(
                    ingestionEvent.getAccountId(),
                    ingestionEvent.getLocationId(),
                    removePGPExtension(ingestionEvent.getEncryptedFile().getAbsolutePath()),
                    ingestionEvent.getEncryptedFile().lastModified(),
                    path,
                    plainMd5,
                    encryptedMd5,
                    PGP
            ));
        } catch (FileNotFoundException | FileModified fileModified) {
            return Optional.empty();
        }
    }

    private String removePGPExtension(String absolutePath) {
        return absolutePath.substring(0, absolutePath.length() - 4);
    }

    @Override
    public void cleanMd5Files(IngestionEvent ingestionEvent) {
        deleteFile(ingestionEvent.getRootPath(), ingestionEvent.getPlainMd5File());
        deleteFile(ingestionEvent.getRootPath(), ingestionEvent.getEncryptedMd5File());
    }

    private String readMd5(Path rootPath, FileStatic md5File) throws IOException, FileModified {
        final Path filePath = rootPath.resolve(md5File.getAbsolutePath());
        assertFileExistsAndHasNotBeenModified(filePath.toFile(), md5File);
        return readMd5File(filePath);
    }

    private static String readMd5File(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim().toUpperCase();
    }

    private void assertFileExistsAndHasNotBeenModified(File file, FileStatic md5File)
            throws FileNotFoundException, FileModified {
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        if (file.lastModified() != md5File.lastModified()) {
            logger.info("File {} has been modified since last observed state.", md5File);
            throw new FileModified(file.getAbsolutePath());
        }
    }

    private Path moveToInternalArea(Path rootPath, FileStatic encryptedFile, String key)
            throws IOException, FileModified {
        final Path srcPath = rootPath.resolve(encryptedFile.getAbsolutePath());
        final Path dstPath = internalArea.resolve(key + "." + encryptedFile.lastModified());
        if (Files.exists(srcPath)) {
            if (srcPath.toFile().lastModified() != encryptedFile.lastModified()) {
                logger.info("File {} has been modified since last observed state.", encryptedFile);
                throw new FileModified(srcPath.toString());
            }
            return Files.move(srcPath, dstPath, ATOMIC_MOVE, REPLACE_EXISTING);
        } else {
            if (!Files.exists(dstPath)) {
                throw new FileNotFoundException(dstPath.toString());
            }
            return dstPath;
        }
    }

    private void deleteFile(Path rootPath, FileStatic file) {
        rootPath.resolve(file.getAbsolutePath()).toFile().delete();
    }

}
