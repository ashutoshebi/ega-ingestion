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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.ega.fire.ingestion.service.IFireService;
import uk.ac.ebi.ega.ingestion.commons.messages.ArchiveEvent;
import uk.ac.ebi.ega.ingestion.file.manager.controller.exceptions.FileHierarchyException;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileHierarchy;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.FileHierarchyRepository;
import uk.ac.ebi.ega.ingestion.file.manager.utils.FileStructureType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;

@Sql(scripts = "classpath:cleanDatabase.sql")
@RunWith(SpringRunner.class)
@AutoConfigureDataJpa
@AutoConfigureTestDatabase
@AutoConfigureTestEntityManager
@TestPropertySource(locations = "classpath:test.properties")
public class FileManagerServiceTest {

    private IFileManagerService fileManagerService;

    @MockBean
    private IFireService fireService;

    @Autowired
    private FileHierarchyRepository fileHierarchyRepository;

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @TestConfiguration
    @EnableJpaRepositories(basePackages = {"uk.ac.ebi.ega.ingestion.file.manager.persistence.repository"})
    @EntityScan(basePackages = {"uk.ac.ebi.ega.ingestion.file.manager.persistence.entities"})
    @EnableJpaAuditing
    static class Configuration {

        @Bean("transactionManager")
        public PlatformTransactionManager jpaTransactionManager() {
            return new JpaTransactionManager();
        }
    }

    @Before
    public void init() {
        fileManagerService = Mockito.spy(new FileManagerService(fireService, Paths.get("/test/path"), fileHierarchyRepository));
    }

    @Transactional
    @Test
    public void archive_CreatesAndSavesFileHierarchy() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
    }

    @Transactional
    @Test
    public void archive_WhenPassBadPath_ThenCorrectsAndCreatesAndSavesFileHierarchy() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs//ega/public///ega-box-01-012345677890.cip/"));
    }

    @Transactional
    @Test
    public void findAllByPath_WhenPassValidFilePath_ThenReturnsFiles() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();

        final Optional<List<FileHierarchy>> fileHierarchies = fileManagerService.findAllByPath(Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip"));

        assertNotNull(fileHierarchies);
        assertTrue(fileHierarchies.isPresent());
        assertFalse(fileHierarchies.get().isEmpty());

        final FileHierarchy fileHierarchy = fileHierarchies.get().get(0);

        assertEquals(FileStructureType.FILE, fileHierarchy.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchy.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchy.getName());

        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAllByPath_WhenPassValidFolderPath_ThenReturnsChildFile() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();

        final Optional<List<FileHierarchy>> fileHierarchies = fileManagerService.findAllByPath(Paths.get("/nfs/ega/public"));

        assertNotNull(fileHierarchies);
        assertTrue(fileHierarchies.isPresent());
        assertFalse(fileHierarchies.get().isEmpty());

        final FileHierarchy fileHierarchy = fileHierarchies.get().get(0);

        assertEquals(FileStructureType.FILE, fileHierarchy.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchy.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchy.getName());

        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAllByPath_WhenPassValidFolderPath_ThenReturnsChildFolder() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();

        final Optional<List<FileHierarchy>> fileHierarchies = fileManagerService.findAllByPath(Paths.get("/nfs/ega"));

        assertNotNull(fileHierarchies);
        assertTrue(fileHierarchies.isPresent());
        assertFalse(fileHierarchies.get().isEmpty());

        final FileHierarchy fileHierarchy = fileHierarchies.get().get(0);

        assertEquals(FileStructureType.FOLDER, fileHierarchy.getFileType());
        assertEquals("/nfs/ega/public", fileHierarchy.getOriginalPath());
        assertEquals("public", fileHierarchy.getName());

        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAllByPath_WhenArchivedWithBadFilePath_ThenReturnsFiles() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs//ega/public///ega-box-01-012345677890.cip/"));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();

        final Optional<List<FileHierarchy>> fileHierarchies = fileManagerService.findAllByPath(Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip"));

        assertNotNull(fileHierarchies);
        assertTrue(fileHierarchies.isPresent());
        assertFalse(fileHierarchies.get().isEmpty());

        final FileHierarchy fileHierarchy = fileHierarchies.get().get(0);

        assertEquals(FileStructureType.FILE, fileHierarchy.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchy.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchy.getName());

        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAllByPath_WhenRetrieveFilesWithBadFilePath_ThenReturnsFiles() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));

        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();

        final Optional<List<FileHierarchy>> fileHierarchies = fileManagerService.findAllByPath(Paths.get("/nfs//ega/public///ega-box-01-012345677890.cip/"));

        assertNotNull(fileHierarchies);
        assertTrue(fileHierarchies.isPresent());
        assertFalse(fileHierarchies.get().isEmpty());

        final FileHierarchy fileHierarchy = fileHierarchies.get().get(0);

        assertEquals(FileStructureType.FILE, fileHierarchy.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchy.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchy.getName());

        TestTransaction.end();
    }

    @Test(expected = FileNotFoundException.class)
    public void findAllByPath_WhenPassInvalidFilePath_ThenThrowsException() throws FileNotFoundException {

        fileManagerService.findAllByPath(Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip"));
    }

    private ArchiveEvent createArchiveEvent(final String path) throws IOException {

        final Path keyPath = Paths.get(testFolder.newFile("keyPath").getAbsolutePath());

        return new ArchiveEvent(
                "user-ega-box-1130",
                "ega-box-1130",
                path,
                "/staging/path",
                26L,
                "3C130EA5D8D2D3DACA7F6808CDF0F148",
                42L,
                "3C130EA5D8D2D3DACA7F6808CDF0F149",
                keyPath.toString(), LocalDateTime.now(), LocalDateTime.now()
        );
    }
}
