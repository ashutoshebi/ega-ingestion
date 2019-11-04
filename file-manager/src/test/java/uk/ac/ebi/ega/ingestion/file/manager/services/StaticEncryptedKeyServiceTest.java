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
package uk.ac.ebi.ega.ingestion.file.manager.services;

import org.junit.Test;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.services.PasswordEncryptionService;
import uk.ac.ebi.ega.ingestion.commons.services.StaticEncryptedKeyService;

import static org.junit.Assert.assertEquals;

public class StaticEncryptedKeyServiceTest {

    @Test
    public void testStaticEncrypted() throws AlgorithmInitializationException {
        StaticEncryptedKeyService service =
                new StaticEncryptedKeyService(new PasswordEncryptionService("kiwi".toCharArray()),"test".toCharArray());
        final String value = service.generateNewEncryptedKey();
        final char[] chars = service.decryptKey(value);
        assertEquals("test",new String(chars));
    }

}
