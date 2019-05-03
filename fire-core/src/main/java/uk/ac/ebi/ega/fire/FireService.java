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
package uk.ac.ebi.ega.fire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.fire.exceptions.FireConfigurationException;
import uk.ac.ebi.ega.fire.metadata.FileMetadataParser;
import uk.ac.ebi.ega.fire.properties.FireProperties;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.List;

public class FireService implements IFireService {

    private static final Logger logger = LoggerFactory.getLogger(FireService.class);

    private FireProperties fireProperties;

    private URL fireUrl;

    public FireService(FireProperties fireProperties) throws MalformedURLException {
        this.fireProperties = fireProperties;
        this.fireUrl = new URL(fireProperties.getUrl());
    }

    @Override
    public IFireFile getFile(String fireFilePath) throws IOException, FireConfigurationException, ParseException {
        HttpURLConnection connection = null;
        try {
            connection = prepareConnection(fireFilePath, fireUrl);
            switch (connection.getResponseCode()) {
                case HttpURLConnection.HTTP_OK:
                    FireDirectFile file = getFileOnS3OrFirstOne(FileMetadataParser.parse(connection.getInputStream()));
                    if (file != null) {
                        logger.info("Used file source storage is {}", file.getStorageClass());
                        return file;
                    }
                case HttpURLConnection.HTTP_NOT_FOUND:
                    throw new FileNotFoundException("File " + fireFilePath + " could not be find in Fire");
                case HttpURLConnection.HTTP_FORBIDDEN:
                    throw new FireConfigurationException("Wrong credentials");
                default:
                    logger.info("Fire Direct returned {}", connection.getResponseCode());
                    throw new ParseException("Response code not known '" + connection.getResponseCode() + "'", 0);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private FireDirectFile getFileOnS3OrFirstOne(List<FireDirectFile> fileLinks) {
        logger.info("Found {} file source(s) in fire", fileLinks.size());
        for (FireDirectFile fileLink : fileLinks) {
            if (fileLink.isStorageS3()) {
                return fileLink;
            }
        }
        if (!fileLinks.isEmpty()) {
            return fileLinks.get(0);
        }
        return null;
    }

    private HttpURLConnection prepareConnection(String fireFilePath, URL url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(10000);
        con.setRequestMethod("GET");
        con.setRequestProperty("X-FIRE-Archive", "ega");
        con.setRequestProperty("X-FIRE-Key", fireProperties.getKey());
        con.setRequestProperty("X-FIRE-FilePath", normalizeFileNameForDirect(fireFilePath));
        return con;
    }

    private String normalizeFileNameForDirect(String fileName) {
        if (fileName.startsWith("/fire/A/ega/vol1/")) {
            return fileName.replace("/fire/A/ega/vol1/", "");
        }
        return fileName;
    }
}
