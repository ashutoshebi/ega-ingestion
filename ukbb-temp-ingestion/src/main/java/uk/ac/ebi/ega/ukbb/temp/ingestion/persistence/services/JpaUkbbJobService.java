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
package uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.ukbb.temp.ingestion.exceptions.TerminateProgramException;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.entity.UkBiobankReEncryptedFileEntity;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankFilesRepository;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankReEncryptedFilesRepository;
import uk.ac.ebi.ega.ukbb.temp.ingestion.services.IUkbbJobService;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

public class JpaUkbbJobService implements IUkbbJobService {

    private static final Logger logger = LoggerFactory.getLogger(JpaUkbbJobService.class);

    private UkBiobankFilesRepository filesRepository;

    private UkBiobankReEncryptedFilesRepository reEncryptedFilesRepository;

    public JpaUkbbJobService(UkBiobankFilesRepository filesRepository,
                             UkBiobankReEncryptedFilesRepository reEncryptedFilesRepository) {
        this.filesRepository = filesRepository;
        this.reEncryptedFilesRepository = reEncryptedFilesRepository;
    }

    @Override
    public boolean isJobFinishedSuccessfully(Path file) {
        final Optional<UkBiobankReEncryptedFileEntity> fileEntity =
                reEncryptedFilesRepository.findByOriginalFilePath(file.toString());
        return fileEntity.isPresent() && fileEntity.get().isFinishedSuccessfully();
    }

    @Override
    public String getSummaryFileMd5(File file) {
        return filesRepository.findByFilePath(file.getAbsolutePath()).get().getMd5Checksum();
    }

    @Override
    public void finishJob(File file, Path outputFilePath, String originalEncryptedMd5, String unencryptedMd5,
                          String newReEncryptedMd5, long unencryptedSize, Long fireId) {
        final UkBiobankReEncryptedFileEntity fileEntity =
                reEncryptedFilesRepository.findByOriginalFilePath(file.getAbsolutePath()).get();

        fileEntity.finish(outputFilePath, originalEncryptedMd5, unencryptedMd5, newReEncryptedMd5, unencryptedSize,
                fireId);
        reEncryptedFilesRepository.save(fileEntity);
    }

    @Override
    public void finishJob(Path filePath, TerminateProgramException e) {
        reEncryptedFilesRepository.findByOriginalFilePath(filePath.toString()).ifPresent(entity -> {
            entity.finish(e);
            reEncryptedFilesRepository.save(entity);
        });
    }

    @Override
    public long startJob(Path file) throws TerminateProgramException {
        checkIfFileExists(file.toFile());
        final UkBiobankReEncryptedFileEntity fileEntity = reEncryptedFilesRepository
                .findByOriginalFilePath(file.toString())
                .orElse(new UkBiobankReEncryptedFileEntity(file));
        fileEntity.start();
        return reEncryptedFilesRepository.save(fileEntity).getReEncryptedFileId();
    }

    private void checkIfFileExists(File file) throws TerminateProgramException {
        if (!filesRepository.findByFilePath(file.getAbsolutePath()).isPresent()) {
            throw TerminateProgramException.fileNotInDataset(file);
        }
    }

}
