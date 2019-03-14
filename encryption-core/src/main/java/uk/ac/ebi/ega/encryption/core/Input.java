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

import uk.ac.ebi.ega.encryption.core.encryption.EncryptionAlgorithm;
import uk.ac.ebi.ega.encryption.core.encryption.PgpKeyring;
import uk.ac.ebi.ega.encryption.core.encryption.PgpSymmetric;
import uk.ac.ebi.ega.encryption.core.encryption.Plain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public interface Input {

    InputStream getInputStream();

    EncryptionAlgorithm getEncryptionAlgorithm();

    char[] getPassword();

    void onFailure();

    static Input file(File file, EncryptionAlgorithm encryptionAlgorithm, PasswordSource passwordSource)
            throws IOException {
        InputStream inputStream = new FileInputStream(file);

        return new Input() {

            @Override
            public InputStream getInputStream() {
                return inputStream;
            }

            @Override
            public EncryptionAlgorithm getEncryptionAlgorithm() {
                return encryptionAlgorithm;
            }

            @Override
            public char[] getPassword() {
                return passwordSource.getPassword();
            }

            @Override
            public void onFailure() {
                // Do nothing
            }

        };
    }

    static Input plainFile(File file) throws IOException {
        return file(file, new Plain(), PasswordSource.staticSource(new char[0]));
    }

    static Input pgpSymmetricFile(File file, PasswordSource passwordSource) throws IOException {
        return file(file, new PgpSymmetric(), passwordSource);
    }

    static Input pgpPublicPrivateKeyringFile(File file, File privateKeyring, PasswordSource passwordSource)
            throws IOException {
        return file(file, new PgpKeyring(new FileInputStream(privateKeyring)), passwordSource);
    }

}
