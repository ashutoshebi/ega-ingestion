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
import uk.ac.ebi.ega.fire.models.OldFireFile;
import uk.ac.ebi.ega.ingestion.commons.models.FileStatus;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.EncryptedObject;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.EncryptedObjectRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class FileStatusUpdaterService implements IFileStatusUpdaterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStatusUpdaterService.class);

    private final EncryptedObjectRepository encryptedObjectRepository;
    private final IFireService fireService;
    private final int batchSize;

    public FileStatusUpdaterService(final EncryptedObjectRepository encryptedObjectRepository,
                                    final IFireService fireService,
                                    final int batchSize) {
        this.encryptedObjectRepository = encryptedObjectRepository;
        this.fireService = fireService;
        this.batchSize = batchSize;
    }

    @Override
    public void updateStatus() {
        LOGGER.info("FileStatusUpdaterService.updateStatus was called.");

        Page<EncryptedObject> pageContainingLocalFilesBeingArchived;
        Pageable pageRequest = PageRequest.of(0, batchSize);

        do {
            LOGGER.trace("Processing PageRequest: {}...", pageRequest);

            pageContainingLocalFilesBeingArchived = encryptedObjectRepository
                    .findByStatus(FileStatus.ARCHIVE_IN_PROGRESS, pageRequest);

            final List<EncryptedObject> localFilesBeingArchived = pageContainingLocalFilesBeingArchived.getContent();
            LOGGER.trace("Local files which are being archived and whose status should be updated: {}",
                    localFilesBeingArchived);

            if (!localFilesBeingArchived.isEmpty()) {
                updateStatus(localFilesBeingArchived);
            }

            pageRequest = pageRequest.next();
        } while (!pageContainingLocalFilesBeingArchived.isLast());
    }

    /**
     * @param encryptedObjects files which are in our local FileDetailsRepository
     *                         and which have the FileStatus.ARCHIVE_IN_PROGRESS status.
     * @see FileStatus
     */
    private void updateStatus(final List<EncryptedObject> encryptedObjects) {
        final List<OldFireFile> filesInFire = getFilesInFireCorrespondingTo(encryptedObjects);
        LOGGER.trace("The files in Fire, whose status will be used to update the local files which are being archived: {}",
                filesInFire);
        updateStatusesBasedOn(filesInFire, encryptedObjects);
    }

    private List<OldFireFile> getFilesInFireCorrespondingTo(final List<EncryptedObject> encryptedObjects) {
        final List<Long> fireIdsOfLocalFilesBeingArchived = encryptedObjects.stream()
                .map(EncryptedObject::getFireId)
                .collect(Collectors.toList());

        return fireService.findAllByFireId(fireIdsOfLocalFilesBeingArchived);
    }

    private void updateStatusesBasedOn(final List<OldFireFile> filesInFire,
                                       final List<EncryptedObject> objects) {

        // FireId => current FileStatus in Fire
        final Map<Long, Optional<FileStatus>> fireIdsToFileStatuses = getFireIdsAndFileStatusesOf(filesInFire);
        LOGGER.trace("Map<\"FireId in Fire\", \"current FileStatus in Fire\">: {}", fireIdsToFileStatuses);

        for (final EncryptedObject object : objects) {
            final Long fireIdOfLocalFileBeingArchived = object.getFireId();
            final Long fireId = fireIdOfLocalFileBeingArchived;
            final Optional<FileStatus> optionalFileStatus = fireIdsToFileStatuses.get(fireId);

            if (optionalFileStatus.isPresent()) {
                final FileStatus fileStatusOfFileInFire = optionalFileStatus.get();

                switch (fileStatusOfFileInFire){
                    case ERROR:
                        LOGGER.error("The file in Fire with ega-pro-filer.ega_ARCHIVE.archive.archive_id={} is in an " +
                                "erroneous state.", fireId);
                        object.error();
                        encryptedObjectRepository.save(object);
                        break;
                    case ARCHIVED_SUCCESSFULLY:
                        //TODO we need to change the uri to the appropriate fire uri, for this we need to update the
                        // fire-core library :(
                        object.archived("fire://change");
                        encryptedObjectRepository.save(object);
                        break;
                }
            } else {
                final String message = String.format("The status of %s was not updated because " +
                        "the new status could not be determined.", object);
                LOGGER.error(message);
            }
        }
    }

    /**
     * Returns a map mapping the FireId to FileStatus of the given files.
     *
     * @param fireFiles list of files which are in Fire
     * @return a map: FireId => current FileStatus in Fire
     */
    private Map<Long, Optional<FileStatus>> getFireIdsAndFileStatusesOf(final List<OldFireFile> fireFiles) {
        return fireFiles.stream()
                .collect(Collectors.toMap(OldFireFile::getFireId, this::getFileStatus));
    }

    private Optional<FileStatus> getFileStatus(final OldFireFile fireFile) {
        LOGGER.trace("Getting the file status of the {} file (which is in Fire)...", fireFile);

        if (fireFile.getExitCode() == null && fireFile.getExitReason() == null) {
            return Optional.of(FileStatus.ARCHIVE_IN_PROGRESS);
        } else if (fireFile.getExitCode() != null && fireFile.getExitCode() == 0 && fireFile.getExitReason() == null) {
            return Optional.of(FileStatus.ARCHIVED_SUCCESSFULLY);
        } else if (fireFile.getExitCode() != null && fireFile.getExitCode() != 0) {
            return Optional.of(FileStatus.ERROR);
        } else {
            final String message = String.format("Unable to determine the status of the file " +
                    "based on its exitCode and exitReason: %s", fireFile);
            LOGGER.error(message);
            return Optional.empty();
        }
    }

}
