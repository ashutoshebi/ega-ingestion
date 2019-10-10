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
package uk.ac.ebi.ega.file.encryption.processor.pipeline;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ResourceUtils;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.WrongPassword;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

@TestPropertySource("classpath:application-test.properties")
@RunWith(SpringRunner.class)
public class DefaultIngestionPipelineTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Value("${file.encryption.keyring.private}")
    private String secretRingPath;

    @Value("${file.encryption.keyring.private.key}")
    private String secretRingKeyPath;

    @Value("${file.encryption.output.path}")
    private String outputFolderPath;

    @Value("${file.to.encrypt.path}")
    private String fileToEncryptPath;

    @Value("${file.plain.md5.path}")
    private String originalPlainFileMD5Path;

    @Test
    public void process_WhenPassValidArguments_ThenSuccessfullyGeneratesEncryptedFile() throws Exception {

        final File origin = ResourceUtils.getFile(fileToEncryptPath);
        final File secretRing = ResourceUtils.getFile(secretRingPath);
        final File secretRingKey = ResourceUtils.getFile(secretRingKeyPath);
        final IngestionPipeline ingestionPipeline = initNewDefaultIngestionPipeline(origin, secretRing, secretRingKey);

        final List<String> originalFileMD5 = Files.readAllLines(ResourceUtils.getFile(originalPlainFileMD5Path).toPath());
        final IngestionPipelineResult ingestionPipelineResult = ingestionPipeline.process();

        assertNotNull(ingestionPipelineResult);
        assertNotNull(originalFileMD5);
        assertFalse(originalFileMD5.isEmpty());
        assertEquals(originalFileMD5.get(0), ingestionPipelineResult.getMd5());
        assertNotNull(ingestionPipelineResult.getKey());
        assertTrue(ingestionPipelineResult.getBytesTransferred() > 0);

        assertNotNull(ingestionPipelineResult.getEncryptedFile());
        assertNotNull(ingestionPipelineResult.getEncryptedFile().getMd5());
        assertNotNull(ingestionPipelineResult.getEncryptedFile().getFile());
        assertTrue(ingestionPipelineResult.getEncryptedFile().getFile().exists());
        assertTrue(ingestionPipelineResult.getEncryptedFile().getFileSize() > 0);

        assertNotNull(ingestionPipelineResult.getOriginalFile());
        assertNotNull(ingestionPipelineResult.getOriginalFile().getMd5());
        assertNotNull(ingestionPipelineResult.getOriginalFile().getFile());
        assertTrue(ingestionPipelineResult.getOriginalFile().getFile().exists());
    }

    @Test(expected = AlgorithmInitializationException.class)
    public void process_WhenPassInValidTypeOfFileToEncrypt_ThenThrowsException() throws Exception {
        //Pass any file other than .gpg
        final File origin = ResourceUtils.getFile(originalPlainFileMD5Path);
        final File secretRing = ResourceUtils.getFile(secretRingPath);
        final File secretRingKey = ResourceUtils.getFile(secretRingKeyPath);
        final IngestionPipeline ingestionPipeline = initNewDefaultIngestionPipeline(origin, secretRing, secretRingKey);
        ingestionPipeline.process();
    }

    @Test(expected = WrongPassword.class)
    public void process_WhenPassInValidPGPSecretKey_ThenThrowsException() throws Exception {
        final File wrongSecretRingKey = temporaryFolder.newFile("secretKeyRing");
        try (final OutputStream outputStream = new FileOutputStream(wrongSecretRingKey)) {
            outputStream.write("wrongPassPhrase".getBytes());
            outputStream.flush();
        }
        final File origin = ResourceUtils.getFile(fileToEncryptPath);
        final File secretRing = ResourceUtils.getFile(secretRingPath);
        //Pass wrong secret key
        final IngestionPipeline ingestionPipeline = initNewDefaultIngestionPipeline(origin, secretRing, wrongSecretRingKey);
        ingestionPipeline.process();
    }

    private IngestionPipeline initNewDefaultIngestionPipeline(final File origin, final File secretRing,
                                                              final File secretRingKey) throws IOException {
        final File outputFolder = temporaryFolder.newFolder(outputFolderPath);
        final File createdOutputFile = new File(outputFolder, UUID.randomUUID().toString());
        final char[] newEncryptionKey = "new_encryption_key_test".toCharArray();
        return new DefaultIngestionPipeline(origin, secretRing, secretRingKey, createdOutputFile, newEncryptionKey);
    }
}
