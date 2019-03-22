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
import uk.ac.ebi.ega.encryption.core.DecryptInputStream;
import uk.ac.ebi.ega.encryption.core.EncryptionService;
import uk.ac.ebi.ega.encryption.core.StreamPipelineBuilder;
import uk.ac.ebi.ega.encryption.core.encryption.PgpKeyring;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.stream.PipelineStream;
import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestPipeline {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void kiwi() throws URISyntaxException, IOException, AlgorithmInitializationException, InterruptedException {
        EncryptionService service = new BaseEncryptionService();
        File originFile = new File(this.getClass().getResource("/keyPairTest/test_file.txt.gpg").toURI());
        String secretRing = this.getClass().getResource("/keyPairTest/secring.gpg").getFile();
        Path passphrase = Paths.get(this.getClass().getResource("/keyPairTest/password.txt").toURI());

        File output = temporaryFolder.newFile("test.out");
        File outputWc = temporaryFolder.newFile("wc.out");
        ProcessBuilder pb = new ProcessBuilder("wc", "-w");
        pb.redirectOutput(outputWc);
        Process process = pb.start();
        try (
                final DecryptInputStream decryptInputStream = new DecryptInputStream(
                        new FileInputStream(originFile),
                        new PgpKeyring(new FileInputStream(secretRing)),
                        FileUtils.readPasswordFile(passphrase));
                PipelineStream source = StreamPipelineBuilder
                        .source(decryptInputStream)
                        .to(new FileOutputStream(output))
                        .to(process.getOutputStream())
                        .build();
        ) {
            source.execute();
            assertTrue(output.exists());
            assertEquals("c7081e1561dcc6434809ffb8bd67cca3", decryptInputStream.getUnencryptedMd5());
        }
        process.waitFor();
        assertEquals(0, process.exitValue());
    }

//    @Test
//    public void kiwi2() throws URISyntaxException, IOException, AlgorithmInitializationException, InterruptedException {
//        EncryptionService service = new BaseEncryptionService();
//        File originFile = new File("/home/jorizci/Chr21.CIV_1.bam.gpg");
//        File secretRing = new File("/home/jorizci/ega_key/secring.gpg");
//        Path passphrase =new File("/home/jorizci/ega_key/EGA_Public_key_passphrase").toPath();
//
//        File output = temporaryFolder.newFile("test.out");
//        File outputWc = temporaryFolder.newFile("wc.out");
//        ProcessBuilder pb = new ProcessBuilder("samtools", "index", "-", output.getAbsolutePath()+".bai");
//        pb.redirectOutput(outputWc);
//        Process process = pb.start();
//
//        try (
//                final DecryptionInputStream decryptionInputStream = new DecryptionInputStream(
//                        new FileInputStream(originFile),
//                        new PgpKeyring(new FileInputStream(secretRing)),
//                        FileUtils.readPasswordFile(passphrase));
//                StreamSource source = StreamPipelineBuilder
//                        .source(decryptionInputStream)
//                        .to(new FileOutputStream(output));
//        ) {
//            source.execute();
//            assertTrue(output.exists());
////            assertEquals("c7081e1561dcc6434809ffb8bd67cca3", decryptionInputStream.getUnencryptedMd5());
//        }
//        process.waitFor();
//        assertEquals(0, process.exitValue());
//    }

}
