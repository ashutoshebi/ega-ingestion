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
package uk.ac.ebi.ega.ukbb.temp.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankFilesRepository;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.repository.UkBiobankReEncryptedFilesRepository;
import uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.services.JpaUkbbJobService;
import uk.ac.ebi.ega.ukbb.temp.ingestion.properties.ReEncryptProperties;
import uk.ac.ebi.ega.ukbb.temp.ingestion.reencryption.BaseReEncryptService;
import uk.ac.ebi.ega.ukbb.temp.ingestion.services.IReEncryptService;
import uk.ac.ebi.ega.ukbb.temp.ingestion.services.IUkbbJobService;
import uk.ac.ebi.ega.ukbb.temp.ingestion.services.IUkbbReEncryptProcessService;
import uk.ac.ebi.ega.ukbb.temp.ingestion.services.ProFilerService;
import uk.ac.ebi.ega.ukbb.temp.ingestion.services.UkbbReEncryptProcessService;

@Configuration
public class UkbbTempIngestionConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(UkbbTempIngestionApplication.class);

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext applicationContext,
                                               IUkbbReEncryptProcessService reEncryptProcessService) {
        return new IngestionCommandLineRunner(applicationContext, reEncryptProcessService);
    }

    @Bean
    public IUkbbReEncryptProcessService reEncryptProcessService(IReEncryptService reEncryptService,
                                                                IUkbbJobService ukbbJobService,
                                                                ProFilerService proFilerService,
                                                                ReEncryptProperties reEncryptProperties) {
        return new UkbbReEncryptProcessService(reEncryptService, ukbbJobService, proFilerService, reEncryptProperties);
    }

    @Bean
    public IUkbbJobService ukbbJobService(UkBiobankFilesRepository filesRepository,
                                          UkBiobankReEncryptedFilesRepository reEncryptedFilesRepository) {
        return new JpaUkbbJobService(filesRepository, reEncryptedFilesRepository);
    }

    @Bean
    public ProFilerService proFilerService(NamedParameterJdbcTemplate namedParameterJdbcTemplate) {
        return new ProFilerService(namedParameterJdbcTemplate);
    }

    @Bean
    @ConfigurationProperties(prefix = "ega.ukbb.temp.ingestion.config")
    public ReEncryptProperties reEncryptProperties() {
        return new ReEncryptProperties();
    }

    @Bean
    public IReEncryptService reEncryptService() {
        return new BaseReEncryptService();
    }

}
