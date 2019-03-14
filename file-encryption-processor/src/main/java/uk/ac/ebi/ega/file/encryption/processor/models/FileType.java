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
package uk.ac.ebi.ega.file.encryption.processor.models;

import java.io.File;
import java.util.HashMap;

public enum FileType {

    ENCRYPTED,

    BAM,

    BINARY;

    private static HashMap<String, FileType> possibleExtensions;

    public static FileType fromExtension(File file) {
        return fromName(file.getName());
    }

    private static FileType fromName(String name) {
        final String[] split = name.split(".");
        for (int i = split.length - 2; i >= 0; i--) {
            FileType fileType = possibleExtensions.get(split[i].toLowerCase());
            switch ((fileType != null) ? fileType : BINARY) {
                case ENCRYPTED:
                    break;
                default:
                    return fileType;
            }
        }
        return BINARY;
    }

    static {
        possibleExtensions = new HashMap<>();
        possibleExtensions.put("bam", BAM);
        possibleExtensions.put("cip", ENCRYPTED);
        possibleExtensions.put("gpg", ENCRYPTED);
    }

}
