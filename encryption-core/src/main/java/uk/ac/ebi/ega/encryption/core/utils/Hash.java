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
package uk.ac.ebi.ega.encryption.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {

    private final static Logger logger = LoggerFactory.getLogger(Hash.class);

    public static MessageDigest getMd5() {
        return getHashingAlgorithm("MD5");
    }

    private static MessageDigest getHashingAlgorithm(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            logger.error(e.getMessage(), e);
            throw new AssertionError(e);
        }
    }

    public static MessageDigest getSha256() {
        return getHashingAlgorithm("SHA-256");
    }

    public static String normalize(MessageDigest messageDigest) {
        switch (messageDigest.getAlgorithm()) {
            case "MD5":
                return DatatypeConverter.printHexBinary(messageDigest.digest()).toLowerCase();
            default:
                return new String(messageDigest.digest());
        }
    }
}
