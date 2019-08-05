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
package uk.ac.ebi.ega.cmdline.fire.archiver;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.ac.ebi.ega.cmdline.fire.archiver.utils.IStableIdGenerator;
import uk.ac.ebi.ega.cmdline.fire.archiver.utils.StableIdGenerator;
import uk.ac.ebi.ega.fire.ingestion.service.IFireService;
import uk.ac.ebi.ega.fire.ingestion.service.IProFilerDatabaseService;
import uk.ac.ebi.ega.fire.ingestion.service.OldFireService;
import uk.ac.ebi.ega.fire.ingestion.service.ProFilerDatabaseService;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class CmdLineFireArchiverConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "ega.cmdline.fire.archiver.config")
    public CmdLineFireArchiverProperties archiverProperties() {
        return new CmdLineFireArchiverProperties();
    }

    @Bean
    public IStableIdGenerator stableIdGenerator(final CmdLineFireArchiverProperties archiverProperties) {
        final String stableIdPrefix = archiverProperties.getStableIdPrefix();
        return new StableIdGenerator(stableIdPrefix);
    }

    @Bean
    public CommandLineRunner commandLineRunner(final ApplicationContext applicationContext,
                                               final IFireService fireService,
                                               final IStableIdGenerator stableIdGenerator) {
        return new FireArchiverCommandLineRunner(applicationContext, fireService, stableIdGenerator);
    }

    @Bean
    public IProFilerDatabaseService proFilerDatabaseService(final NamedParameterJdbcTemplate proFilerJdbcTemplate) {
        return new ProFilerDatabaseService(proFilerJdbcTemplate);
    }

    @Bean
    public IFireService fireService(final IProFilerDatabaseService proFilerDatabaseService,
                                    final CmdLineFireArchiverProperties properties) {
        final Path fireStaging = Paths.get(properties.getStagingPath());
        return new OldFireService(fireStaging, proFilerDatabaseService);
    }

}
