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
package uk.ac.ebi.ega.encryption.core;

import org.bouncycastle.openpgp.PGPException;
import org.junit.Test;
import uk.ac.ebi.ega.encryption.core.encryption.PgpKeyring;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.WrongHeaderException;
import uk.ac.ebi.ega.encryption.core.exceptions.Md5CheckException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PgpKeyringTest {

    @Test
    public void testDecryptEncryptedMd5() throws IOException, PGPException, AlgorithmInitializationException,
            URISyntaxException, Md5CheckException {
        EncryptionService service = new BaseEncryptionService();

        URI encryptedFile = this.getClass().getResource("/keyPairTest/test_file.txt.gpg").toURI();
        String secretRing = this.getClass().getResource("/keyPairTest/secring.gpg").getFile();
        Path passphrase = Paths.get(this.getClass().getResource("/keyPairTest/password.txt").toURI());
        File encryptedMd5 = new File(this.getClass().getResource("/keyPairTest/test_file.txt.gpg.md5").getFile());
        service.encrypt(
                Input.file(new File(encryptedFile),
                        new PgpKeyring(new FileInputStream(secretRing)),
                        PasswordSource.fileSource(passphrase)),
                Md5Check.any(encryptedMd5),
                Output.noOutput());
    }

    @Test
    public void testDecryptDecryptedMd5() throws IOException, PGPException, AlgorithmInitializationException,
            URISyntaxException,
            Md5CheckException {
        EncryptionService service = new BaseEncryptionService();

        URI encryptedFile = this.getClass().getResource("/keyPairTest/test_file.txt.gpg").toURI();
        String secretRing = this.getClass().getResource("/keyPairTest/secring.gpg").getFile();
        Path passphrase = Paths.get(this.getClass().getResource("/keyPairTest/password.txt").toURI());
        File encryptedMd5 = new File(this.getClass().getResource("/keyPairTest/test_file.txt.md5").getFile());
        service.encrypt(
                Input.file(new File(encryptedFile),
                        new PgpKeyring(new FileInputStream(secretRing)),
                        PasswordSource.fileSource(passphrase)),
                Md5Check.any(encryptedMd5),
                Output.noOutput());
    }

    @Test(expected = Md5CheckException.class)
    public void testDecryptWrongMd5() throws IOException, AlgorithmInitializationException,
            URISyntaxException, Md5CheckException {
        EncryptionService service = new BaseEncryptionService();

        URI encryptedFile = this.getClass().getResource("/keyPairTest/test_file.txt.gpg").toURI();
        String secretRing = this.getClass().getResource("/keyPairTest/secring.gpg").getFile();
        Path passphrase = Paths.get(this.getClass().getResource("/keyPairTest/password.txt").toURI());
        service.encrypt(
                Input.file(new File(encryptedFile),
                        new PgpKeyring(new FileInputStream(secretRing)),
                        PasswordSource.fileSource(passphrase)),
                Md5Check.any("bad"),
                Output.noOutput());
    }

    @Test(expected = WrongHeaderException.class)
    public void testDecryptWrongKey() throws IOException, PGPException, AlgorithmInitializationException,
            URISyntaxException, Md5CheckException {
        EncryptionService service = new BaseEncryptionService();

        URI encryptedFile = this.getClass().getResource("/keyPairTest/test_file.txt2.gpg").toURI();
        String secretRing = this.getClass().getResource("/keyPairTest/secring.gpg").getFile();
        Path passphrase = Paths.get(this.getClass().getResource("/keyPairTest/password.txt").toURI());
        File encryptedMd5 = new File(this.getClass().getResource("/keyPairTest/test_file.txt.gpg.md5").getFile());
        service.encrypt(
                Input.file(new File(encryptedFile),
                        new PgpKeyring(new FileInputStream(secretRing)),
                        PasswordSource.fileSource(passphrase)),
                Md5Check.any(encryptedMd5),
                Output.noOutput());
    }

    @Test(expected = AlgorithmInitializationException.class)
    public void testDecryptMisconfiguration() throws IOException, AlgorithmInitializationException,
            URISyntaxException, Md5CheckException {
        EncryptionService service = new BaseEncryptionService();

        URI encryptedFile = this.getClass().getResource("/keyPairTest/test_file.txt.gpg").toURI();
        String secretRing = this.getClass().getResource("/keyPairTest/secring.gpg").getFile();
        File encryptedMd5 = new File(this.getClass().getResource("/keyPairTest/test_file.txt.gpg.md5").getFile());
        service.encrypt(
                Input.file(new File(encryptedFile),
                        new PgpKeyring(new FileInputStream(secretRing)),
                        PasswordSource.staticSource("badpassword".toCharArray())),
                Md5Check.any(encryptedMd5),
                Output.noOutput());
    }

}
