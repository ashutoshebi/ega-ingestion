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
package uk.ac.ebi.ega.file.encryption.processor.pipelines;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.UserErrorException;
import uk.ac.ebi.ega.file.encryption.processor.services.PipelineService;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DefaultIngestionPipelineTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testPipelineBuilderBasic() throws URISyntaxException, IOException, UserErrorException,
            SystemErrorException {
        File fileInStaging = copyToTemporaryFolder("/keyPairTest/test_file.txt.gpg");
        File outputFileInStaging = new File(fileInStaging.getAbsolutePath().replaceFirst("gpg", "cip"));
        final IngestionPipelineResult process = new DefaultIngestionPipeline(fileInStaging,
                getPrivateKeyRing(), getPrivateKeyRingPassword(), outputFileInStaging,
                getEncryptKey()).process();

        assertNotNull(process.getEncryptedFile());
        assertNull(process.getEncryptedIndexFile());
        assertTrue(process.getEncryptedFile().getFile().exists());
        assertEquals(getExpectedMd5(), process.getMd5());
        assertTrue(temporaryFolder.getRoot().toPath().resolve("test_file.txt.cip").toFile().exists());
        assertTrue(temporaryFolder.getRoot().toPath().resolve("test_file.txt.gpg").toFile().exists());
    }

    @Test(expected = UserErrorException.class)
    public void testWrongPgpFile() throws URISyntaxException, IOException, UserErrorException, SystemErrorException {
        File fileInStaging = copyToTemporaryFolder("/keyPairTest/test_file.txt.original");
        File outputFileInStaging = temporaryFolder.newFile("doesNotMatter");
        new DefaultIngestionPipeline(fileInStaging,
                getPrivateKeyRing(), getPrivateKeyRingPassword(), outputFileInStaging,
                getEncryptKey()).process();
    }

    @Test(expected = SystemErrorException.class)
    public void testWrongPgpPassword() throws URISyntaxException, IOException, UserErrorException, SystemErrorException {
        File fileInStaging = copyToTemporaryFolder("/keyPairTest/test_file.txt.gpg");
        File outputFileInStaging = temporaryFolder.newFile("doesNotMatter");
        new DefaultIngestionPipeline(fileInStaging,
                getPrivateKeyRing(), getWrongPrivateKeyRingPassword(), outputFileInStaging,
                getEncryptKey()).process();
    }

    @Test(expected = SystemErrorException.class)
    public void testMissingFile() throws URISyntaxException, IOException, UserErrorException, SystemErrorException {
        File fileInStaging = copyToTemporaryFolder("/keyPairTest/test_file.txt.gpg");
        new DefaultIngestionPipeline(fileInStaging,
                getPrivateKeyRing(), getPrivateKeyRingPassword(), new File("/nope"),
                getEncryptKey()).process();
    }

    private File getWrongPrivateKeyRingPassword() throws URISyntaxException {
        return new File(this.getClass().getResource("/keyPairTest/wrong_password.txt").toURI());
    }

    private String getExpectedMd5() throws IOException, URISyntaxException {
        return new String(FileUtils.readPasswordFile(
                new File(this.getClass().getResource("/keyPairTest/test_file.txt.md5").toURI()).toPath()));
    }

    private File copyToTemporaryFolder(String path) throws URISyntaxException, IOException {
        File originFile = new File(this.getClass().getResource(path).toURI());
        return Files.copy(originFile.toPath(),
                temporaryFolder.getRoot().toPath().resolve(originFile.getName())).toFile();
    }

    private File getPrivateKeyRing() throws URISyntaxException {
        return new File(this.getClass().getResource("/keyPairTest/secring.gpg").toURI());
    }

    private File getPrivateKeyRingPassword() throws URISyntaxException {
        return new File(this.getClass().getResource("/keyPairTest/password.txt").toURI());
    }

    private char[] getEncryptKey() throws URISyntaxException, IOException {
        return FileUtils.readPasswordFile(new File(
                this.getClass().getResource("/keyPairTest/encrypt_key.txt").toURI()).toPath());
    }

}
