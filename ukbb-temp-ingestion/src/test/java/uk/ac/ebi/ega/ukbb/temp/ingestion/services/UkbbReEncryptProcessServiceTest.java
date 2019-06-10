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
import org.mockito.AdditionalAnswers;
import uk.ac.ebi.ega.ukbb.temp.ingestion.exceptions.TerminateProgramException;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.entity.UkBiobankFileEntity;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.entity.UkBiobankReEncryptedFileEntity;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankFilesRepository;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankReEncryptedFilesRepository;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.services.JpaUkbbJobService;
import uk.ac.ebi.ega.ukbb.temp.ingestion.properties.ReEncryptProperties;
import uk.ac.ebi.ega.ukbb.temp.ingestion.reencryption.BaseReEncryptService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UkbbReEncryptProcessServiceTest {

    private static final Path INPUT_FILE = getPathFromResource("/aes256cbc-base64-encoded.txt");
    private static final Path SRC_KEY_FILE_PATH = getPathFromResource("/password.txt");
    private static final Path DST_KEY_FILE_PATH = SRC_KEY_FILE_PATH;
    private static final Path NONEXISTENT_INPUT_FILE_PATH = Paths.get("this file does not exist");
    private static final String MD5_OF_ORIGINAL_UNENCRYPTED_FILE = "edc715389af2498a623134608ba0a55b";

    private final IReEncryptService reEncryptService = new BaseReEncryptService();
    private final UkBiobankFilesRepository originalFilesRepository = mock(UkBiobankFilesRepository.class);
    private final UkBiobankReEncryptedFilesRepository reEncryptedFilesRepository = mock(UkBiobankReEncryptedFilesRepository.class);
    private final IUkbbJobService ukbbJobService = new JpaUkbbJobService(originalFilesRepository, reEncryptedFilesRepository);
    private final ProFilerService proFilerService = mock(ProFilerService.class);

    private IUkbbReEncryptProcessService ukbbReEncryptProcessService;
    private ReEncryptProperties reEncryptProperties;
    private Path inputFilePath;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        inputFilePath = temporaryFolder.newFile().toPath();
        Files.copy(INPUT_FILE, inputFilePath, StandardCopyOption.REPLACE_EXISTING);

        reEncryptProperties = getReEncryptProperties();

        ukbbReEncryptProcessService = new UkbbReEncryptProcessService(reEncryptService, ukbbJobService,
                proFilerService, reEncryptProperties);

        when(reEncryptedFilesRepository.save(any())).thenAnswer(AdditionalAnswers.returnsFirstArg());

        when(reEncryptedFilesRepository.findByOriginalFilePath(anyString()))
                .thenReturn(Optional.of(getUkBiobankReEncryptedFileEntity()));
    }

    @Test
    public void reEncrypt_SuppliedCorrectArguments_ExecutesSuccessfully() throws TerminateProgramException {
        when(originalFilesRepository.findByFilePath(eq(inputFilePath.toString()))).thenReturn(getUkBiobankFileEntity());

        ukbbReEncryptProcessService.reEncrypt(SRC_KEY_FILE_PATH, DST_KEY_FILE_PATH, inputFilePath);

        assertThatResultIsSavedIntoDatabase();
        assertThatOutputFileIsStoredInFire();
    }

    @Test
    public void reEncrypt_WhenInstructedToNotToSaveIntoFire_ThenItDoesNotSaveIntoFire() throws TerminateProgramException {
        when(originalFilesRepository.findByFilePath(eq(inputFilePath.toString()))).thenReturn(getUkBiobankFileEntity());
        reEncryptProperties.setStoreFileInFire(false);

        ukbbReEncryptProcessService.reEncrypt(SRC_KEY_FILE_PATH, DST_KEY_FILE_PATH, inputFilePath);

        assertThatResultIsSavedIntoDatabase();
        assertThatOutputFileIsNotStoredInFire();
    }

    @Test
    public void reEncrypt_SuppliedWithNonExistingInputFile_ReturnsFailureResult() {
        when(originalFilesRepository.findByFilePath(anyString())).thenReturn(getUkBiobankFileEntity());

        try {
            ukbbReEncryptProcessService.reEncrypt(SRC_KEY_FILE_PATH, DST_KEY_FILE_PATH, NONEXISTENT_INPUT_FILE_PATH);
        } catch (TerminateProgramException e) {
            assertThat(e.getTermCode()).isEqualTo(1);
            assertThat(e.getMessage()).endsWith("could not be read.");
        }

        assertThatResultIsSavedIntoDatabase();
        assertThatOutputFileIsNotStoredInFire();
    }

    @Test
    public void reEncrypt_FailureDuringMD5Calculation_ReturnsFailureResult() {
        final String anMD5FromTheDBWhichWillNotMatchTheOneCalculatedByOurService = "abc";
        when(originalFilesRepository.findByFilePath(eq(inputFilePath.toString()))).thenReturn(
                getUkBiobankFileEntity(anMD5FromTheDBWhichWillNotMatchTheOneCalculatedByOurService));

        try {
            ukbbReEncryptProcessService.reEncrypt(SRC_KEY_FILE_PATH, DST_KEY_FILE_PATH, inputFilePath);
        } catch (TerminateProgramException e) {
            assertThat(e.getTermCode()).isEqualTo(3);
            assertThat(e.getMessage()).contains("Checksum mismatch");
        }

        assertThatResultIsSavedIntoDatabase();
        assertThatOutputFileIsNotStoredInFire();
    }

    private void assertThatOutputFileIsStoredInFire() {
        verify(proFilerService, times(1)).insertFile(any(), any(), anyString());
        verify(proFilerService, times(1)).insertArchive(anyLong(), anyString(), any(), anyString());
    }

    private void assertThatOutputFileIsNotStoredInFire() {
        verify(proFilerService, never()).insertFile(anyString(), any(), anyString());
        verify(proFilerService, never()).insertArchive(any(), anyString(), any(), anyString());
    }

    private void assertThatResultIsSavedIntoDatabase() {
        verify(reEncryptedFilesRepository, times(2)).save(any(UkBiobankReEncryptedFileEntity.class));
    }

    private static Path getPathFromResource(final String resourceName) {
        return Paths.get(UkbbReEncryptProcessServiceTest.class.getResource(resourceName).getPath());
    }

    private Optional<UkBiobankFileEntity> getUkBiobankFileEntity() {
        return getUkBiobankFileEntity(MD5_OF_ORIGINAL_UNENCRYPTED_FILE);
    }

    private Optional<UkBiobankFileEntity> getUkBiobankFileEntity(final String md5) {
        return Optional.of(new UkBiobankFileEntity(inputFilePath.toString(), "", 0,
                md5, "", "", "", ""));
    }

    private UkBiobankReEncryptedFileEntity getUkBiobankReEncryptedFileEntity() {
        return new UkBiobankReEncryptedFileEntity(inputFilePath);
    }

    private ReEncryptProperties getReEncryptProperties() throws IOException {
        final ReEncryptProperties reEncryptProperties = new ReEncryptProperties();

        reEncryptProperties.setUkbbPath(inputFilePath.getRoot().toString());
        reEncryptProperties.setStagingPath(temporaryFolder.newFolder().toString());
        reEncryptProperties.setRelativePathInsideStaging("relativePathInsideStaging");

        reEncryptProperties.setStoreFileInFire(true);

        return reEncryptProperties;
    }

}