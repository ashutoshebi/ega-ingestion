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
package uk.ac.ebi.ega.file.encryption.processor;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.IngestionPipelineResult;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.IngestionSamToolsIndex;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.PipelineBuilder;
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

public class PipelineBuilderTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testPipelineBuilderBasic() throws URISyntaxException, IOException, UserErrorException,
            SystemErrorException {
        File fileInStaging = copyToTemporaryFolder("/keyPairTest/test_file.txt.gpg");
        PipelineService service = new PipelineBuilder(getPrivateKeyRing(), getPrivateKeyRingPassword());
        final IngestionPipelineResult process = service.getPipeline(fileInStaging).process();

        assertNotNull(process.getEncryptedFile());
        assertNull(process.getEncryptedIndexFile());
        assertTrue(process.getEncryptedFile().getFile().exists());
        assertEquals(getExpectedMd5(), process.getMd5());
        assertTrue(temporaryFolder.getRoot().toPath().resolve("test_file.txt.cip").toFile().exists());
        assertTrue(temporaryFolder.getRoot().toPath().resolve("test_file.txt.gpg").toFile().exists());
    }

    @Test
    public void testPipelineBuilderBam() throws URISyntaxException, IOException, UserErrorException,
            SystemErrorException {
        File fileInStaging = copyToTemporaryFolder("/keyPairTest/test.bam.gpg");
        PipelineService service = new PipelineBuilder(getPrivateKeyRing(), getPrivateKeyRingPassword());
        assertTrue(service.getPipeline(fileInStaging) instanceof IngestionSamToolsIndex);
        final IngestionPipelineResult process = service.getPipeline(fileInStaging).process();

        assertNotNull(process.getEncryptedFile());
        assertTrue(process.getEncryptedFile().getFile().exists());
        assertNotNull(process.getEncryptedIndexFile());
        assertTrue(process.getEncryptedIndexFile().getFile().exists());

        assertTrue(temporaryFolder.getRoot().toPath().resolve("test.bam.cip").toFile().exists());
        assertTrue(temporaryFolder.getRoot().toPath().resolve("test.bai.cip").toFile().exists());
        assertTrue(temporaryFolder.getRoot().toPath().resolve("test.bam.gpg").toFile().exists());
    }

    @Test(expected = UserErrorException.class)
    public void testPipelineBuilderBadBam() throws URISyntaxException, IOException, UserErrorException,
            SystemErrorException {
        File fileInStaging = copyToTemporaryFolder("/keyPairTest/bad_bam.bam.gpg");
        PipelineService service = new PipelineBuilder(getPrivateKeyRing(), getPrivateKeyRingPassword());
        assertTrue(service.getPipeline(fileInStaging) instanceof IngestionSamToolsIndex);
        service.getPipeline(fileInStaging).process();
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

    @Test(expected = UserErrorException.class)
    public void testWrongPgpFile() throws URISyntaxException, IOException, UserErrorException, SystemErrorException {
        File fileInStaging = copyToTemporaryFolder("/keyPairTest/test_file.txt.original");
        PipelineService service = new PipelineBuilder(getPrivateKeyRing(), getPrivateKeyRingPassword());
        service.getPipeline(fileInStaging).process();
    }

    @Test(expected = SystemErrorException.class)
    public void testWrongPgpPassword() throws URISyntaxException, IOException, UserErrorException, SystemErrorException {
        File fileInStaging = copyToTemporaryFolder("/keyPairTest/test_file.txt.gpg");
        PipelineService service = new PipelineBuilder(getPrivateKeyRing(), getWrongPrivateKeyRingPassword());
        service.getPipeline(fileInStaging).process();
    }

    @Test(expected = SystemErrorException.class)
    public void testMissingFile() throws URISyntaxException, IOException, UserErrorException, SystemErrorException {
        File fileInStaging = copyToTemporaryFolder("/keyPairTest/test_file.txt.gpg");
        PipelineService service = new PipelineBuilder(getPrivateKeyRing(), new File("/nope"));
        service.getPipeline(fileInStaging).process();
    }

    private File getWrongPrivateKeyRingPassword() throws URISyntaxException {
        return new File(this.getClass().getResource("/keyPairTest/wrong_password.txt").toURI());
    }

}
