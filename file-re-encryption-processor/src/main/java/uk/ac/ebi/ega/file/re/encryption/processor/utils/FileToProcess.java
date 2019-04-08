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
package uk.ac.ebi.ega.file.re.encryption.processor.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.file.encryption.processor.exceptions.SkipIngestionException;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.file.re.encryption.processor.exceptions.SkipIngestionException;
import uk.ac.ebi.ega.file.re.encryption.processor.pipelines.exceptions.SystemErrorException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

public class FileToProcess {

    private final static Logger logger = LoggerFactory.getLogger(FileToProcess.class);

    private File path;

    private File stagingPath;

    private final long size;

    private final long lastUpdate;

    public FileToProcess(Path path, long size, LocalDateTime lastUpdate, Path stagingRoot, String filename) {
        this.path = path.toFile();
        this.size = size;
        this.lastUpdate = DateUtils.toEpochMilliseconds(lastUpdate);
        this.stagingPath = stagingRoot.resolve(filename).toFile();
    }

    public void moveFileToStaging() throws SkipIngestionException, SystemErrorException {
        if (!stagingPath.exists() && path.exists()) {
            assertFileHasNotChangedOrMoved();
            try {
                Files.move(path.toPath(), stagingPath.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                throw new SystemErrorException(e);
            }
            logger.info("File {} moved to {}", path.getAbsolutePath(), stagingPath.getAbsolutePath());
        }
    }

    private void assertFileHasNotChangedOrMoved() throws SkipIngestionException {
        if (!path.exists()) {
            throw SkipIngestionException.fileNotFound(path);
        }
        if ((path.lastModified() != lastUpdate) || (path.length() != size)) {
            throw SkipIngestionException.fileModified(path);
        }
    }

    public void rollbackFileToStaging() {
        if (stagingPath.exists()) {
            try {
                Files.move(stagingPath.toPath(), path.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                logger.error("Error while rollback of file '{}'", stagingPath.getAbsolutePath());
            }
        }
    }

    public File getStagingFile() {
        return stagingPath;
    }

    public void deleteStagingFile() {
        stagingPath.delete();
    }
}
