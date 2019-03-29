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
package uk.ac.ebi.ega.ingestion.file.manager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import uk.ac.ebi.ega.ingestion.file.manager.converter.BoxAssignationConverter;
import uk.ac.ebi.ega.ingestion.file.manager.validator.DownloadBoxAssignationValidator;

@Configuration
@EnableJpaRepositories
@EnableJpaAuditing
public class DatabaseConfiguration {

    @Bean
    public BoxAssignationConverter boxAssignationConverter() {
        return new BoxAssignationConverter();
    }

    @Bean
    public RepositoryRestConfigurer repositoryRestConfigurer() {
        return new RepositoryRestConfigurer() {

            @Override
            public void configureConversionService(ConfigurableConversionService conversionService) {
                conversionService.addConverter(boxAssignationConverter());
            }

            @Override
            public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener listener) {
                listener.addValidator("beforeCreate", new DownloadBoxAssignationValidator());
            }

        };
    }
}