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
import uk.ac.ebi.ega.ingestion.commons.messages.EncryptEvent;
import uk.ac.ebi.ega.ingestion.commons.messages.NewFileEvent;
import uk.ac.ebi.ega.ingestion.commons.models.Encryption;
import uk.ac.ebi.ega.ingestion.commons.models.IFileDetails;
import uk.ac.ebi.ega.ingestion.commons.services.IEncryptedKeyService;
import uk.ac.ebi.ega.ingestion.file.manager.controller.exceptions.FileHierarchyException;
import uk.ac.ebi.ega.ingestion.file.manager.models.FileHierarchyModel;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.EncryptedObject;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.EncryptedObjectRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.FileHierarchyRepository;
import uk.ac.ebi.ega.ingestion.file.manager.utils.FileStructureType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Sql(scripts = "classpath:cleanDatabase.sql")
@RunWith(SpringRunner.class)
@AutoConfigureDataJpa
@AutoConfigureTestDatabase
@AutoConfigureTestEntityManager
@TestPropertySource(locations = "classpath:test.properties")
public class FileManagerServiceTest {

    public static final String TEST_ACCOUNT = "ega-test-account";
    public static final String TEST_STAGING = "ega-test-staging";
    private IFileManagerService fileManagerService;

    @MockBean
    private IEncryptedKeyService encryptedKeyService;

    @MockBean
    private KafkaTemplate<String, EncryptEvent> kafkaTemplate;

