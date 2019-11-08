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
package uk.ac.ebi.ega.file.encryption.processor.service;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ResourceUtils;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;
import uk.ac.ebi.ega.encryption.core.services.PasswordEncryptionService;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.ingestion.commons.messages.EncryptEvent;
import uk.ac.ebi.ega.ingestion.commons.messages.FileEncryptionData;
import uk.ac.ebi.ega.ingestion.commons.messages.FileEncryptionResult;
import uk.ac.ebi.ega.ingestion.commons.models.Encryption;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource("classpath:application-test.properties")
@RunWith(SpringRunner.class)
public class GSFileEncryptionServiceTest {

    @Value("${password.encryption.key}")
    private String passwordEncryptionKey;

    @Value("${file.encryption.output.path}")
    private String outputFolderPath;

    @Value("${gs.credentials.file}")
    private String gsCredentials;

    /**
     * Test case data needs to be modified accordingly in future when it will be enabled.
     */
    @Ignore
    @Test
    public void encrypt_whenPassValidEventData_thenEncryptsFileDownloadedFromGS() throws IOException, URISyntaxException {
        final IPasswordEncryptionService passwordEncryptionService = new PasswordEncryptionService(FileUtils.readPasswordFile(Paths.get(passwordEncryptionKey)));
        final IFileEncryptionService gsEncryptionService = new GSFileEncryptionService(passwordEncryptionService, Paths.get(outputFolderPath));
        final FileEncryptionResult encryptionResult = gsEncryptionService.encrypt(initEncryptEvent());
        assertThat(encryptionResult).isEqualTo(expectedResult());
    }

    private EncryptEvent initEncryptEvent() throws URISyntaxException, IOException {
        final String NR = "NotRequired";
        final Path credentialsPath = ResourceUtils.getFile(gsCredentials).toPath();
        final String credentials = Files.readAllLines(credentialsPath).get(0);
        return new EncryptEvent(
                new URI("gs://{bucket-name}/{file-name}"),
                Encryption.PLAIN,
                NR,
                Encryption.EGA_AES,
                "encrypted-encryption-key",
                NR,
                "PlainMd5",
                credentials
        );
    }

    private FileEncryptionResult expectedResult() throws URISyntaxException {
        //Dummy values. Replace these values once actual testing data is available.
        return FileEncryptionResult.success(new FileEncryptionData(
                0,
                new URI("file://encrypted/file/path"),
                "add-encrypted-md5",
                0,
                "add-encryption-key",
                Encryption.EGA_AES));
    }
}
