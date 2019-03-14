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
package uk.ac.ebi.ega.fire.metadata;

import uk.ac.ebi.ega.fire.FireDirectFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class FileMetadataParser {

    public static List<FireDirectFile> parse(InputStream inputStream) throws IOException, ParseException {
        List<FireDirectFile> links = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = in.readLine()) != null) {
                assertBegin(line);
                links.add(parseStorage(in));
            }
        }
        return links;
    }

    private static void assertBegin(String line) throws ParseException {
        if (!line.equals("BEGIN")) {
            throw new ParseException("Expected token 'BEGIN' but found '" + line + "'", 0);
        }
    }

    private static FireDirectFile parseStorage(BufferedReader in) throws IOException, ParseException {
        StringBuffer buffer = new StringBuffer();
        String line;
        String urlGet = null;
        String urlHead = null;
        String md5 = null;
        long size = -1;
        String expireTimestamp = null;
        String storageClass = null;
        while ((line = in.readLine()) != null) {
            buffer.append(line);
            if (line.equals("END")) {
                break;
            } else {
                final String[] fields = line.split(" ");
                if (fields.length > 2) {
                    throw new ParseException("Line is not a key value pair '" + line + "'", 0);
                }
                switch (fields[0]) {
                    case "OBJECT_GET":
                        urlGet = fields[1];
                        break;
                    case "OBJECT_HEAD":
                        urlHead = fields[1];
                        break;
                    case "OBJECT_MD5":
                        md5 = fields[1];
                        break;
                    case "OBJECT_LENGTH":
                        size = Long.parseLong(fields[1]);
                        break;
                    case "OBJECT_URL_EXPIRE":
                        expireTimestamp = fields[1];
                        break;
                    case "OBJECT_STORAGE_CLASS":
                        storageClass = fields[1];
                        break;
                }
            }
        }
        return new FireDirectFile(urlGet, urlHead, md5, size, expireTimestamp, storageClass);
    }

}
