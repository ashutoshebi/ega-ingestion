/*
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
 */
package uk.ac.ebi.ega.ingestion.file.manager.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.ac.ebi.ega.fire.ingestion.service.IFireService;
import uk.ac.ebi.ega.fire.ingestion.service.OldFireFile;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileDetails;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileStatus;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.FileDetailsRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FileStatusUpdaterService implements IFileStatusUpdaterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStatusUpdaterService.class);

    private final FileDetailsRepository fileDetailsRepository;
    private final IFireService fireService;
    private final int batchSize;

    public FileStatusUpdaterService(final FileDetailsRepository fileDetailsRepository,
                                    final IFireService fireService,
                                    final int batchSize) {
        this.fileDetailsRepository = fileDetailsRepository;
        this.fireService = fireService;
        this.batchSize = batchSize;
    }

    @Override
    public void updateStatus() {
        LOGGER.trace("FileStatusUpdaterService.updateStatus was called.");

        Page<FileDetails> pageContainingLocalFilesBeingArchived;
        Pageable pageRequest = PageRequest.of(0, batchSize);

        do {
            LOGGER.trace("Processing PageRequest: {}...", pageRequest);

            pageContainingLocalFilesBeingArchived = fileDetailsRepository
                    .findByStatus(FileStatus.ARCHIVE_IN_PROGRESS, pageRequest);

            final List<FileDetails> localFilesBeingArchived = pageContainingLocalFilesBeingArchived.getContent();
            LOGGER.trace("Local files which are being archived and whose status should be updated: {}",
                    localFilesBeingArchived);

            if (!localFilesBeingArchived.isEmpty()) {
                updateStatus(localFilesBeingArchived);
            }

            pageRequest = pageRequest.next();
        } while (!pageContainingLocalFilesBeingArchived.isLast());
    }

    /**
     * @param localFilesBeingArchived files which are in our local FileDetailsRepository
     *                                and which have the FileStatus.ARCHIVE_IN_PROGRESS status.
     *
     * @see uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileStatus
     */
    private void updateStatus(final List<FileDetails> localFilesBeingArchived) {
        final List<OldFireFile> filesInFire = getFilesInFireCorrespondingTo(localFilesBeingArchived);
        LOGGER.trace("The files in Fire, whose status will be used to update the local files which are being archived: {}",
                filesInFire);
        updateStatusesBasedOn(filesInFire, localFilesBeingArchived);
    }

    private List<OldFireFile> getFilesInFireCorrespondingTo(final List<FileDetails> localFilesBeingArchived) {
        final List<Long> fileIdsOfLocalFilesBeingArchived = localFilesBeingArchived.stream()
                .map(FileDetails::getFileId)
                .collect(Collectors.toList());

        return fireService.findAllByFileId(fileIdsOfLocalFilesBeingArchived);
    }

    private void updateStatusesBasedOn(final List<OldFireFile> filesInFire,
                                       final List<FileDetails> localFilesBeingArchived) {

        // FileId => current FileStatus in Fire
        final Map<Long, Optional<FileStatus>> fileIdsToFileStatuses = getFileIdsAndFileStatusesOf(filesInFire);
        LOGGER.trace("Map<\"FileId in Fire\", \"current FileStatus in Fire\">: {}", fileIdsToFileStatuses);

        for (final FileDetails localFileBeingArchived : localFilesBeingArchived) {
            final Long fileIdOfLocalFileBeingArchived = localFileBeingArchived.getFileId();
            final Long fileId = fileIdOfLocalFileBeingArchived;
            final Optional<FileStatus> optionalFileStatus = fileIdsToFileStatuses.get(fileId);
            LOGGER.trace("Local file {} might be updated with status {}", localFileBeingArchived, optionalFileStatus);

            if (optionalFileStatus.isPresent()) {
                final FileStatus fileStatusOfFileInFire = optionalFileStatus.get();

                if (FileStatus.ERROR.equals(fileStatusOfFileInFire)) {
                    LOGGER.error("The file in Fire with EGA_FILE_STABLE_ID={} is in an erroneous state.", fileId);
                }

                LOGGER.trace("Local file {} will be updated with status {}", localFileBeingArchived, optionalFileStatus);
                localFileBeingArchived.setStatus(fileStatusOfFileInFire);
                fileDetailsRepository.save(localFileBeingArchived);
            } else {
                final String message = String.format("The status of %s was not updated because " +
                                "the new status could not be determined.", localFileBeingArchived);
                LOGGER.error(message);
            }
        }
    }

    /**
     * Returns a map mapping the FileId to FileStatus of the given files.
     * @param fireFiles list of files which are in Fire
     * @return a map: FileId => current FileStatus in Fire
     */
    private Map<Long, Optional<FileStatus>> getFileIdsAndFileStatusesOf(final List<OldFireFile> fireFiles) {
        return fireFiles.stream()
                .collect(Collectors.toMap(OldFireFile::getFileId, this::getFileStatus));
    }

    private Optional<FileStatus> getFileStatus(final OldFireFile fireFile) {
        LOGGER.trace("Getting the file status of the {} file (which is in Fire)...", fireFile);

        if (fireFile.getExitCode() == null && fireFile.getExitReason() == null) {
            return Optional.of(FileStatus.ARCHIVE_IN_PROGRESS);
        }
        else if (fireFile.getExitCode() != null && fireFile.getExitCode() == 0 && fireFile.getExitReason() == null) {
            return Optional.of(FileStatus.ARCHIVED_SUCCESSFULLY);
        }
        else if (fireFile.getExitCode() != null && fireFile.getExitCode() != 0) {
            return Optional.of(FileStatus.ERROR);
        }
        else {
            final String message = String.format("Unable to determine the status of the file " +
                    "based on its exitCode and exitReason: %s", fireFile);
            LOGGER.error(message);
            return Optional.empty();
        }
    }

}
