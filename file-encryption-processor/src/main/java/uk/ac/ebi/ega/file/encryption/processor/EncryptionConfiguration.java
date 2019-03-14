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
package uk.ac.ebi.ega.file.encryption.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.PipelineBuilder;
import uk.ac.ebi.ega.file.encryption.processor.services.FileEncryptionJobService;
import uk.ac.ebi.ega.file.encryption.processor.services.PipelineService;
import uk.ac.ebi.ega.file.encryption.processor.services.ProcessEncryptionFileService;
import uk.ac.ebi.ega.file.encryption.processor.services.ProcessEncryptionFileServiceImpl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

@Configuration
public class EncryptionConfiguration {

    @Bean
    public PipelineService ingestionPipelineService(
            @Value("${file.encryption.keyring.private}") String privateKeyRing,
            @Value("${file.encryption.keyring.private.key}") String privateKeyRingPassword)
            throws FileNotFoundException {
        File privateKeyRingFile = new File(privateKeyRing);
        if (!privateKeyRingFile.exists()) {
            throw new FileNotFoundException("Private key ring file could not be found");
        }
        File privateKeyRingPasswordFile = new File(privateKeyRingPassword);
        if (!privateKeyRingPasswordFile.exists()) {
            throw new FileNotFoundException("Password file for private key ring could not be found");
        }
        return new PipelineBuilder(privateKeyRingFile, privateKeyRingPasswordFile);
    }

    @Bean
    public ProcessEncryptionFileService processEncryptionFileService(
            @Autowired PipelineService ingestionPipelineService,
            @Autowired FileEncryptionJobService jobService,
            @Value("${spring.kafka.client-id}") String clientId,
            @Value("${file.encryption.staging.root}") String staging)
            throws IOException {
        File stagingRoot = new File(staging);
        if (!stagingRoot.exists()) {
            throw new FileNotFoundException("Staging path for encryption is not found");
        }
        return new ProcessEncryptionFileServiceImpl(jobService, ingestionPipelineService, clientId,
                stagingRoot.toPath());
    }

}
