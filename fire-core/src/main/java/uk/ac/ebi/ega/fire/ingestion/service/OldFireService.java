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
package uk.ac.ebi.ega.fire.ingestion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.NoSuchObjectException;
import java.util.List;
import java.util.Optional;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

public class OldFireService implements IFireService {

    private final static Logger logger = LoggerFactory.getLogger(OldFireService.class);

    private final Path fireStaging;

    private final IProFilerDatabaseService proFilerDatabaseService;

    public OldFireService(Path fireStaging, IProFilerDatabaseService proFilerDatabaseService) {
        this.fireStaging = fireStaging;
        this.proFilerDatabaseService = proFilerDatabaseService;
    }

    @Override
    public Optional<Long> archiveFile(String egaFileId, File file, String md5, String pathOnFire) {
        logger.debug("Started archiving the {} file with the following parameters: " +
                        "egaFileId: {}, md5: {}, pathOnFire: {}", file, egaFileId, md5, pathOnFire);

        try {
            File fileInStaging = moveFileToFireStaging(file, pathOnFire);
            long archiveId = proFilerDatabaseService.archiveFile(egaFileId, fileInStaging, md5, pathOnFire);
            logger.debug("The {} file has been archived. archiveId: {}, egaFileId: {}, fileInStaging: {}, md5: {}, pathOnFire: {}",
                    file, archiveId, egaFileId, fileInStaging, md5, pathOnFire);
            return Optional.of(archiveId);
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public List<OldFireFile> findAllByFireId(final List<Long> fireIds) {
        return proFilerDatabaseService.findAllByFireId(fireIds);
    }

    private File moveFileToFireStaging(File file, String pathOnFire) throws IOException {
        final Path fileInStagingPath = fireStaging.resolve(pathOnFire).resolve(file.getName());
        final File fileInStaging = fileInStagingPath.toFile();

        try {
            logger.debug("About to move '{}' to '{}'", file.getAbsolutePath(), fileInStaging.getAbsolutePath());
            Files.createDirectories(fileInStagingPath.getParent());
            Files.move(file.toPath(), fileInStagingPath, ATOMIC_MOVE);
            logger.info("File '{}' moved to '{}'", file.getAbsolutePath(), fileInStaging.getAbsolutePath());
            return fileInStaging;
        } catch (NoSuchObjectException e) {
            if (fileInStaging.exists()) {
                logger.info("File '{}' is already present", fileInStaging.getAbsolutePath());
                return fileInStaging;
            } else {
                throw e;
            }
        }
    }

}
