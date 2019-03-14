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

import uk.ac.ebi.ega.encryption.core.exceptions.Md5CheckException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.stream.Collectors;

public interface Md5Check {

    void check(String originalMd5, String unencryptedMd5) throws Md5CheckException;

    String getMd5Configuration();

    static Md5Check any(String md5) {
        return new Md5Check() {
            @Override
            public void check(String originalMd5, String unencryptedMd5) throws Md5CheckException {
                if (!Objects.equals(md5, originalMd5) && !Objects.equals(md5, unencryptedMd5)) {
                    throw new Md5CheckException("Expected Md5 value '" + md5 + "' did not match any possible value");
                }
            }

            @Override
            public String getMd5Configuration() {
                return "Any value of Md5 matches '" + md5 + "'";
            }
        };
    }

    static Md5Check any(File md5File) throws IOException {
        return any(Files.lines(md5File.toPath()).collect(Collectors.joining()).trim());
    }

}
