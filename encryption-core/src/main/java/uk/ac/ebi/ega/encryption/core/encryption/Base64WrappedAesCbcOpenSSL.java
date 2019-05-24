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
package uk.ac.ebi.ega.encryption.core.encryption;

import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;

public class Base64WrappedAesCbcOpenSSL implements EncryptionAlgorithm {

    @Override
    public OutputStream encrypt(char[] password, OutputStream outputStream) throws AlgorithmInitializationException {
        final OutputStream base64EncodedOutputStream = Base64.getMimeEncoder().wrap(outputStream);
        final AesCbcOpenSSL aesCbcOpenSSL = new AesCbcOpenSSL();
        return aesCbcOpenSSL.encrypt(password, base64EncodedOutputStream);
    }

    @Override
    public InputStream decrypt(InputStream inputStream, char[] password) throws AlgorithmInitializationException {
        final InputStream base64DecodedInputStream = Base64.getMimeDecoder().wrap(inputStream);
        final AesCbcOpenSSL aesCbcOpenSSL = new AesCbcOpenSSL();
        return aesCbcOpenSSL.decrypt(base64DecodedInputStream, password);
    }

}
