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
import org.junit.Test;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.entity.UkBiobankFileEntity;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankFilesRepository;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReEncryptServiceTest {

    private static final Path INPUT_FILE_PATH = getPathFromResource("/aes256cbc-base64-encoded.txt");
    private static final String INPUT_PASSWORD = "kiwi";
    private static final Path OUTPUT_FILE_PATH = Paths.get("/tmp/output23.txt"); // TODO bjuhasz
    private static final String OUTPUT_PASSWORD = INPUT_PASSWORD;
    private static final String MD5_OF_ORIGINAL_UNENCRYPTED_FILE = "edc715389af2498a623134608ba0a55b";

    private UkBiobankFilesRepository filesRepository = mock(UkBiobankFilesRepository.class);

    private ReEncryptService reEncryptService;

    @Before
    public void setUp() {
        this.reEncryptService = new ReEncryptService(filesRepository);
    }

    @Test
    public void reEncrypt() {
        when(filesRepository.findByFilePath(eq(INPUT_FILE_PATH.toString()))).thenReturn(getUkBiobankFileEntity());

        final Result result = reEncryptService.reEncrypt(INPUT_FILE_PATH, INPUT_PASSWORD, OUTPUT_FILE_PATH, OUTPUT_PASSWORD);

        assertThat(result.getStatus()).isEqualTo(Result.Status.SUCCESS);
    }

    private static Path getPathFromResource(final String resourceName) {
        //return new File(this.getClass().getResource("/keyPairTest/test_file.txt.md5").getFile());
        return Paths.get(ReEncryptServiceTest.class.getResource(resourceName).getPath());
    }

    private Optional<UkBiobankFileEntity> getUkBiobankFileEntity() {
        return Optional.of(new UkBiobankFileEntity(INPUT_FILE_PATH.toString(), "", 0,
                MD5_OF_ORIGINAL_UNENCRYPTED_FILE, "", "", "", ""));
    }
}