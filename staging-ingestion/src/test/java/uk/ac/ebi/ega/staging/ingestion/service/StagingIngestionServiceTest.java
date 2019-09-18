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
package uk.ac.ebi.ega.staging.ingestion.service;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.ega.ingestion.commons.messages.IngestionEvent;
import uk.ac.ebi.ega.ingestion.commons.messages.NewFileEvent;
import uk.ac.ebi.ega.ingestion.commons.models.Encryption;
import uk.ac.ebi.ega.ingestion.commons.models.FileStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class StagingIngestionServiceTest {

    public static final String RESOURCE_TEST_MD5 = "/test.md5";
    public static final String RESOURCE_TEST_GPG = "/test.gpg";
    public static final String RESOURCE_TEST_GPG_MD5 = "/test.gpg.md5";

    public static final String TEST_MD5 = "test.md5";
    public static final String TEST_GPG = "test.gpg";
    public static final String TEST_GPG_MD5 = "test.gpg.md5";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void cleanMd5FilesDeletesOnly() throws IOException {
        StagingIngestionService service = getService();
        copyResourceToTemp(RESOURCE_TEST_MD5, TEST_MD5);
        copyResourceToTemp(RESOURCE_TEST_GPG_MD5, TEST_GPG_MD5);
        copyResourceToTemp(RESOURCE_TEST_GPG, TEST_GPG);
        Path rootTestPath = temporaryFolder.getRoot().toPath();

        // All files present
        assertTestFileExists(TEST_GPG);
        assertTestFileExists(TEST_MD5);
        assertTestFileExists(TEST_GPG_MD5);

        service.cleanMd5Files(new IngestionEvent(
                "test-account-id",
                "test-location-id",
                rootTestPath,
                getTestFileStatic(TEST_GPG),
                getTestFileStatic(TEST_MD5),
                getTestFileStatic(TEST_GPG_MD5)
        ));
        assertTestFileDoesNotExist(TEST_MD5);
        assertTestFileDoesNotExist(TEST_GPG_MD5);
        assertTestFileExists(TEST_GPG);
    }

    private void assertTestFileExists(Path rootTestPath, String s) {
        assertTrue(rootTestPath.resolve(s).toFile().exists());
    }

    @Test
    public void filesPresentAndCorrectReturns() throws IOException {
        StagingIngestionService service = getService();
        copyResourceToTemp(RESOURCE_TEST_MD5, TEST_MD5);
        copyResourceToTemp(RESOURCE_TEST_GPG_MD5, TEST_GPG_MD5);
        copyResourceToTemp(RESOURCE_TEST_GPG, TEST_GPG);

        // All files present
        assertTestFileExists(TEST_GPG);
        assertTestFileExists(TEST_MD5);
        assertTestFileExists(TEST_GPG_MD5);

        Path rootTestPath = temporaryFolder.getRoot().toPath();
        final FileStatic gpgFile = getTestFileStatic(TEST_GPG);
        final Optional<NewFileEvent> newFileEventOptional = service.ingest("test-key",
                new IngestionEvent(
                        "test-account-id",
                        "test-location-id",
                        rootTestPath,
                        gpgFile,
                        getTestFileStatic(TEST_MD5),
                        getTestFileStatic(TEST_GPG_MD5)
                ));

        // Md5 are still present, GPG file has been moved to staging path with name test-key.timestamp
        assertTestFileExists(TEST_MD5);
        assertTestFileExists(TEST_GPG_MD5);
        assertTestFileDoesNotExist(TEST_GPG);
        assertStagingFileExists("test-key." + gpgFile.lastModified());

        assertTrue(newFileEventOptional.isPresent());
        final NewFileEvent newFileEvent = newFileEventOptional.get();
        assertEquals("test-account-id", newFileEvent.getAccountId());
        assertEquals("test-location-id", newFileEvent.getLocationId());
        assertEquals("test", newFileEvent.getUserPath());
        assertEquals(gpgFile.lastModified(), newFileEvent.getLastModified());
        assertEquals("91fe33afafbe2d57c865443f11bae7gg".toUpperCase(), newFileEvent.getEncryptedMd5());
        assertEquals("91fe33afafbe2d57c865443f11bae7ff".toUpperCase(), newFileEvent.getPlainMd5());
        assertEquals(Encryption.PGP, newFileEvent.getEncryption());
    }

    @Test
    public void filesNotPresentReturnsOptionalEmpty() throws IOException {
        StagingIngestionService service = getService();

        Path rootTestPath = temporaryFolder.getRoot().toPath();
        final Optional<NewFileEvent> newFileEventOptional = service.ingest("test-key",
                new IngestionEvent(
                        "test-account-id",
                        "test-location-id",
                        rootTestPath,
                        new FileStatic("doesnotexist", 0L, 0L),
                        new FileStatic("doesnotexist", 0L, 0L),
                        new FileStatic("doesnotexist", 0L, 0L)
                ));

        assertFalse(newFileEventOptional.isPresent());
    }

    @Test
    public void filesPresentButDifferentLastModifiedDateReturnsOptionalEmpty() throws IOException {
        StagingIngestionService service = getService();
        copyResourceToTemp(RESOURCE_TEST_MD5, TEST_MD5);
        copyResourceToTemp(RESOURCE_TEST_GPG_MD5, TEST_GPG_MD5);
        copyResourceToTemp(RESOURCE_TEST_GPG, TEST_GPG);

        // All files present
        assertTestFileExists(TEST_GPG);
        assertTestFileExists(TEST_MD5);
        assertTestFileExists(TEST_GPG_MD5);

        Path rootTestPath = temporaryFolder.getRoot().toPath();
        final FileStatic gpgFile = getTestFileStatic(TEST_GPG);
        final FileStatic md5File = getTestFileStatic(TEST_MD5);
        final FileStatic gpgMd5File = getTestFileStatic(TEST_GPG_MD5);
        assertFalse(service.ingest("test-key",
                new IngestionEvent(
                        "test-account-id",
                        "test-location-id",
                        rootTestPath,
                        gpgFile,
                        new FileStatic(md5File.getAbsolutePath(), md5File.length(), md5File.lastModified() - 1),
                        gpgMd5File
                )).isPresent());
        assertFalse(service.ingest("test-key",
                new IngestionEvent(
                        "test-account-id",
                        "test-location-id",
                        rootTestPath,
                        gpgFile,
                        md5File,
                        new FileStatic(gpgMd5File.getAbsolutePath(), gpgMd5File.length(), gpgMd5File.lastModified() - 1)
                )).isPresent());
        assertFalse(service.ingest("test-key",
                new IngestionEvent(
                        "test-account-id",
                        "test-location-id",
                        rootTestPath,
                        new FileStatic(gpgFile.getAbsolutePath(), gpgFile.length(), gpgFile.lastModified() - 1),
                        md5File,
                        gpgMd5File
                )).isPresent());
    }

    @Test
    public void md5FilesPresentPGPMissingReturnsOptionalEmpty() throws IOException {
        StagingIngestionService service = getService();
        copyResourceToTemp(RESOURCE_TEST_MD5, TEST_MD5);
        copyResourceToTemp(RESOURCE_TEST_GPG_MD5, TEST_GPG_MD5);

        // Md5 files present, pgp missing
        assertTestFileExists(TEST_MD5);
        assertTestFileExists(TEST_GPG_MD5);

        Path rootTestPath = temporaryFolder.getRoot().toPath();
        final FileStatic gpgFile = getTestFileStatic(TEST_GPG);
        assertFalse(service.ingest("test-key",
                new IngestionEvent(
                        "test-account-id",
                        "test-location-id",
                        rootTestPath,
                        new FileStatic("doesnotexist", 0L, 0L),
                        getTestFileStatic(TEST_MD5),
                        getTestFileStatic(TEST_GPG_MD5)
                )).isPresent());
    }

    @Test
    public void executeTwiceWillReturnValidNewFileEvent() throws IOException {
        StagingIngestionService service = getService();
        copyResourceToTemp(RESOURCE_TEST_MD5, TEST_MD5);
        copyResourceToTemp(RESOURCE_TEST_GPG_MD5, TEST_GPG_MD5);
        copyResourceToTemp(RESOURCE_TEST_GPG, TEST_GPG);

        // All files present
        assertTestFileExists(TEST_GPG);
        assertTestFileExists(TEST_MD5);
        assertTestFileExists(TEST_GPG_MD5);

        Path rootTestPath = temporaryFolder.getRoot().toPath();
        final FileStatic gpgFile = getTestFileStatic(TEST_GPG);
        assertTrue(service.ingest("test-key",
                new IngestionEvent(
                        "test-account-id",
                        "test-location-id",
                        rootTestPath,
                        gpgFile,
                        getTestFileStatic(TEST_MD5),
                        getTestFileStatic(TEST_GPG_MD5)
                )).isPresent());

        // Md5 are still present, GPG file has been moved to staging path with name test-key.timestamp
        assertTestFileExists(TEST_MD5);
        assertTestFileExists(TEST_GPG_MD5);
        assertTestFileDoesNotExist(TEST_GPG);
        assertStagingFileExists("test-key." + gpgFile.lastModified());

        final Optional<NewFileEvent> newFileEventOptional = service.ingest("test-key",
                new IngestionEvent(
                        "test-account-id",
                        "test-location-id",
                        rootTestPath,
                        gpgFile,
                        getTestFileStatic(TEST_MD5),
                        getTestFileStatic(TEST_GPG_MD5)
                ));

        assertTrue(newFileEventOptional.isPresent());
        final NewFileEvent newFileEvent = newFileEventOptional.get();
        assertEquals("test-account-id", newFileEvent.getAccountId());
        assertEquals("test-location-id", newFileEvent.getLocationId());
        assertEquals("test", newFileEvent.getUserPath());
        assertEquals(gpgFile.lastModified(), newFileEvent.getLastModified());
        assertEquals("91fe33afafbe2d57c865443f11bae7gg".toUpperCase(), newFileEvent.getEncryptedMd5());
        assertEquals("91fe33afafbe2d57c865443f11bae7ff".toUpperCase(), newFileEvent.getPlainMd5());
        assertEquals(Encryption.PGP, newFileEvent.getEncryption());
    }

    private void assertStagingFileExists(String name) {
        Path rootTestPath = temporaryFolder.getRoot().toPath();
        assertTrue(rootTestPath.resolve("staging").resolve(name).toFile().exists());
    }

    private void assertTestFileExists(String filename) {
        Path rootTestPath = temporaryFolder.getRoot().toPath();
        assertTrue(rootTestPath.resolve(filename).toFile().exists());
    }

    private void assertTestFileDoesNotExist(String filename) {
        Path rootTestPath = temporaryFolder.getRoot().toPath();
        assertFalse(rootTestPath.resolve(filename).toFile().exists());
    }

    private FileStatic getTestFileStatic(String fileName) {
        File file = temporaryFolder.getRoot().toPath().resolve(fileName).toFile();
        return new FileStatic(file.getName(), file.length(), file.lastModified());
    }

    private void copyResourceToTemp(String resource, String dstName) throws IOException {
        final File file = temporaryFolder.newFile(dstName);
        Files.copy(this.getClass().getResourceAsStream(resource), file.toPath(), REPLACE_EXISTING);

    }

    private StagingIngestionService getService() throws IOException {
        Path stagingFolder = temporaryFolder.newFolder("staging").toPath();
        return new StagingIngestionServiceImpl(stagingFolder);
    }

}
