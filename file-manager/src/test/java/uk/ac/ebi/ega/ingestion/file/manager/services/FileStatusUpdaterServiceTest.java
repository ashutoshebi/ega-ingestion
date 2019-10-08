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
package uk.ac.ebi.ega.ingestion.file.manager.services;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.ega.fire.ingestion.service.IFireService;
import uk.ac.ebi.ega.fire.models.OldFireFile;
import uk.ac.ebi.ega.ingestion.commons.models.Encryption;
import uk.ac.ebi.ega.ingestion.commons.models.FileStatus;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.EncryptedObject;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.EncryptedObjectRepository;

import java.net.URI;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@AutoConfigureDataJpa
@AutoConfigureTestDatabase
@AutoConfigureTestEntityManager
@TestPropertySource(locations = "classpath:test.properties")
public class FileStatusUpdaterServiceTest {

    private static final Long FIRE_ID = 12L;

    @Autowired
    private EncryptedObjectRepository encryptedObjectRepository;

    private final IFireService fireService = mock(IFireService.class);

    private FileStatusUpdaterService fileStatusUpdaterService;

    @Before
    public void setUp() {
        fileStatusUpdaterService = new FileStatusUpdaterService(encryptedObjectRepository, fireService, 20);
    }

    @Test
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void whenTheFileIsArchivedInFireThenTheLocalFileIsUpdatedToBeArchivedToo() {
        createFileInLocalDBWhichIsBeingArchived();
        createArchivedFileInFire();

        fileStatusUpdaterService.updateStatus();

        final EncryptedObject updatedFileDetails = encryptedObjectRepository.findAll().iterator().next();
        assertEquals(FileStatus.ARCHIVED_SUCCESSFULLY, updatedFileDetails.getStatus());
    }

    @Test
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void whenTheFileHasErrorInFireThenTheLocalFileIsUpdatedToHaveErrorToo() {
        createFileInLocalDBWhichIsBeingArchived();
        createErroneousFileInFire();

        fileStatusUpdaterService.updateStatus();

        final EncryptedObject encryptedObject = encryptedObjectRepository.findAll().iterator().next();
        assertEquals(FileStatus.ERROR, encryptedObject.getStatus());
    }

    @Test
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void whenTheFileInFireIsStillBeingArchivedThenTheLocalFileIsUpdatedToBeingArchivedToo() {
        createFileInLocalDBWhichIsBeingArchived();
        createFileInFireWhichIsStillBeingArchived();

        fileStatusUpdaterService.updateStatus();

        final EncryptedObject updatedFileDetails = encryptedObjectRepository.findAll().iterator().next();
        assertEquals(FileStatus.ARCHIVE_IN_PROGRESS, updatedFileDetails.getStatus());
    }

    @Test
    @Transactional
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void whenTheFileInFireIsInAnUnknownStateThenTheLocalFileIsNotChanged() {
        createFileInLocalDBWhichIsBeingArchived();
        createFileInFireWhichIsInAStrangeState();

        fileStatusUpdaterService.updateStatus();

        final EncryptedObject encryptedObject = encryptedObjectRepository.findAll().iterator().next();
        assertEquals(FileStatus.ARCHIVE_IN_PROGRESS, encryptedObject.getStatus());
    }

    private void createArchivedFileInFire() {
        fireShouldReturnGivenFile(new OldFireFile(FIRE_ID, 0, null));
    }

    private void createErroneousFileInFire() {
        fireShouldReturnGivenFile(new OldFireFile(FIRE_ID, 2, "error message"));
    }

    private void createFileInFireWhichIsStillBeingArchived() {
        fireShouldReturnGivenFile(new OldFireFile(FIRE_ID, null, null));
    }

    private void createFileInFireWhichIsInAStrangeState() {
        fireShouldReturnGivenFile(new OldFireFile(FIRE_ID, null, "message"));
    }

    private void fireShouldReturnGivenFile(final OldFireFile fireFile) {
        when(fireService.findAllByFireId(Collections.singletonList(FIRE_ID))).thenReturn(Collections.singletonList(fireFile));
    }

    private void createFileInLocalDBWhichIsBeingArchived() {
        final EncryptedObject encryptedObject = createFileDetails();
        encryptedObjectRepository.save(encryptedObject);
    }

    private EncryptedObject createFileDetails() {
        final EncryptedObject encryptedObject = new EncryptedObject("", "", "", 0L, "", "", 0L, "");
        encryptedObject.archive("file://test", FIRE_ID, "", 0L, 0L, Encryption.EGA_AES, "");
        return encryptedObject;
    }

    @TestConfiguration
    @EnableJpaRepositories(basePackages = {"uk.ac.ebi.ega.ingestion.file.manager.persistence.repository"})
    @EntityScan(basePackages = {"uk.ac.ebi.ega.ingestion.file.manager.persistence.entities"})
    @EnableJpaAuditing
    static class Configuration {

    }

}
