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
package uk.ac.ebi.ega.ingestion.file.discovery.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.ingestion.file.discovery.FileDiscoveryConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;

public class FileProcessesHandlerImpl implements FileProcessesHandler {

    private static final Logger logger = LoggerFactory.getLogger(FileDiscoveryConfiguration.class);

    @Override
    public void changeFileGroup(File file) throws IOException {
        UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
        UserPrincipal lockUser = lookupService.lookupPrincipalByName("jorizci");
        if ("/tmp/ingestion/box-4/file-06".equals(file.toString())) {
            Files.setOwner(file.toPath(), lockUser);
            logger.info("Mwa ha ha ha {}, {}", file.toString(), file.canRead());
        }else{
            logger.info(":( {}, {}", file.toString(), file.canRead());
        }
    }

}
