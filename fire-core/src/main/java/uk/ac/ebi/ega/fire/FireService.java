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
import uk.ac.ebi.ega.fire.metadata.FileMetadataParser;
import uk.ac.ebi.ega.fire.properties.FireProperties;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.List;

public class FireService {

    private static final Logger logger = LoggerFactory.getLogger(FireService.class);

    private FireProperties fireProperties;

    public FireService(FireProperties fireProperties) {
        this.fireProperties = fireProperties;
    }

    public IFireFile getFile(String fireFilePath, boolean useFireDirect) throws FileNotFoundException {
        if (useFireDirect) {
            HttpURLConnection connection = null;
            try {
                connection = prepareConnection(fireFilePath, new URL(fireProperties.getUrl()));
                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    FireDirectFile file = getFileOnS3OrFirstOne(FileMetadataParser.parse(connection.getInputStream()));
                    if (file != null) {
                        logger.info("Used file source storage is {}", file.getStorageClass());
                        return file;
                    }
                } else {
                    logger.info("Fire Direct returned {}", connection.getResponseCode());
                }
            } catch (IOException | ParseException e) {
                logger.error(e.getMessage(), e);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        } else {
            return new FuseFireFile(fireProperties.getMountPath(), fireFilePath);
        }
        throw new FileNotFoundException("File " + fireFilePath + " could not be accessed " +
                "through Fire Direct");
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
