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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;
import uk.ac.ebi.ega.fire.IFireService;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.entity.UkBiobankFileEntity;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankFilesRepository;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankReEncryptedFilesRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
    private IFireService fireService = mock(IFireService.class);

    private ReEncryptService reEncryptService;

    private Path outputFile;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        this.reEncryptService = new ReEncryptService(originalFilesRepository,
                reEncryptedFilesRepository, fireService);
        outputFile = temporaryFolder.newFile("temporaryOutputFile").toPath();
    }

    @Test
    public void reEncrypt_SuppliedCorrectArguments_ExecutesSuccessfully() {
        when(originalFilesRepository.findByFilePath(eq(INPUT_FILE.toString()))).thenReturn(getUkBiobankFileEntity());

        final Result result = reEncryptService.reEncrypt(INPUT_FILE, INPUT_PASSWORD, outputFile, OUTPUT_PASSWORD);

        assertThat(result.getStatus()).isEqualTo(Result.Status.SUCCESS);
        assertThatResultIsSavedIntoDatabase();
        assertThatOutputFileIsStoredInFire();
    }

    @Test
    public void reEncrypt_SuppliedWithNonExistingInputFile_ReturnsFailureResult() {
        final Result result = reEncryptService.reEncrypt(NONEXISTENT_INPUT_FILE, INPUT_PASSWORD, outputFile, OUTPUT_PASSWORD);

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

        final Result result = reEncryptService.reEncrypt(INPUT_FILE, INPUT_PASSWORD, outputFile, OUTPUT_PASSWORD);

        assertThat(result.getStatus()).isEqualTo(Result.Status.FAILURE);
        assertThat(result.getMessageAndException())
                .contains("Md5CheckException")
                .contains("Mismatch of md5");
        assertThatResultIsSavedIntoDatabase();
        assertThatOutputFileIsNotStoredInFire();
    }

    @Ignore
    @Test
    public void reEncrypt_WhatHappensIfSavingTheReEncryptionReportToTheDbFails() {
        // TODO bjuhasz
    }

    private void assertThatOutputFileIsStoredInFire() {
        // TODO bjuhasz
    }

    private void assertThatOutputFileIsNotStoredInFire() {
        // TODO bjuhasz
    }

    private void assertThatResultIsSavedIntoDatabase() {
        verify(reEncryptedFilesRepository, times(1)).save(any());
    }

    private static Path getPathFromResource(final String resourceName) {
        //return new File(this.getClass().getResource("/keyPairTest/test_file.txt.md5").getFile());
        return Paths.get(ReEncryptServiceTest.class.getResource(resourceName).getPath());
    }

    private Optional<UkBiobankFileEntity> getUkBiobankFileEntity() {
        return getUkBiobankFileEntity(MD5_OF_ORIGINAL_UNENCRYPTED_FILE);
    }

    private Optional<UkBiobankFileEntity> getUkBiobankFileEntity(final String md5) {
        return Optional.of(new UkBiobankFileEntity(INPUT_FILE.toString(), "", 0,
                md5, "", "", "", ""));
    }
}