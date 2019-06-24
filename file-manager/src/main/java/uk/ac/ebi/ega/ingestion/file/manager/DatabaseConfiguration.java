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

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.event.ValidatingRepositoryEventListener;
import org.springframework.data.rest.core.mapping.ExposureConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.DownloadBoxFileJob;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.DownloadBoxJob;
import uk.ac.ebi.ega.ingestion.file.manager.utils.CustomDataSourceInitialization;
import uk.ac.ebi.ega.ingestion.file.manager.validator.DownloadBoxAssignationValidator;
import uk.ac.ebi.ega.ingestion.file.manager.validator.DownloadBoxJobValidator;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import static org.springframework.http.HttpMethod.*;

@Configuration
@EnableJpaRepositories(
        transactionManagerRef = "fileManager_transactionManager")
@EnableJpaAuditing
public class DatabaseConfiguration {

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Bean("fileManager_datasource_properties")
    @ConfigurationProperties("datasource.file-manager")
    public DataSourceProperties fileManagerDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean("fileManager_datasource")
    @Primary
    @ConfigurationProperties("datasource.file-manager.hikari")
    public DataSource fileManagerDataSource() {
        return fileManagerDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean("fileManager_transactionManager")
    public JpaTransactionManager fileManagerTransactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    public CustomDataSourceInitialization customDatasourceInitializer() {
        return new CustomDataSourceInitialization(fileManagerDataSource(), fileManagerDataSourceProperties(),
                applicationContext);
    }

    @Bean("pea_datasource_properties")
    @ConfigurationProperties("datasource.pea")
    public DataSourceProperties peaDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean("pea_datasource")
    @ConfigurationProperties("datasource.pea.hikari")
    public DataSource peaDataSource() {
        return peaDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean("pea_jdbc_template")
    public NamedParameterJdbcTemplate peaJdbcTemplate() {
        return new NamedParameterJdbcTemplate(peaDataSource());
    }

    @Bean("pea_transaction_manager")
    public DataSourceTransactionManager peaTransactionManager() {
        return new DataSourceTransactionManager(peaDataSource());
    }

    // FIRE Database configuration - Start

    @Bean("fire_datasource_properties")
    @ConfigurationProperties("datasource.fire")
    public DataSourceProperties fireDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean("fire_datasource")
    @ConfigurationProperties("datasource.fire.hikari")
    public DataSource fireDataSource() {
        return fireDataSourceProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean("fire_jdbc_template")
    public NamedParameterJdbcTemplate fireJdbcTemplate() {
        return new NamedParameterJdbcTemplate(fireDataSource());
    }

    @Bean("fire_transaction_manager")
    public DataSourceTransactionManager fireTransactionManager() {
        return new DataSourceTransactionManager(fireDataSource());
    }

    // FIRE Database configuration - End

    /**
     * Combines both transaction manager & allows to rollback or commit both transactions
     * together based on outcome. If either of the fails then both will be rollBacked
     *
     * @param jpaTransactionManager FileManager's JPA Transaction manager
     * @param dataSourceTransactionManager Fire JDBCTemplate Transaction manager
     * @return ChainedTransactionManager combines both File Manager & Fire Transaction manager
     * @see ChainedTransactionManager
     */
    @Bean(name = "fileManagerFireChainedTransactionManager")
    public ChainedTransactionManager transactionManager(@Qualifier("fileManager_transactionManager") JpaTransactionManager jpaTransactionManager,
                                                        @Qualifier("fire_transaction_manager") DataSourceTransactionManager dataSourceTransactionManager) {
        return new ChainedTransactionManager(jpaTransactionManager, dataSourceTransactionManager);
    }

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