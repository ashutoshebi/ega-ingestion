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
package uk.ac.ebi.ega.encryption;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import uk.ac.ebi.ega.encryption.core.BaseEncryptionService;
import uk.ac.ebi.ega.encryption.core.EncryptionReport;
import uk.ac.ebi.ega.encryption.core.EncryptionService;
import uk.ac.ebi.ega.encryption.core.Input;
import uk.ac.ebi.ega.encryption.core.Md5Check;
import uk.ac.ebi.ega.encryption.core.Output;
import uk.ac.ebi.ega.encryption.core.PasswordSource;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.exceptions.Md5CheckException;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BaseEncryptionServiceTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testSuccess() throws IOException, URISyntaxException, Md5CheckException,
            AlgorithmInitializationException {
        EncryptionService service = new BaseEncryptionService();
        File originFile = new File(this.getClass().getResource("/keyPairTest/test_file.txt.gpg").toURI());
        File md5 = new File(this.getClass().getResource("/keyPairTest/test_file.txt.md5").getFile());
        File outputFile = temporaryFolder.newFile("test.out");
        String secretRing = this.getClass().getResource("/keyPairTest/secring.gpg").getFile();
        Path passphrase = Paths.get(this.getClass().getResource("/keyPairTest/password.txt").toURI());

        final EncryptionReport report = service.encrypt(Input.pgpPublicPrivateKeyringFile(
                originFile, new File(secretRing), PasswordSource.fileSource(passphrase)),
                Md5Check.any(md5),
                Output.plainFile(outputFile, true));
        assertTrue(outputFile.exists());
        assertEquals("b2e6283b2044de260d6df0e854cd3fa2", report.getOriginalMd5());
        assertEquals("c7081e1561dcc6434809ffb8bd67cca3", report.getUnencryptedMd5());
    }

    @Test
    public void testDeleteOnFailure() throws IOException, URISyntaxException, AlgorithmInitializationException {
        EncryptionService service = new BaseEncryptionService();
        File originFile = new File(this.getClass().getResource("/keyPairTest/test_file.txt.original").toURI());
        File outputFile = temporaryFolder.newFile("test.out");

        boolean exceptionThrown = false;
        try {
            service.encrypt(Input.plainFile(originFile), Md5Check.any("error"), Output.plainFile(outputFile, true));
        } catch (Md5CheckException e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        assertFalse(outputFile.exists());
    }

}
