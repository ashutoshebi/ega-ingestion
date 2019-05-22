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

import org.bouncycastle.util.io.Streams;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.WrongHeaderException;
import uk.ac.ebi.ega.encryption.core.utils.Encryption;
import uk.ac.ebi.ega.encryption.core.utils.Hash;
import uk.ac.ebi.ega.encryption.core.utils.Random;
import uk.ac.ebi.ega.encryption.core.utils.io.IOUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Implements encryption / decryption using AES 256 CBC no IV compatible with openssl
 */
public class AesCbcOpenSSL extends JdkEncryptionAlgorithm {

    private static final byte[] SALTED_MAGIC = "Salted__".getBytes(US_ASCII);

    private boolean useMd5Salt;

    private boolean noSalt;

    private byte[] fixedSalt;

    private SecretKeySpec secretKeySpec;

    private IvParameterSpec ivParameterSpec;

    public AesCbcOpenSSL() {
        super();
        noSalt = false;
        useMd5Salt = false;
    }

    @Override
    protected void initializeRead(InputStream inputStream, char[] password) throws AlgorithmInitializationException {
        byte[] salt = noSalt ? new byte[0] : readSaltFromHeader(inputStream);
        initializePasswordAndIV(IOUtils.convertToBytes(password), salt);
    }

    private void initializePasswordAndIV(byte[] password, byte[] salt) {
        if(useMd5Salt){
            initializePasswordAndIvMd5(password,salt);
        }else{
            initializePasswordAndIVSha256(password,salt);
        }
    }

    private void initializePasswordAndIvMd5(byte[] password, byte[] salt) {
        final byte[] passAndSalt = concat(password, salt);
        byte[] hash = new byte[0];
        byte[] keyAndIv = new byte[0];
        for (int i = 0; i < 3; i++) {
            final byte[] data = concat(hash, passAndSalt);
            final MessageDigest md = Hash.getMd5();
            hash = md.digest(data);
            keyAndIv = concat(keyAndIv, hash);
        }

        final byte[] keyValue = Arrays.copyOfRange(keyAndIv, 0, 32);
        final byte[] IV = Arrays.copyOfRange(keyAndIv, 32, 48);
        secretKeySpec = new SecretKeySpec(keyValue, "AES");
        ivParameterSpec = new IvParameterSpec(IV);
    }

    private void initializePasswordAndIVSha256(byte[] password, byte[] salt) {
        final byte[] passAndSalt = concat(password, salt);
        final MessageDigest digestFunction = Hash.getSha256();

        byte[] hash = new byte[0];
        byte[] temp = new byte[0];
        for (int i = 0; i < 2; i++) {
            hash = digestFunction.digest(concat(hash, passAndSalt));
            temp = concat(temp, hash);
        }

        byte[] pass = digestFunction.digest(passAndSalt);
        byte[] IV = Arrays.copyOfRange(digestFunction.digest(concat(pass, passAndSalt)), 0, 16);
        secretKeySpec = new SecretKeySpec(pass, "AES");
        ivParameterSpec = new IvParameterSpec(IV);
    }

    private static byte[] concat(final byte[] a, final byte[] b) {
        final byte[] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    private byte[] readSaltFromHeader(InputStream inputStream) throws AlgorithmInitializationException {
        assertHeader(inputStream);
        byte[] salt = new byte[8];
        try {
            Streams.readFully(inputStream, salt);
        } catch (IOException e) {
            throw new AlgorithmInitializationException("AesCbc OpenSSL Could not read salt", e);
        }
        return salt;
    }

    private void assertHeader(InputStream inputStream) throws AlgorithmInitializationException {
        byte[] header = new byte[SALTED_MAGIC.length];
        try {
            Streams.readFully(inputStream, header);
        } catch (IOException e) {
            throw new AlgorithmInitializationException("AesCbc OpenSSL Could not read header", e);
        }
        if (!Arrays.equals(header, SALTED_MAGIC)) {
            throw new WrongHeaderException("Wrong file header, 'Salted__' not found");
        }
    }

    @Override
    protected void initializeWrite(char[] password, OutputStream outputStream) throws AlgorithmInitializationException {
        try {
            byte[] salt = noSalt ? new byte[0] : generateAndWriteSaltHeader(outputStream);
            initializePasswordAndIV(IOUtils.convertToBytes(password), salt);
        } catch (IOException e) {
            throw new AlgorithmInitializationException("AesCbcOpenSSL could not write magic and salt to output", e);
        }
    }

    private byte[] generateAndWriteSaltHeader(OutputStream outputStream) throws IOException {
        outputStream.write(SALTED_MAGIC);
        byte[] salt = generateSalt();
        outputStream.write(salt);
        outputStream.flush();
        return salt;
    }

    private byte[] generateSalt() {
        if (fixedSalt != null) {
            return fixedSalt;
        }

        byte[] randomBytes = new byte[8];
        Random.getSHA1PRNG().nextBytes(randomBytes);
        return randomBytes;
    }


    @Override
    protected Cipher getCipher(int encryptMode) {
        return Encryption.getCipher("AES/CBC/PKCS5Padding", encryptMode, secretKeySpec, ivParameterSpec);
    }

    public byte[] getFixedSalt() {
        return fixedSalt;
    }

    public void setFixedSalt(byte[] fixedSalt) {
        noSalt = false;
        this.fixedSalt = fixedSalt;
    }

    public void setNoSalt() {
        noSalt = true;
    }

    public void setUseMd5Salt(boolean useMd5Salt) {
        this.useMd5Salt = useMd5Salt;
    }
}
