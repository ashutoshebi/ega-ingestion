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

import uk.ac.ebi.ega.encryption.core.encryption.AesCbcOpenSSL;
import uk.ac.ebi.ega.encryption.core.encryption.AesCtr256Ega;
import uk.ac.ebi.ega.encryption.core.encryption.EncryptionAlgorithm;
import uk.ac.ebi.ega.encryption.core.encryption.Plain;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public interface Output {

    OutputStream getOutputStream();

    EncryptionAlgorithm getEncryptionAlgorithm();

    char[] getPassword();

    void onFailure();

    static Output noOutput() {
        return new Output() {

            @Override
            public OutputStream getOutputStream() {
                return new OutputStream() {

                    @Override
                    public void write(final byte[] bytes, final int offset, final int length) {
                        //to /dev/null
                    }

                    @Override
                    public void write(final int byte_) {
                        //to /dev/null
                    }

                    @Override
                    public void write(final byte[] bytes) throws IOException {
                        //to /dev/null
                    }

                };
            }

            @Override
            public EncryptionAlgorithm getEncryptionAlgorithm() {
                return new Plain();
            }

            @Override
            public char[] getPassword() {
                return new char[0];
            }

            @Override
            public void onFailure() {
                // Do nothing
            }
        };
    }

    static Output file(File file, boolean deleteOnFailure, EncryptionAlgorithm encryptionAlgorithm,
                       PasswordSource source) throws IOException {
        file.createNewFile();
        FileOutputStream outputStream = new FileOutputStream(file);
        return new Output() {

            @Override
            public OutputStream getOutputStream() {
                return outputStream;
            }

            @Override
            public EncryptionAlgorithm getEncryptionAlgorithm() {
                return encryptionAlgorithm;
            }

            @Override
            public char[] getPassword() {
                return source.getPassword();
            }

            @Override
            public void onFailure() {
                if (deleteOnFailure) {
                    file.delete();
                }
            }

        };
    }

    static Output plainFile(File path, boolean deleteOnFailure) throws IOException {
        return file(path, deleteOnFailure, new Plain(), PasswordSource.staticSource(new char[0]));
    }

    static Output aesCtr256Ega(File path, boolean deleteOnFailure, PasswordSource passwordSource) throws IOException {
        return file(path, deleteOnFailure, new AesCtr256Ega(), passwordSource);
    }

    static Output aesCbcOpenSSL(File path, boolean deleteOnFailure, PasswordSource passwordSource) throws IOException {
        return file(path, deleteOnFailure, new AesCbcOpenSSL(), passwordSource);
    }

}