    @MockBean
    private IFireService fireService;

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
                fileHierarchyRepository, encryptedObjectRepository, "encrypt-topic",
                kafkaTemplate, encryptedKeyService, "NUPD"));
    }

    @Sql(scripts = "classpath:cleanDatabase.sql")
    @Test
    public void newFile_CreatesDataAndSendsMessageToQueue() throws FileHierarchyException {
        final NewFileEvent fileEvent = createFileEvent("/test/test.pgp");
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
        assertTrue(encryptedObjectRepository.findByPathAndVersion("/test/test.pgp", fileEvent.getLastModified()).isPresent());
    }

    private NewFileEvent createFileEvent(String path) {
        return new NewFileEvent(
                TEST_ACCOUNT,
                TEST_STAGING,
                path,
                new Date().getTime(),
                Paths.get(path),
                "250CF8B51C773F3F8DC8B4BE867A9A02",
                "270CF8B51C773F3F8DC8B4BE867A9B03",
                Encryption.PGP);
    }

    @Sql(scripts = "classpath:cleanDatabase.sql")
    @Test
    public void newFile_sendTwoVersionsSameFile() throws FileHierarchyException, InterruptedException {
        final NewFileEvent event1 = createFileEvent("/test/test.pgp");
        TimeUnit.SECONDS.sleep(1l);
        final NewFileEvent event2 = createFileEvent("/test/test.pgp");
        fileManagerService.newFile("test-01", event1);
        fileManagerService.newFile("test-01", event2);
long count = encryptedObjectRepository.count();
        assertTrue(encryptedObjectRepository.findByPathAndVersion("/test/test.pgp", event1.getLastModified()).isPresent());
        assertTrue(encryptedObjectRepository.findByPathAndVersion("/test/test.pgp", event2.getLastModified()).isPresent());
        assertNotEquals(event1.getLastModified(), event2.getLastModified());

    }

    @Sql(scripts = "classpath:cleanDatabase.sql")
    @Test
    public void newFile_DuplicateMessageSendsMessageToQueueTwiceIfItIsStillInProcess() throws FileHierarchyException {
        final NewFileEvent fileEvent = createFileEvent("/test/test.pgp");
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

        assertTrue(encryptedObjectRepository.findByPathAndVersion("/test/test.pgp", fileEvent.getLastModified()).isPresent());
    }

    @Sql(scripts = "classpath:cleanDatabase.sql")
    @Test
    public void newFile_DuplicateMessageDoesNotSendWhenTheObjectHasBeenProcessed() throws FileHierarchyException {
        final NewFileEvent fileEvent = createFileEvent("/test/test.pgp");
        fileManagerService.newFile("test-01", fileEvent);

        final EncryptedObject object = encryptedObjectRepository.findByPathAndVersion("/test/test.pgp",
                fileEvent.getLastModified()).get();
        object.archive("file://temp/test.pgp", 0L, "270CF8B51C773F3F8DC8B4BE867A9B03", 0L, 0L,
                Encryption.EGA_AES, "test");
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

        assertTrue(encryptedObjectRepository.findByPathAndVersion("/test/test.pgp",
                fileEvent.getLastModified()).isPresent());
    }

    @Transactional
    @Test
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void findAll_WhenPassValidFolderPath_ThenReturnsChildFile() throws IOException, FileHierarchyException {
        fileManagerService.newFile("test-01", createFileEvent("/test/test.pgp"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels =
                fileManagerService.findAllFilesAndFoldersInPath(TEST_ACCOUNT, TEST_STAGING,
                        Optional.of(Paths.get("/test")));

        assertNotNull(fileHierarchyModels);
        assertFalse(fileHierarchyModels.isEmpty());

        final FileHierarchyModel fileHierarchy = fileHierarchyModels.get(0);

        assertEquals(FileStructureType.FILE, fileHierarchy.getFileType());
        assertEquals("/test/test.pgp", fileHierarchy.getOriginalPath());
        assertEquals("test.pgp", fileHierarchy.getName());
        TestTransaction.end();
    }

    @Transactional
    @Test
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void findAll_WhenPassValidFolderPath_ThenReturnsChildFolder() throws IOException, FileHierarchyException {
        fileManagerService.newFile("test-01", createFileEvent("/nfs/ega/public/test.pgp"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels = fileManagerService.findAllFilesAndFoldersInPath(
                TEST_ACCOUNT, TEST_STAGING, Optional.of(Paths.get("/nfs/ega")));

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
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void findAll_WhenArchivedWithOnlyFilenameAsPath_ThenReturnsFile() throws IOException, FileHierarchyException {
        fileManagerService.newFile("test-01", createFileEvent("ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels =
                fileManagerService.findAllFilesAndFoldersInPath(TEST_ACCOUNT, TEST_STAGING,
                        Optional.of(Paths.get("/ega-box-01-012345677890.cip")));

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
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void findAll_WhenArchivedWithBadFilePath_ThenReturnsFiles() throws IOException, FileHierarchyException {
        fileManagerService.newFile("test-01", createFileEvent("/nfs//ega/public///ega-box-01-012345677890.cip/"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels =
                fileManagerService.findAllFilesAndFoldersInPath(TEST_ACCOUNT, TEST_STAGING,
                        Optional.of(Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip")));

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
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void findAll_WhenRetrieveFilesWithBadFilePath_ThenReturnsFiles() throws IOException, FileHierarchyException {
        fileManagerService.newFile("test-01", createFileEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels =
                fileManagerService.findAllFilesAndFoldersInPath(TEST_ACCOUNT, TEST_STAGING,
                        Optional.of(Paths.get("/nfs//ega/public///ega-box-01-012345677890.cip/")));

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
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void findAll_WhenNoFilePathPass_ThenReturnsFilesInRootPath() throws IOException, FileHierarchyException {
        fileManagerService.newFile("test-01", createFileEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        fileManagerService.newFile("test-02", createFileEvent("/ega-box-02-074365477890_root.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels =
                fileManagerService.findAllFilesAndFoldersInPath(TEST_ACCOUNT, TEST_STAGING, Optional.empty());

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
    @Test(expected = FileNotFoundException.class)
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void findAll_WhenPassInvalidAccountIdAndLocationId_ThenThrowsException() throws IOException, FileHierarchyException {
        fileManagerService.newFile("test-01", createFileEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final List<FileHierarchyModel> fileHierarchyModels =
                fileManagerService.findAllFilesAndFoldersInPath(TEST_ACCOUNT, TEST_STAGING,
                        Optional.of(Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip")));

        assertNotNull(fileHierarchyModels);
        assertFalse(fileHierarchyModels.isEmpty());

        final FileHierarchyModel fileHierarchy = fileHierarchyModels.get(0);

        assertEquals(FileStructureType.FILE, fileHierarchy.getFileType());
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchy.getOriginalPath());
        assertEquals("ega-box-01-012345677890.cip", fileHierarchy.getName());

        fileManagerService.findAllFilesAndFoldersInPath("invalid", "invalid",
                Optional.of(Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip")));
    }

    @Test(expected = FileNotFoundException.class)
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void findAll_WhenPassInvalidFilePath_ThenThrowsException() throws FileNotFoundException, FileHierarchyException {
        fileManagerService.newFile("test-01", createFileEvent("/nfs/ega/public/ega-box-01-012345677889.cip"));
        fileManagerService.findAllFilesAndFoldersInPath(TEST_ACCOUNT, TEST_STAGING,
                Optional.of(Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip")));
    }

    @Test
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void findAllFiles_WhenFileExists_ThenReturnsFiles() throws FileHierarchyException {
        fileManagerService.newFile("test-01", createFileEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        fileManagerService.newFile("test-02", createFileEvent("/nfs/ega/public/ega-box-02-012345677890.cip"));

        final Pageable page = PageRequest.of(0, 2);
        final Page<? extends IFileDetails> fileHierarchyModelsPage =
                fileManagerService.findAllFiles(TEST_ACCOUNT, TEST_STAGING, null, page);

        assertNotNull(fileHierarchyModelsPage);
        assertTrue(fileHierarchyModelsPage.hasContent());
        assertEquals(2, fileHierarchyModelsPage.getTotalElements());

        final List<? extends IFileDetails> fileHierarchyModels = fileHierarchyModelsPage.getContent();
        final IFileDetails fileHierarchyModelElementOne = fileHierarchyModels.get(0);

        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchyModelElementOne.getPath());
        final IFileDetails fileHierarchyModelElementTwo = fileHierarchyModels.get(1);
        assertEquals("/nfs/ega/public/ega-box-02-012345677890.cip", fileHierarchyModelElementTwo.getPath());
    }

    @Transactional
    @Test
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void findAllFiles_WhenPassValidFilePath_ThenReturnsFileAsStream() throws IOException, FileHierarchyException {
        fileManagerService.newFile("test-01", createFileEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final Stream<? extends IFileDetails> fileHierarchyModelStream = fileManagerService.findAllFiles(TEST_ACCOUNT,
                TEST_STAGING, Optional.of("/nfs/ega/public/ega-box-01-012345677890.cip"));
        assertNotNull(fileHierarchyModelStream);

        final Object[] fileHierarchyArray = fileHierarchyModelStream.toArray();
        assertEquals(1, fileHierarchyArray.length);

        final IFileDetails fileHierarchyFirstValue = (IFileDetails) fileHierarchyArray[0];
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchyFirstValue.getPath());
        TestTransaction.end();
    }

    @Transactional
    @Test
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void findAllFiles_WhenPassValidFolderPath_ThenReturnsFilesUnderFolderAsStream() throws IOException, FileHierarchyException {
        fileManagerService.newFile("test-01", createFileEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        fileManagerService.newFile("test-02", createFileEvent("/nfs/ega/public/ega-box-01-012343547870.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final Stream<? extends IFileDetails> stream = fileManagerService.findAllFiles(TEST_ACCOUNT,
                TEST_STAGING, Optional.of("/nfs/ega/public"));
        assertNotNull(stream);

        final Object[] fileHierarchyArray = stream.toArray();
        assertEquals(2, fileHierarchyArray.length);

        final IFileDetails fileHierarchyFirstValue = (IFileDetails) fileHierarchyArray[0];
        assertEquals("/nfs/ega/public/ega-box-01-012343547870.cip", fileHierarchyFirstValue.getPath());

        final IFileDetails fileHierarchySecondValue = (IFileDetails) fileHierarchyArray[1];
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", fileHierarchySecondValue.getPath());
        TestTransaction.end();
    }

    @Transactional
    @Test
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void findAllFiles_WhenPassAccountIdAndStagingAreaId_ThenReturnsAllFilesAsStream() throws FileHierarchyException {
        fileManagerService.newFile("test-01", createFileEvent("/nfs/ega/public/ega-box-01-012345677890.cip"));
        fileManagerService.newFile("test-02", createFileEvent("/nfs/ega/public/ega-box-01-012343547870.cip"));
        TestTransaction.flagForCommit();
        TestTransaction.end();

        TestTransaction.start();
        final Stream<? extends IFileDetails> stream = fileManagerService.findAllFiles(TEST_ACCOUNT,
                TEST_STAGING, Optional.empty());
        assertNotNull(stream);

        final Object[] fileHierarchyArray = stream.toArray();
        assertEquals(2, fileHierarchyArray.length);

        final IFileDetails firstValue = (IFileDetails) fileHierarchyArray[0];
        assertEquals("/nfs/ega/public/ega-box-01-012343547870.cip", firstValue.getPath());

        final IFileDetails secondValue = (IFileDetails) fileHierarchyArray[1];
        assertEquals("/nfs/ega/public/ega-box-01-012345677890.cip", secondValue.getPath());
        TestTransaction.end();
    }

    @Transactional
    @Test
    public void findAllFiles_WhenNoFilesFoundUnderGivenPath_ThenReturnsEmptyStream() {
        final Stream<? extends IFileDetails> stream = fileManagerService.findAllFiles(TEST_ACCOUNT,
                TEST_STAGING, Optional.empty());
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test(expected = FileNotFoundException.class)
    @Sql(scripts = "classpath:cleanDatabase.sql")
    public void findAllFiles_WhenPassInvalidFilePath_ThenThrowsException() throws FileNotFoundException {
        fileManagerService.findAllFilesAndFoldersInPath(TEST_ACCOUNT, TEST_STAGING,
                Optional.of(Paths.get("/nfs/ega/public/ega-box-01-012345677890.cip")));
    }

}
