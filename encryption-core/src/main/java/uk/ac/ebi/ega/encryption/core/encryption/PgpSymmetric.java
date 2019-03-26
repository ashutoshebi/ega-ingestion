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

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPBEEncryptedData;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PBEDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBEDataDecryptorFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.WrongHeaderException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;

/**
 * Provides functionality to decrypt PGP-symmetric files
 */
public class PgpSymmetric implements EncryptionAlgorithm {

    private final static Logger logger = LoggerFactory.getLogger(PgpSymmetric.class);

    @Override
    public OutputStream encrypt(char[] password, OutputStream outputStream) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream decrypt(InputStream input, char[] passPhrase) throws AlgorithmInitializationException {
        installProviderIfNeeded();
        try {
            InputStream decoderStream = PGPUtil.getDecoderStream(input);
            PGPPBEEncryptedData pbe = getPBEEncryptedData(decoderStream);

            InputStream clearCompressedStream;
            try {
                clearCompressedStream = pbe.getDataStream(getDataDecryptorFactory(passPhrase));
            } catch (PGPException e) {
                logger.error("Data check error, possible password mismatch", e);
                throw new WrongHeaderException("Data check error, possible password mismatch");
            }
            return getPGPLiteralData(clearCompressedStream).getInputStream();
        } catch (IOException e) {
            throw new AlgorithmInitializationException(e);
        }
    }

    private static PBEDataDecryptorFactory getDataDecryptorFactory(char[] passPhrase) {
        try {
            PGPDigestCalculatorProvider digestCalculatorProvider = new JcaPGPDigestCalculatorProviderBuilder()
                    .setProvider("BC").build();
            return new JcePBEDataDecryptorFactoryBuilder(digestCalculatorProvider)
                    .setProvider("BC").build(passPhrase);
        } catch (PGPException e) {
            logger.error(e.getMessage(), e);
            throw new AssertionError(e);
        }
    }

    private static PGPLiteralData getPGPLiteralData(InputStream clearCompressedStream) throws IOException {
        PGPCompressedData cData = (PGPCompressedData) new JcaPGPObjectFactory(clearCompressedStream).nextObject();
        try {
            return (PGPLiteralData) new JcaPGPObjectFactory(cData.getDataStream()).nextObject();
        } catch (PGPException e) {
            logger.error(e.getMessage(), e);
            throw new AssertionError(e);
        }
    }

    private static PGPPBEEncryptedData getPBEEncryptedData(InputStream input) throws AlgorithmInitializationException,
            IOException {
        JcaPGPObjectFactory pgpF = new JcaPGPObjectFactory(input);
        PGPEncryptedDataList enc;

        Object o = pgpF.nextObject();
        // the first object might be a PGP marker packet.
        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }
        if (enc == null) {
            throw new AlgorithmInitializationException("PGP PBE header could not be read");
        }

        return (PGPPBEEncryptedData) enc.get(0);
    }

    private static void installProviderIfNeeded() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

}
