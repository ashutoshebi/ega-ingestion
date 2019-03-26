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
package uk.ac.ebi.ega.encryption.core.encryption;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.bc.BcPGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.WrongHeaderException;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.WrongPassword;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.Iterator;

public class PgpKeyring implements EncryptionAlgorithm {

    private InputStream privateKeyringStream;

    public PgpKeyring(InputStream privateKeyringStream) {
        this.privateKeyringStream = privateKeyringStream;
    }

    @Override
    public OutputStream encrypt(char[] password, OutputStream outputStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream decrypt(InputStream inputStream, char[] password) throws AlgorithmInitializationException {
        installProviderIfNeeded();

        PGPEncryptedDataList encryptedDataList = null;
        try {
            encryptedDataList = getPgpEncryptedDataList(inputStream);
            if(encryptedDataList==null){
                throw new WrongHeaderException("No Pgp header found");
            }
        } catch (IOException e) {
            throw new AlgorithmInitializationException("Unexpected exception", e);
        }

        Iterator<PGPPublicKeyEncryptedData> iterator = encryptedDataList.getEncryptedDataObjects();
        PGPPrivateKey privateKey = null;
        PGPPublicKeyEncryptedData encryptedPublicKeyData = null;

        while (privateKey == null && iterator.hasNext()) {
            encryptedPublicKeyData = iterator.next();
            try {
                privateKey = findSecretKey(encryptedPublicKeyData.getKeyID(), password);
            } catch (IOException e) {
                throw new AlgorithmInitializationException("Exception retrieving secret Key", e);
            } catch (PGPException e){
                throw new WrongPassword("Error decrypting pgp keyring",e);
            }
        }

        if (privateKey == null) {
            throw new WrongHeaderException("Secret key for message not found.");
        }

        Object message = getMessage(privateKey, encryptedPublicKeyData);

        if (message instanceof PGPLiteralData) {
            return ((PGPLiteralData) message).getInputStream();
        } else if (message instanceof PGPOnePassSignatureList) {
            throw new AlgorithmInitializationException("Encrypted message contains a signed message.");
        } else {
            throw new AlgorithmInitializationException("Message type unknown.");
        }
    }

    private Object getMessage(PGPPrivateKey privateKey, PGPPublicKeyEncryptedData encryptedPublicKeyData)
            throws AlgorithmInitializationException {
        try {
            return decompressMessageIfNeeded(getClearData(privateKey, encryptedPublicKeyData));
        } catch (PGPException | IOException e) {
            throw new AlgorithmInitializationException("Unexpected exception", e);
        }
    }

    private Object decompressMessageIfNeeded(InputStream clear) throws PGPException, IOException {
        Object message = new JcaPGPObjectFactory(clear).nextObject();
        if (message instanceof PGPCompressedData) {
            PGPCompressedData cData = (PGPCompressedData) message;
            PGPObjectFactory pgpFact = new JcaPGPObjectFactory(cData.getDataStream());
            message = pgpFact.nextObject();
        }
        return message;
    }

    private InputStream getClearData(PGPPrivateKey privateKey, PGPPublicKeyEncryptedData encryptedPublicKeyData)
            throws AlgorithmInitializationException {
        InputStream clear;
        try {
            clear = encryptedPublicKeyData.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC")
                    .build(privateKey));
        } catch (PGPException e) {
            throw new AlgorithmInitializationException("Could not decrypt public key.");
        }
        return clear;
    }

    /**
     * Retrieves the encrypted data list skipping any pgp header marker block if any
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    private PGPEncryptedDataList getPgpEncryptedDataList(InputStream inputStream) throws IOException {
        return skipHeaderMarker(new JcaPGPObjectFactory(PGPUtil.getDecoderStream(inputStream)));
    }

    private PGPEncryptedDataList skipHeaderMarker(PGPObjectFactory pgpF) throws IOException {
        Object o = pgpF.nextObject();
        if (o instanceof PGPEncryptedDataList) {
            return (PGPEncryptedDataList) o;
        } else {
            return (PGPEncryptedDataList) pgpF.nextObject();
        }
    }

    private PGPPrivateKey findSecretKey(long keyID, char[] password) throws PGPException, IOException {
        PGPSecretKey pgpSecKey =
                new BcPGPSecretKeyRingCollection(PGPUtil.getDecoderStream(privateKeyringStream)).getSecretKey(keyID);
        if (pgpSecKey == null) {
            return null;
        }
        return pgpSecKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(password));
    }

    private static void installProviderIfNeeded() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

}
