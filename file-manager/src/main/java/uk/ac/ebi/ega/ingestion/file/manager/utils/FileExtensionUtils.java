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
package uk.ac.ebi.ega.ingestion.file.manager.utils;

import com.querydsl.core.util.ArrayUtils;

import java.util.HashMap;

import static uk.ac.ebi.ega.ingestion.file.manager.utils.FileType.BAM;
import static uk.ac.ebi.ega.ingestion.file.manager.utils.FileType.BINARY;
import static uk.ac.ebi.ega.ingestion.file.manager.utils.FileType.COMPRESSED;
import static uk.ac.ebi.ega.ingestion.file.manager.utils.FileType.CRAM;
import static uk.ac.ebi.ega.ingestion.file.manager.utils.FileType.ENCRYPTED;
import static uk.ac.ebi.ega.ingestion.file.manager.utils.FileType.FASTA;
import static uk.ac.ebi.ega.ingestion.file.manager.utils.FileType.GTC;
import static uk.ac.ebi.ega.ingestion.file.manager.utils.FileType.PLAIN_TEXT;
import static uk.ac.ebi.ega.ingestion.file.manager.utils.FileType.TABIX_INDEX;
import static uk.ac.ebi.ega.ingestion.file.manager.utils.FileType.VCF;

public class FileExtensionUtils {

    private static HashMap<String, FileType> possibleExtensions;

    static {
        possibleExtensions = new HashMap<>();
        possibleExtensions.put("bam", BAM);
        possibleExtensions.put("bim", BINARY);
        possibleExtensions.put("cram", CRAM);
        possibleExtensions.put("cip", ENCRYPTED);
        possibleExtensions.put("gpg", ENCRYPTED);
        possibleExtensions.put("txt", PLAIN_TEXT);
        possibleExtensions.put("gz", COMPRESSED);
        possibleExtensions.put("tar", COMPRESSED);
        possibleExtensions.put("rar", COMPRESSED);
        possibleExtensions.put("zip", COMPRESSED);
        possibleExtensions.put("7zip", COMPRESSED);
        possibleExtensions.put("fam", PLAIN_TEXT);
        possibleExtensions.put("fastq", FASTA);
        possibleExtensions.put("fasta", FASTA);
        possibleExtensions.put("fas", FASTA);
        possibleExtensions.put("fa", FASTA);
        possibleExtensions.put("seq", FASTA);
        possibleExtensions.put("fsa", FASTA);
        possibleExtensions.put("ffn", FASTA);
        possibleExtensions.put("faa", FASTA);
        possibleExtensions.put("mpfa", FASTA);
        possibleExtensions.put("frn", FASTA);
        possibleExtensions.put("vcf", VCF);
        possibleExtensions.put("tbi", TABIX_INDEX);
        possibleExtensions.put("gtc", GTC);
        possibleExtensions.put("bed", BINARY);
        possibleExtensions.put("ab1", BINARY);
        possibleExtensions.put("bgen", BINARY);
    }

    public static String getFilename(String filenameWithExtensions) {
        final String[] nameChunks = filenameWithExtensions.split(".");
        StringBuilder sb = new StringBuilder(nameChunks[0]);
        for (int i = 1; i < nameChunks.length; i++) {
            if (!possibleExtensions.containsKey(nameChunks[i])) {
                sb.append(".").append(nameChunks[i]);
            } else {
                break;
            }
        }
        return sb.toString();
    }

    public static String getFileExtension(String filenameWithExtensions) {
        final String[] nameChunks = filenameWithExtensions.split("\\.");
        int i = 0;
        for (; i < nameChunks.length; i++) {
            if (!possibleExtensions.containsKey(nameChunks[i])) {
                continue;
            } else {
                break;
            }
        }
        return String.join(".", (String[]) ArrayUtils.subarray(nameChunks, i, nameChunks.length));
    }


    public static String getEncryptionExtension(String fileExtensionWithEncryption) {
        if (fileExtensionWithEncryption.endsWith("gpg") || fileExtensionWithEncryption.endsWith("cip")) {
            return fileExtensionWithEncryption.substring(fileExtensionWithEncryption.length() - 3);
        }
        return null;
    }

    public static String removeEncryptionExtension(String filenameWithExtensions) {
        if (filenameWithExtensions.endsWith("gpg") || filenameWithExtensions.endsWith("cip")) {
            int itr = filenameWithExtensions.length() > 3 ? 4 : 3;
            return filenameWithExtensions.substring(0, filenameWithExtensions.length() - itr);
        }
        return filenameWithExtensions;
    }

}
