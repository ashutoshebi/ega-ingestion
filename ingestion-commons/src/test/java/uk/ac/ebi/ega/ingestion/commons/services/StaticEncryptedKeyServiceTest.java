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
package uk.ac.ebi.ega.ingestion.commons.services;

import org.junit.Test;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;
import uk.ac.ebi.ega.encryption.core.services.PasswordEncryptionService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class StaticEncryptedKeyServiceTest {

    @Test
    public void testEncryptedStaticKeyCanBeDecrypted() throws AlgorithmInitializationException {
        IPasswordEncryptionService encryptionService = new PasswordEncryptionService("test".toCharArray());
        IEncryptedKeyService keyService = new StaticEncryptedKeyService(encryptionService,"test2".toCharArray());

        String value = keyService.generateNewEncryptedKey();
        final char[] chars = keyService.decryptKey(value);

        assertNotEquals("test2", value);
        assertEquals("test2", new String(chars));
    }

}
