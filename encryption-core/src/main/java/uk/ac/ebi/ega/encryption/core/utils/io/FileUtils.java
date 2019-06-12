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
package uk.ac.ebi.ega.encryption.core.utils.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
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

    /**
     * This method is intended for small files that can be handled in memory.
     *
     * @param path
     * @return
     * @throws IOException
     */
    public static char[] readFile(Path path) throws IOException {
        final byte[] bytes = Files.readAllBytes(path);
        char[] chars = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            chars[i] = (char) (bytes[i] & 0xFF);
        }
        return chars;
    }

    public static char[] trim(char[] array) {
        int charsToSkipAtBeginning = 0;
        int lastPositionToCopy = array.length - 1;

        for (int i = 0; i < array.length; i++) {
            if (array[i] == ' ' || array[i] == '\n' || array[i] == '\t') {
                charsToSkipAtBeginning++;
            } else {
                break;
            }
        }

        for (int i = lastPositionToCopy; i > 0 && lastPositionToCopy > charsToSkipAtBeginning; i--) {
            if (!(array[i] == ' ' || array[i] == '\n' || array[i] == '\t')) {
                lastPositionToCopy = i;
                break;
            }
        }
        return Arrays.copyOfRange(array, charsToSkipAtBeginning, lastPositionToCopy + 1);
    }

    public static char[] readPasswordFile(Path path) throws IOException {
        return trim(readFile(path));
    }

    public static String normalizeSize(long size) {
        float normalizedSize = size;
        int unit = 0;
        while (normalizedSize > 1024) {
            normalizedSize /= 1024;
            unit++;
        }

        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(0);
        df.setMaximumFractionDigits(2);
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.UK));

        String textValue = df.format(normalizedSize);
        switch (unit) {
            case 0:
                return textValue + "bytes";
            case 1:
                return textValue + "Kb";
            case 2:
                return textValue + "Mb";
            case 3:
                return textValue + "Gb";
            case 4:
                return textValue + "Tb";
            case 5:
                return textValue + "Pb";
            case 6:
                return textValue + "Eb";
            case 7:
                return textValue + "Zb";
            case 8:
                return textValue + "Yb";
        }
        return Long.toString(size);
    }

    // Copied from https://github.com/EbiEga/ega-production/blob/master/
    //  database-commons/src/main/java/uk/ac/ebi/ega/database/commons/utils/FileUtils.java
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
