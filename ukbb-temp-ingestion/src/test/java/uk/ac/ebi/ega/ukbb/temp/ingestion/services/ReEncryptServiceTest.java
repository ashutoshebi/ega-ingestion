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
package uk.ac.ebi.ega.ukbb.temp.ingestion.services;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.dao.DataRetrievalFailureException;
import uk.ac.ebi.ega.encryption.core.DecryptInputStream;
import uk.ac.ebi.ega.encryption.core.encryption.AesCtr256Ega;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.entity.UkBiobankFileEntity;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.entity.UkBiobankReEncryptedFileEntity;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankFilesRepository;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankReEncryptedFilesRepository;
import uk.ac.ebi.ega.ukbb.temp.ingestion.properties.ReEncryptProperties;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReEncryptServiceTest {

    private static final Path INPUT_FILE = getPathFromResource("/aes256cbc-base64-encoded.txt");
    private static final String INPUT_PASSWORD = "kiwi";
    private static final String OUTPUT_PASSWORD = INPUT_PASSWORD;
    private static final Path NONEXISTENT_INPUT_FILE = Paths.get("this file does not exist");
    private static final String MD5_OF_ORIGINAL_UNENCRYPTED_FILE = "edc715389af2498a623134608ba0a55b";

    private UkBiobankFilesRepository originalFilesRepository = mock(UkBiobankFilesRepository.class);
    private UkBiobankReEncryptedFilesRepository reEncryptedFilesRepository = mock(UkBiobankReEncryptedFilesRepository.class);
    private ProFilerService proFilerService = mock(ProFilerService.class);

    private ReEncryptService reEncryptService;
    private Path outputFile;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        this.reEncryptService = new ReEncryptService(originalFilesRepository,
                reEncryptedFilesRepository, proFilerService, new ReEncryptProperties());
        outputFile = temporaryFolder.newFile("temporaryOutputFile").toPath();
    }

    @Test
    public void reEncrypt_SuppliedCorrectArguments_ExecutesSuccessfully() {
        when(originalFilesRepository.findByFilePath(eq(INPUT_FILE.toString()))).thenReturn(getUkBiobankFileEntity());

        final Result result = reEncryptService.reEncryptAndStoreInProFiler(INPUT_FILE, INPUT_PASSWORD, outputFile, outputFile, OUTPUT_PASSWORD);

        assertThat(result.getStatus()).isEqualTo(Result.Status.SUCCESS);
        assertThatResultIsSavedIntoDatabase();
        assertThatOutputFileIsStoredInFire();
    }

    @Test
    public void reEncrypt_SuppliedWithNonExistingInputFile_ReturnsFailureResult() {
        final Result result = reEncryptService.reEncryptAndStoreInProFiler(NONEXISTENT_INPUT_FILE, INPUT_PASSWORD, outputFile, outputFile, OUTPUT_PASSWORD);

        assertThat(result.getStatus()).isEqualTo(Result.Status.FAILURE);
        assertThat(result.getMessageAndException())
                .contains("FileNotFoundException")
                .contains("File could not be found on DOS");
        assertThatResultIsSavedIntoDatabase();
        assertThatOutputFileIsNotStoredInFire();
    }

    @Test
    public void reEncrypt_FailureDuringMD5Calculation_ReturnsFailureResult() {
        final String anMD5FromTheDBWhichWillNotMatchTheOneCalculatedByOurService = "abc";
        when(originalFilesRepository.findByFilePath(eq(INPUT_FILE.toString()))).thenReturn(
                getUkBiobankFileEntity(anMD5FromTheDBWhichWillNotMatchTheOneCalculatedByOurService));

        final Result result = reEncryptService.reEncryptAndStoreInProFiler(INPUT_FILE, INPUT_PASSWORD, outputFile, outputFile, OUTPUT_PASSWORD);

        assertThat(result.getStatus()).isEqualTo(Result.Status.FAILURE);
        assertThat(result.getMessageAndException())
                .contains("Md5CheckException")
                .contains("Mismatch of md5");
        assertThatResultIsSavedIntoDatabase();
        assertThatOutputFileIsNotStoredInFire();
    }

    @Test
    public void reEncrypt_WhenTheReEncryptionReportCannotBeSavedToTheDb_ThenTheUserIsStillInformedAboutTheFailure() {
        when(reEncryptedFilesRepository.save(any())).thenThrow(new DataRetrievalFailureException("example exception from a test"));
        when(originalFilesRepository.findByFilePath(eq(INPUT_FILE.toString()))).thenReturn(getUkBiobankFileEntity());

        final Result result = reEncryptService.reEncryptAndStoreInProFiler(INPUT_FILE, INPUT_PASSWORD, outputFile, outputFile, OUTPUT_PASSWORD);

        // If there's a DB-exception, then the user is still informed, she still receives the Result object:
        assertThat(result.getStatus()).isEqualTo(Result.Status.FAILURE);
        assertThat(result.getMessageAndException())
                .contains("DataRetrievalFailureException")
                .contains("Error while saving the result to the DB");
    }

    @Test
    public void reEncrypt_IfAnExceptionOccurs_ThenFileIsNotStoredInFire() {
        when(originalFilesRepository.findByFilePath(any())).thenThrow(new RuntimeException("example exception from a test"));

        final Result result = reEncryptService.reEncryptAndStoreInProFiler(INPUT_FILE, INPUT_PASSWORD, outputFile, outputFile, OUTPUT_PASSWORD);

        // If there's an exception, then the user is still informed, she still receives the Result object:
        assertThat(result.getStatus()).isEqualTo(Result.Status.ABORTED);
        assertThat(result.getMessageAndException())
                .contains("RuntimeException")
                .contains("Generic error");
        assertThatOutputFileIsNotStoredInFire();
    }

    @Test
    public void testThatTheReEncryptedFileCanBeDecrypted() throws FileNotFoundException, AlgorithmInitializationException {
        when(originalFilesRepository.findByFilePath(eq(INPUT_FILE.toString()))).thenReturn(getUkBiobankFileEntity());

        final Result result = reEncryptService.reEncryptAndStoreInProFiler(INPUT_FILE, INPUT_PASSWORD, outputFile, outputFile, OUTPUT_PASSWORD);
        assertThat(result.getStatus()).isEqualTo(Result.Status.SUCCESS);

        final String fileAsString = decryptFile(outputFile, OUTPUT_PASSWORD);
        assertThat(fileAsString).startsWith("Lorem ipsum");
    }

    private void assertThatOutputFileIsStoredInFire() {
        verify(proFilerService, times(1)).insertFile(any(), any(), anyString());
        verify(proFilerService, times(1)).insertArchive(any(), anyString(), any(), anyString());
    }

    private void assertThatOutputFileIsNotStoredInFire() {
        verify(proFilerService, never()).insertFile(anyString(), any(), anyString());
        verify(proFilerService, never()).insertArchive(any(), anyString(), any(), anyString());
    }

    private void assertThatResultIsSavedIntoDatabase() {
        verify(reEncryptedFilesRepository, times(1))
                .save(any(UkBiobankReEncryptedFileEntity.class));
    }

    private static Path getPathFromResource(final String resourceName) {
        return Paths.get(ReEncryptServiceTest.class.getResource(resourceName).getPath());
    }

    private Optional<UkBiobankFileEntity> getUkBiobankFileEntity() {
        return getUkBiobankFileEntity(MD5_OF_ORIGINAL_UNENCRYPTED_FILE);
    }

    private Optional<UkBiobankFileEntity> getUkBiobankFileEntity(final String md5) {
        return Optional.of(new UkBiobankFileEntity(INPUT_FILE.toString(), "", 0,
                md5, "", "", "", ""));
    }

    private String decryptFile(final Path encryptedFile, final String password)
            throws FileNotFoundException, AlgorithmInitializationException {
        final InputStream encryptedStream = new FileInputStream(encryptedFile.toFile());
        final DecryptInputStream decryptedStream = new DecryptInputStream(encryptedStream,
                new AesCtr256Ega(), password.toCharArray());
        return new BufferedReader(new InputStreamReader(decryptedStream))
                .lines().collect(Collectors.joining("\n"));
    }
}