/*
 *
 * Copyright 2018 EMBL - European Bioinformatics Institute
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
package uk.ac.ebi.ega.encryption.core.encryption;

import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.utils.io.IOUtils;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public abstract class JdkEncryptionAlgorithm implements EncryptionAlgorithm {

    @Override
    public OutputStream encrypt(char[] password, OutputStream outputStream) throws AlgorithmInitializationException {
        initializeWrite(password, outputStream);
        return new CipherOutputStream(outputStream, getCipher(Cipher.ENCRYPT_MODE));
    }

    @Override
    public InputStream decrypt(InputStream inputStream, char[] password) throws AlgorithmInitializationException {
        initializeRead(inputStream, password);
        return new CipherInputStream(inputStream, getCipher(Cipher.DECRYPT_MODE));
    }

    public byte[] encrypt(char[] password, char[] content) throws AlgorithmInitializationException {
        try (final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(content.length)) {
            initializeWrite(password, outputStream);
            try (OutputStream encryptStream = new CipherOutputStream(outputStream, getCipher(Cipher.ENCRYPT_MODE))) {
                encryptStream.write(IOUtils.convertToBytes(content));
            }
            outputStream.flush();
            outputStream.close();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new AlgorithmInitializationException(e);
        }
    }

    public char[] decrypt(char[] password, byte[] content, Charset encoding) throws AlgorithmInitializationException {
        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(content)) {
            initializeRead(inputStream, password);
            try (InputStream decryptedStream = new CipherInputStream(inputStream, getCipher(Cipher.DECRYPT_MODE))) {
                return IOUtils.toCharArray(decryptedStream, encoding);
            }
        } catch (IOException e) {
            throw new AlgorithmInitializationException(e);
        }
    }

    protected abstract void initializeRead(InputStream inputStream, char[] password) throws AlgorithmInitializationException;

    protected abstract void initializeWrite(char[] password, OutputStream outputStream) throws AlgorithmInitializationException;

    protected abstract Cipher getCipher(int encryptMode);

}
