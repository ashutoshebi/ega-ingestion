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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.ega.fire.ingestion.service.IFireService;
import uk.ac.ebi.ega.ingestion.commons.messages.ArchiveEvent;
import uk.ac.ebi.ega.ingestion.commons.messages.EncryptEvent;
import uk.ac.ebi.ega.ingestion.commons.messages.NewFileEvent;
import uk.ac.ebi.ega.ingestion.commons.models.Encryption;
import uk.ac.ebi.ega.ingestion.commons.services.IEncryptedKeyService;
import uk.ac.ebi.ega.ingestion.file.manager.controller.exceptions.FileHierarchyException;
import uk.ac.ebi.ega.ingestion.file.manager.models.FileHierarchyModel;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.EncryptedObject;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileStatus;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.EncryptedObjectRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.FileHierarchyRepository;
import uk.ac.ebi.ega.ingestion.file.manager.utils.FileStructureType;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    private IEncryptedKeyService encryptedKeyService;

    @MockBean
    private KafkaTemplate<String, EncryptEvent> kafkaTemplate;

    @MockBean
    private IFireService fireService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private FileHierarchyRepository fileHierarchyRepository;

    @Autowired
    private EncryptedObjectRepository encryptedObjectRepository;

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
        fileManagerService = Mockito.spy(new FileManagerService(fireService, Paths.get("/test/path"),
                fileHierarchyRepository, entityManager, encryptedObjectRepository, "encrypt-topic",
                kafkaTemplate, encryptedKeyService));
    }

    @Sql(scripts = "classpath:cleanDatabase.sql")
    @Test
    public void newFile_CreatesDataAndSendsMessageToQueue() {
        final NewFileEvent fileEvent = new NewFileEvent(
                "ega-test-account",
                "ega-test-staging",
                "test.pgp",
                new Date().getTime(),
                Paths.get("/test/test.pgp"),
                "250CF8B51C773F3F8DC8B4BE867A9A02",
                "270CF8B51C773F3F8DC8B4BE867A9B03",
                Encryption.PGP);
        fileManagerService.newFile("test-01", fileEvent);
        verify(kafkaTemplate).send(eq("encrypt-topic"), anyString(), argThat(arg -> {
            assertEquals(Paths.get("/test/test.pgp").toUri(), arg.getUri());
            assertNull(arg.getDecryptionKey());
            assertNull(arg.getEncryptionKey());
            assertEquals(Encryption.PGP, arg.getCurrentEncryption());
            assertEquals(Encryption.EGA_AES, arg.getNewEncryption());
            assertEquals("250CF8B51C773F3F8DC8B4BE867A9A02", arg.getPlainMd5());
            assertEquals("270CF8B51C773F3F8DC8B4BE867A9B03", arg.getEncryptedMd5());
            return true;
        }));
        assertTrue(encryptedObjectRepository.findByPathAndVersion("test.pgp", fileEvent.getLastModified()).isPresent());
    }

    @Sql(scripts = "classpath:cleanDatabase.sql")
    @Test
    public void newFile_DuplicateMessageSendsMessageToQueueTwiceIfItIsStillInProcess() {
        final NewFileEvent fileEvent = new NewFileEvent(
                "ega-test-account",
                "ega-test-staging",
                "test.pgp",
                new Date().getTime(),
                Paths.get("/test/test.pgp"),
                "250CF8B51C773F3F8DC8B4BE867A9A02",
                "270CF8B51C773F3F8DC8B4BE867A9B03",
                Encryption.PGP);
        fileManagerService.newFile("test-01", fileEvent);
        fileManagerService.newFile("test-01", fileEvent);

        verify(kafkaTemplate, times(2)).send(eq("encrypt-topic"), anyString(), argThat(arg -> {
            assertEquals(Paths.get("/test/test.pgp").toUri(), arg.getUri());
            assertNull(arg.getDecryptionKey());
            assertNull(arg.getEncryptionKey());
            assertEquals(Encryption.PGP, arg.getCurrentEncryption());
            assertEquals(Encryption.EGA_AES, arg.getNewEncryption());
            assertEquals("250CF8B51C773F3F8DC8B4BE867A9A02", arg.getPlainMd5());
            assertEquals("270CF8B51C773F3F8DC8B4BE867A9B03", arg.getEncryptedMd5());
            return true;
        }));

        assertTrue(encryptedObjectRepository.findByPathAndVersion("test.pgp", fileEvent.getLastModified()).isPresent());
    }

    @Sql(scripts = "classpath:cleanDatabase.sql")
    @Test
    public void newFile_DuplicateMessageDoesNotSendWhenTheObjectHasBeenProcessed() {
        final NewFileEvent fileEvent = new NewFileEvent(
                "ega-test-account",
                "ega-test-staging",
                "test.pgp",
                new Date().getTime(),
                Paths.get("/test/test.pgp"),
                "250CF8B51C773F3F8DC8B4BE867A9A02",
                "270CF8B51C773F3F8DC8B4BE867A9B03",
                Encryption.PGP);
        fileManagerService.newFile("test-01", fileEvent);

        final EncryptedObject object = encryptedObjectRepository.findByPathAndVersion("test.pgp",
                fileEvent.getLastModified()).get();
        object.setStatus(FileStatus.ARCHIVE_IN_PROGRESS);
        encryptedObjectRepository.save(object);

        fileManagerService.newFile("test-01", fileEvent);
        verify(kafkaTemplate, times(1)).send(eq("encrypt-topic"), anyString(), argThat(arg -> {
            assertEquals(Paths.get("/test/test.pgp").toUri(), arg.getUri());
            assertNull(arg.getDecryptionKey());
            assertNull(arg.getEncryptionKey());
            assertEquals(Encryption.PGP, arg.getCurrentEncryption());
            assertEquals(Encryption.EGA_AES, arg.getNewEncryption());
            assertEquals("250CF8B51C773F3F8DC8B4BE867A9A02", arg.getPlainMd5());
            assertEquals("270CF8B51C773F3F8DC8B4BE867A9B03", arg.getEncryptedMd5());
            return true;
        }));

        assertTrue(encryptedObjectRepository.findByPathAndVersion("test.pgp", fileEvent.getLastModified()).isPresent());
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

    @Test(expected = FileHierarchyException.class)
    public void archive_WhenPassEmptyPath_ThenThrowsFileHierarchyException() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent(""));
    }

    @Transactional
    @Test
    public void findAll_WhenPassValidFilePath_ThenReturnsFiles() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels = fileManagerService.findAllFilesAndFoldersInPathNonRecursive("user-ega-box-1130", "ega-box-1130",
                Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip"));

        assertNotNull(fileHierarchyModels);
        assertFalse(fileHierarchyModels.isEmpty());

        final FileHierarchyModel fileHierarchy = fileHierarchyModels.get(0);

        assertEquals(FileStructureType.FILE, fileHierarchy.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchy.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchy.getName());
        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAll_WhenPassValidFolderPath_ThenReturnsChildFile() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels = fileManagerService.findAllFilesAndFoldersInPathNonRecursive("user-ega-box-1130", "ega-box-1130",
                Paths.get("/nfs/ega/public"));

        assertNotNull(fileHierarchyModels);
        assertFalse(fileHierarchyModels.isEmpty());

        final FileHierarchyModel fileHierarchy = fileHierarchyModels.get(0);

        assertEquals(FileStructureType.FILE, fileHierarchy.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchy.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchy.getName());
        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAll_WhenPassValidFolderPath_ThenReturnsChildFolder() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels = fileManagerService.findAllFilesAndFoldersInPathNonRecursive("user-ega-box-1130", "ega-box-1130",
                Paths.get("/nfs/ega"));

        assertNotNull(fileHierarchyModels);
        assertFalse(fileHierarchyModels.isEmpty());

        final FileHierarchyModel fileHierarchy = fileHierarchyModels.get(0);

        assertEquals(FileStructureType.FOLDER, fileHierarchy.getFileType());
        assertEquals("/nfs/ega/public", fileHierarchy.getOriginalPath());
        assertEquals("public", fileHierarchy.getName());
        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAll_WhenArchivedWithOnlyFilenameAsPath_ThenReturnsFile() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels = fileManagerService.findAllFilesAndFoldersInPathNonRecursive("user-ega-box-1130", "ega-box-1130",
                Paths.get("/ega-box-01-012345677890.cip"));

        assertNotNull(fileHierarchyModels);
        assertFalse(fileHierarchyModels.isEmpty());

        final FileHierarchyModel fileHierarchy = fileHierarchyModels.get(0);

        assertEquals(FileStructureType.FILE, fileHierarchy.getFileType());
        assertEquals("/ega-box-01-012345677890.cip", fileHierarchy.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchy.getName());
        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAll_WhenArchivedWithBadFilePath_ThenReturnsFiles() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs//ega/public///ega-box-01-012345677890.cip/"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels = fileManagerService.findAllFilesAndFoldersInPathNonRecursive("user-ega-box-1130", "ega-box-1130",
                Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip"));

        assertNotNull(fileHierarchyModels);
        assertFalse(fileHierarchyModels.isEmpty());

        final FileHierarchyModel fileHierarchy = fileHierarchyModels.get(0);

        assertEquals(FileStructureType.FILE, fileHierarchy.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchy.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchy.getName());
        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAll_WhenRetrieveFilesWithBadFilePath_ThenReturnsFiles() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels = fileManagerService.findAllFilesAndFoldersInPathNonRecursive("user-ega-box-1130", "ega-box-1130",
                Paths.get("/nfs//ega/public///ega-box-01-012345677890.cip/"));

        assertNotNull(fileHierarchyModels);
        assertFalse(fileHierarchyModels.isEmpty());

        final FileHierarchyModel fileHierarchy = fileHierarchyModels.get(0);

        assertEquals(FileStructureType.FILE, fileHierarchy.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchy.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchy.getName());
        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAll_WhenRetrieveFilesWithCaseInsensitiveFilePath_ThenReturnsFiles() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels = fileManagerService.findAllFilesAndFoldersInPathNonRecursive("user-ega-box-1130", "ega-box-1130",
                Paths.get("/NFS/ega/PUBLIC/ega-BOX-01-012345677890.cIp"));

        assertNotNull(fileHierarchyModels);
        assertFalse(fileHierarchyModels.isEmpty());

        final FileHierarchyModel fileHierarchy = fileHierarchyModels.get(0);

        assertEquals(FileStructureType.FILE, fileHierarchy.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchy.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchy.getName());
        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAll_WhenRetrieveFilesWithCaseInsensitiveAccountIdAndStagingAreaId_ThenReturnsFiles() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels = fileManagerService.findAllFilesAndFoldersInPathNonRecursive("uSer-EGA-boX-1130", "EgA-bOx-1130",
                Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip"));

        assertNotNull(fileHierarchyModels);
        assertFalse(fileHierarchyModels.isEmpty());

        final FileHierarchyModel fileHierarchy = fileHierarchyModels.get(0);

        assertEquals(FileStructureType.FILE, fileHierarchy.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchy.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchy.getName());
        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAll_WhenNoFilePathPass_ThenReturnsFilesInRootPath() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        fileManagerService.archive(createArchiveEvent("/ega-box-02-074365477890_root.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels = fileManagerService.findAllFilesAndFoldersInPathNonRecursive("uSer-EGA-boX-1130", "EgA-bOx-1130",
                Paths.get(""));

        assertNotNull(fileHierarchyModels);
        assertFalse(fileHierarchyModels.isEmpty());
        assertEquals(2, fileHierarchyModels.size());

        final FileHierarchyModel fileHierarchyFirstValue = fileHierarchyModels.get(0);

        assertEquals("/ega-box-02-074365477890_root.cip", fileHierarchyFirstValue.getOriginalPath());
        assertEquals("ega-box-02-074365477890_root.cip", fileHierarchyFirstValue.getName());
        assertEquals(FileStructureType.FILE, fileHierarchyFirstValue.getFileType());

        final FileHierarchyModel fileHierarchySecondValue = fileHierarchyModels.get(1);

        assertEquals("/nfs", fileHierarchySecondValue.getOriginalPath());
        assertEquals("nfs", fileHierarchySecondValue.getName());
        assertEquals(FileStructureType.FOLDER, fileHierarchySecondValue.getFileType());
        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAll_WhenRetrieveFilesWithEmptyFolder_ThenReturnsEmptyList() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels = fileManagerService.findAllFilesAndFoldersInPathNonRecursive("uSer-EGA-boX-1130", "EgA-bOx-1130",
                Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip"));

        assertNotNull(fileHierarchyModels);
        assertFalse(fileHierarchyModels.isEmpty());

        final FileHierarchyModel fileHierarchy = fileHierarchyModels.get(0);

        assertEquals(FileStructureType.FILE, fileHierarchy.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchy.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchy.getName());
        TestTransaction.end();

        TestTransaction.start();
        TestTransaction.flagForCommit();
        fileHierarchyRepository.deleteById(fileHierarchy.getId());
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModelsAfterChildDeleted = fileManagerService.findAllFilesAndFoldersInPathNonRecursive("uSer-EGA-boX-1130", "EgA-bOx-1130",
                Paths.get("/nfs/ega/public"));

        assertNotNull(fileHierarchyModelsAfterChildDeleted);
        assertTrue(fileHierarchyModelsAfterChildDeleted.isEmpty());
        TestTransaction.end();
    }

    @Transactional
    @Test(expected = FileNotFoundException.class)
    public void findAll_WhenPassInvalidAccountIdAndLocationId_ThenThrowsException() throws IOException, FileHierarchyException {

        try {

            when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

            fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
            TestTransaction.flagForCommit();
            TestTransaction.end();

            TestTransaction.start();
            final List<FileHierarchyModel> fileHierarchyModels = fileManagerService.findAllFilesAndFoldersInPathNonRecursive("user-ega-box-1130", "ega-box-1130",
                    Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip"));

            assertNotNull(fileHierarchyModels);
            assertFalse(fileHierarchyModels.isEmpty());

            final FileHierarchyModel fileHierarchy = fileHierarchyModels.get(0);

            assertEquals(FileStructureType.FILE, fileHierarchy.getFileType());
            assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchy.getOriginalPath());
            assertEquals("ega-box-01-012345677890.cip", fileHierarchy.getName());

            fileManagerService.findAllFilesAndFoldersInPathNonRecursive("invalid_account_id", "invalid_staging_area_id",
                    Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip"));
        } finally {
            TestTransaction.end();
        }
    }

    @Test(expected = FileNotFoundException.class)
    public void findAll_WhenPassInvalidFilePath_ThenThrowsException() throws FileNotFoundException {
        fileManagerService.findAllFilesAndFoldersInPathNonRecursive("user-ega-box-1130", "ega-box-1130",
                Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip"));
    }

    @Transactional
    @Test
    public void findAllFiles_WhenFileExists_ThenReturnsFiles() throws IOException, FileHierarchyException {
        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-02-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final Pageable firstPageWithTwoElements = PageRequest.of(0, 2);
        final Page<FileHierarchyModel> fileHierarchyModelsPage = fileManagerService.findAllFilesInRootPathRecursive("user-ega-box-1130", "ega-box-1130",
                null, firstPageWithTwoElements);

        assertNotNull(fileHierarchyModelsPage);
        assertTrue(fileHierarchyModelsPage.hasContent());
        assertEquals(2, fileHierarchyModelsPage.getTotalElements());

        final List<FileHierarchyModel> fileHierarchyModels = fileHierarchyModelsPage.getContent();
        final FileHierarchyModel fileHierarchyModelElementOne = fileHierarchyModels.get(0);

        assertEquals(FileStructureType.FILE, fileHierarchyModelElementOne.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchyModelElementOne.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchyModelElementOne.getName());

        final FileHierarchyModel fileHierarchyModelElementTwo = fileHierarchyModels.get(1);

        assertEquals(FileStructureType.FILE, fileHierarchyModelElementTwo.getFileType());
        assertEquals("/nfs/ega/public/ega-box-02-012345677890.cip", fileHierarchyModelElementTwo.getOriginalPath());
        assertEquals("ega-box-02-012345677890.cip", fileHierarchyModelElementTwo.getName());
        TestTransaction.end();
    }

    @Test(expected = FileNotFoundException.class)
    public void findAllFiles_WhenFileDoesNotExists_ThenThrowsFileNotFoundException() throws FileNotFoundException {
        final Pageable firstPageWithTwoElements = PageRequest.of(0, 2);
        fileManagerService.findAllFilesInRootPathRecursive("user-ega-box-1130", "ega-box-1130", null,
                firstPageWithTwoElements);
    }

    @Transactional
    @Test
    public void findAllFiles_WhenPassValidFilePath_ThenReturnsFileAsStream() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final Stream<FileHierarchyModel> fileHierarchyModelStream = fileManagerService.findAllFilesInPathNonRecursive("user-ega-box-1130",
                "ega-box-1130", Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip"));
        assertNotNull(fileHierarchyModelStream);

        final Object[] fileHierarchyArray = fileHierarchyModelStream.toArray();
        assertEquals(1, fileHierarchyArray.length);

        final FileHierarchyModel fileHierarchyFirstValue = (FileHierarchyModel) fileHierarchyArray[0];
        assertEquals(FileStructureType.FILE, fileHierarchyFirstValue.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchyFirstValue.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchyFirstValue.getName());
        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAllFiles_WhenPassMixedCaseArguments_ThenReturnsFileAsStream() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final Stream<FileHierarchyModel> fileHierarchyModelStream = fileManagerService.findAllFilesInPathNonRecursive("User-EGA-box-1130",
                "Ega-BoX-1130", Paths.get("/nfs/EGA/PUBlIc/ega-bOx-01-012345677890.cip"));
        assertNotNull(fileHierarchyModelStream);

        final Object[] fileHierarchyArray = fileHierarchyModelStream.toArray();
        assertEquals(1, fileHierarchyArray.length);

        final FileHierarchyModel fileHierarchyFirstValue = (FileHierarchyModel) fileHierarchyArray[0];
        assertEquals(FileStructureType.FILE, fileHierarchyFirstValue.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchyFirstValue.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchyFirstValue.getName());
        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAllFiles_WhenPassValidFolderPath_ThenReturnsFilesUnderFolderAsStream() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012343547870.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final Stream<FileHierarchyModel> fileHierarchyModelStream = fileManagerService.findAllFilesInPathNonRecursive("user-ega-box-1130",
                "ega-box-1130", Paths.get("/nfs/ega/public"));
        assertNotNull(fileHierarchyModelStream);

        final Object[] fileHierarchyArray = fileHierarchyModelStream.toArray();
        assertEquals(2, fileHierarchyArray.length);

        final FileHierarchyModel fileHierarchyFirstValue = (FileHierarchyModel) fileHierarchyArray[0];
        assertEquals(FileStructureType.FILE, fileHierarchyFirstValue.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012343547870.cip", fileHierarchyFirstValue.getOriginalPath());
        assertEquals("ega-box-01-012343547870.cip", fileHierarchyFirstValue.getName());

        final FileHierarchyModel fileHierarchySecondValue = (FileHierarchyModel) fileHierarchyArray[1];
        assertEquals(FileStructureType.FILE, fileHierarchySecondValue.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchySecondValue.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchySecondValue.getName());
        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAllFiles_WhenPassAccountIdAndStagingAreaId_ThenReturnsAllFilesAsStream() throws IOException, FileHierarchyException {

        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012343547870.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final Stream<FileHierarchyModel> fileHierarchyModelStream = fileManagerService.findAllFilesInRootPathRecursive("user-ega-box-1130",
                "ega-box-1130");
        assertNotNull(fileHierarchyModelStream);

        final Object[] fileHierarchyArray = fileHierarchyModelStream.toArray();
        assertEquals(2, fileHierarchyArray.length);

        final FileHierarchyModel fileHierarchyFirstValue = (FileHierarchyModel) fileHierarchyArray[0];
        assertEquals(FileStructureType.FILE, fileHierarchyFirstValue.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012343547870.cip", fileHierarchyFirstValue.getOriginalPath());
        assertEquals("ega-box-01-012343547870.cip", fileHierarchyFirstValue.getName());

        final FileHierarchyModel fileHierarchySecondValue = (FileHierarchyModel) fileHierarchyArray[1];
        assertEquals(FileStructureType.FILE, fileHierarchySecondValue.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchySecondValue.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchySecondValue.getName());
        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAllFiles_WhenPassValidFolderPathWithNoFiles_ThenReturnsEmptyStream() throws IOException, FileHierarchyException {
        when(fireService.archiveFile(nullable(String.class), any(File.class), anyString(), anyString())).thenReturn(Optional.of(1L));

        fileManagerService.archive(createArchiveEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final Stream<FileHierarchyModel> fileHierarchyModelStream = fileManagerService.findAllFilesInPathNonRecursive("user-ega-box-1130",
                "ega-box-1130", Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip"));
        assertNotNull(fileHierarchyModelStream);

        final Object[] fileHierarchyArray = fileHierarchyModelStream.toArray();
        assertEquals(1, fileHierarchyArray.length);
        final FileHierarchyModel fileHierarchyModel = (FileHierarchyModel) fileHierarchyArray[0];
        TestTransaction.end();

        TestTransaction.start();
        TestTransaction.flagForCommit();
        fileHierarchyRepository.deleteById(fileHierarchyModel.getId());
        TestTransaction.end();

        TestTransaction.start();
        final Stream<FileHierarchyModel> fileHierarchyModelStreamAfterChildDeleted = fileManagerService.findAllFilesInPathNonRecursive("user-ega-box-1130",
                "ega-box-1130", Paths.get("/nfs/ega/public"));
        assertNotNull(fileHierarchyModelStreamAfterChildDeleted);
        assertEquals(0, fileHierarchyModelStreamAfterChildDeleted.count());
        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAllFiles_WhenNoFilesFoundUnderGivenPath_ThenReturnsEmptyStream() throws FileNotFoundException {
        final Stream<FileHierarchyModel> fileHierarchyModelStream = fileManagerService.findAllFilesInPathNonRecursive("user-ega-box-1130",
                "ega-box-1130", Paths.get(""));
        assertNotNull(fileHierarchyModelStream);
        assertEquals(0, fileHierarchyModelStream.count());
    }

    @Test(expected = FileNotFoundException.class)
    public void findAllFiles_WhenPassInvalidFilePath_ThenThrowsException() throws FileNotFoundException {
        fileManagerService.findAllFilesInPathNonRecursive("user-ega-box-1130",
                "ega-box-1130", Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip"));
    }

    private ArchiveEvent createArchiveEvent(final String path) throws IOException {

        final UUID uuid = UUID.randomUUID();
        final Path keyPath = Paths.get(testFolder.newFile("keyPath_" + uuid.toString()).getAbsolutePath());

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
