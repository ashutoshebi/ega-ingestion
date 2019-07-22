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
package uk.ac.ebi.ega.file.encryption.processor.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.file.encryption.processor.exceptions.SkipIngestionException;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.ingestion.commons.models.FileStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class FileToProcess {

    private final static Logger logger = LoggerFactory.getLogger(FileToProcess.class);

    private File file;

    private File stagingFile;

    private final long size;

    private final long lastModified;

    public FileToProcess(File file, File stagingFile, long size, long lastModified) {
        this.file = file;
        this.stagingFile = stagingFile;
        this.size = size;
        this.lastModified = lastModified;
    }

    public FileToProcess(Path rootPath, FileStatic file, Path stagingFilePath) {
        this(rootPath.resolve(file.getAbsolutePath()).toFile(),
                stagingFilePath.toFile(),
                file.length(),
                file.lastModified());
    }

    public void moveFileToStaging() throws SkipIngestionException, SystemErrorException {
        if (!stagingFile.exists() && file.exists()) {
            assertFileHasNotChangedOrMoved();
            try {
                Files.move(file.toPath(), stagingFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                throw new SystemErrorException(e);
            }
            logger.info("File {} moved to {}", file.getAbsolutePath(), stagingFile.getAbsolutePath());
        }
    }

    public void assertFileHasNotChangedOrMoved() throws SkipIngestionException {
        if (!file.exists()) {
            throw SkipIngestionException.fileNotFound(file);
        }
        if ((file.lastModified() != lastModified) || (file.length() != size)) {
            throw SkipIngestionException.fileModified(file);
        }
    }

    public void rollbackFileToStaging() {
        if (stagingFile.exists()) {
            try {
                Files.move(stagingFile.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                logger.error("Error while rollback of file '{}'", stagingFile.getAbsolutePath());
            }
        }
    }

    public File getFile() {
        return file;
    }

    public File getStagingFile() {
        return stagingFile;
    }

    public long getSize() {
        return size;
    }

    public long getLastModified() {
        return lastModified;
    }

    public void deleteStagingFile() {
        stagingFile.delete();
    }
}
