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
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.core.mapping.ExposureConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.DownloadBoxFileJob;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.DownloadBoxJob;
import uk.ac.ebi.ega.ingestion.file.manager.validator.DownloadBoxAssignationValidator;
import uk.ac.ebi.ega.ingestion.file.manager.validator.DownloadBoxJobValidator;

import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;

@Configuration
@EnableJpaRepositories
@EnableJpaAuditing
public class DatabaseConfiguration {

    @Bean
    public RepositoryRestConfigurer repositoryRestConfigurer(LocalValidatorFactoryBean beanValidator) {
        return new RepositoryRestConfigurer() {

            @Override
            public void configureRepositoryRestConfiguration(RepositoryRestConfiguration repositoryRestConfiguration) {
                ExposureConfiguration config = repositoryRestConfiguration.getExposureConfiguration();
                config.disablePutForCreation();
                config.forDomainType(DownloadBoxJob.class)
                        .withItemExposure((metadata, httpMethods) -> httpMethods.disable(PATCH, DELETE));
                config.forDomainType(DownloadBoxFileJob.class)
                        .withItemExposure((metadata, httpMethods) -> httpMethods.disable(POST, PUT, PATCH, DELETE));
            }

            @Override
            public void configureValidatingRepositoryEventListener(ValidatingRepositoryEventListener listener) {
                listener.addValidator("beforeCreate", beanValidator);
                listener.addValidator("beforeCreate", new DownloadBoxAssignationValidator());
                listener.addValidator("beforeCreate", new DownloadBoxJobValidator());

                listener.addValidator("beforeSave", beanValidator);
                listener.addValidator("beforeSave", new DownloadBoxAssignationValidator());
                listener.addValidator("beforeSave", new DownloadBoxJobValidator());
            }

        };
    }
}