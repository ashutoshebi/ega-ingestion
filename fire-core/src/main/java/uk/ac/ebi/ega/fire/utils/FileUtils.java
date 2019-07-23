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
package uk.ac.ebi.ega.fire.utils;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FileUtils {

    private final static Set<String> knownFormats = new HashSet<>(Arrays.asList(
            "cip", "gpg",
            "txt", "gz", "tar", "rar", "zip", "7zip",
            "vcf", "tbi", "bam", "cram",
            "fastq", "fasta", "fas", "fa", "seq", "fsa", "ffn", "faa", "mpfa", "frn",
            "gtc",
            "bgen"
    ));

    public static String getType(String fileName) {

        String[] components = new File(fileName).getName().split("\\.");
        if (components.length == 1) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        // Skip file name, assume 0 is name for sure, now we advance until we find a known extension
        int i = 1;
        for (; i < components.length; i++) {
            if (knownFormats.contains(components[i])) {
                break;
            }
        }
        // Now we continue until we find the encrypted format adding all the file extensions
        for (; i < components.length; i++) {
            if ("cip".equals(components[i]) || "gpg".equals(components[i])) {
                break;
            }
            stringBuilder.append(".").append(components[i]);
        }
        return stringBuilder.toString();
    }

}
