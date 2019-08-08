/*
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
 */
package uk.ac.ebi.ega.cmdline.fire.re.archiver;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.ac.ebi.ega.cmdline.fire.re.archiver.services.IReEncryptionService;
import uk.ac.ebi.ega.cmdline.fire.re.archiver.services.ReEncryptionService;
import uk.ac.ebi.ega.cmdline.fire.re.archiver.utils.IStableIdGenerator;
import uk.ac.ebi.ega.cmdline.fire.re.archiver.utils.StableIdGenerator;
import uk.ac.ebi.ega.file.encryption.processor.jobs.EncryptJob;
import uk.ac.ebi.ega.file.encryption.processor.models.IngestionProcess;
import uk.ac.ebi.ega.file.encryption.processor.services.EncryptService;
import uk.ac.ebi.ega.file.encryption.processor.services.IPasswordGeneratorService;
import uk.ac.ebi.ega.file.encryption.processor.services.PasswordGeneratorService;
import uk.ac.ebi.ega.fire.ingestion.service.IFireService;
import uk.ac.ebi.ega.fire.ingestion.service.IProFilerDatabaseService;
import uk.ac.ebi.ega.fire.ingestion.service.OldFireService;
import uk.ac.ebi.ega.fire.ingestion.service.ProFilerDatabaseService;
import uk.ac.ebi.ega.jobs.core.Job;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class CmdLineFireReArchiverConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "ega.cmdline.fire.archiver.config")
    public CmdLineFireReArchiverProperties archiverProperties() {
        return new CmdLineFireReArchiverProperties();
    }

    @Bean
    public IStableIdGenerator stableIdGenerator(final CmdLineFireReArchiverProperties archiverProperties) {
        final String stableIdPrefix = archiverProperties.getStableIdPrefix();
        return new StableIdGenerator(stableIdPrefix);
    }

    @Bean
    public CommandLineRunner commandLineRunner(final ApplicationContext applicationContext,
                                               final IFireService fireService,
                                               final IStableIdGenerator stableIdGenerator,
                                               final IReEncryptionService reEncryptionService) {
        return new CmdLineFireReArchiverCommandLineRunner(applicationContext, fireService, stableIdGenerator, reEncryptionService);
    }

    @Bean
    public IProFilerDatabaseService proFilerDatabaseService(final NamedParameterJdbcTemplate proFilerJdbcTemplate) {
        return new ProFilerDatabaseService(proFilerJdbcTemplate);
    }

    @Bean
    public IFireService fireService(final IProFilerDatabaseService proFilerDatabaseService,
                                    final CmdLineFireReArchiverProperties properties) {
        final Path fireStaging = Paths.get(properties.getStagingPath());
        return new OldFireService(fireStaging, proFilerDatabaseService);
    }

    @Bean
    public IReEncryptionService reEncryptionService() {
        // TODO bjuhasz
        return new ReEncryptionService(null, null, null);
    }

    ///////////// TODO bjuhasz:

    @Bean
    public Job<IngestionProcess> encryptJob(@Value("${file.encryption.keyring.private}") String privateKeyRing,
                                            @Value("${file.encryption.keyring.private.key}") String privateKeyRingPassword,
                                            IPasswordGeneratorService passwordGeneratorService) throws IOException {
        final File privateKeyRingFile = new File(privateKeyRing);
        if (!privateKeyRingFile.exists()) {
            throw new FileNotFoundException("Private key ring file could not be found");
        }
        final File privateKeyRingPasswordFile = new File(privateKeyRingPassword);
        if (!privateKeyRingPasswordFile.exists()) {
            throw new FileNotFoundException("Password file for private key ring could not be found");
        }

        return new EncryptJob(privateKeyRingFile, privateKeyRingPasswordFile,
                passwordGeneratorService);
    }

    @Bean
    public IPasswordGeneratorService passwordGeneratorService(@Value("${file.encryption.static.key}") String encryptionKeyPath)
            throws IOException {
        final File encryptPasswordFile = new File(encryptionKeyPath);
        if (!encryptPasswordFile.exists()) {
            throw new FileNotFoundException("Password file to encrypt output file could not be found");
        }
        return new PasswordGeneratorService(encryptPasswordFile);
    }

    @Bean
    public EncryptService encryptService(@Value("${file.encryption.staging.root}") String staging,
                                         Job<IngestionProcess> job) throws FileNotFoundException {
        final File stagingRoot = new File(staging);

        if (!stagingRoot.exists()) {
            throw new FileNotFoundException("Staging path for encryption is not found");
        }

/*
        return new EncryptService(stagingRoot.toPath(), executorPersistenceService, job,
                kafkaTemplate, completedTopic, delayConfiguration);
*/
        return null;
    }
}
