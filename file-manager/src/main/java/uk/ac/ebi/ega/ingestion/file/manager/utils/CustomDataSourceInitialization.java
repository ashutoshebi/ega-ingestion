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
package uk.ac.ebi.ega.ingestion.file.manager.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceSchemaCreatedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import javax.sql.DataSource;

public class CustomDataSourceInitialization implements ApplicationListener<DataSourceSchemaCreatedEvent>,
        InitializingBean {

    private static final Log logger = LogFactory.getLog(CustomDataSourceInitialization.class);

    private final DataSource dataSource;

    private final DataSourceProperties properties;

    private final ApplicationContext applicationContext;

    private CustomDataSourceInitializer dataSourceInitializer;

    private boolean initialized;

    public CustomDataSourceInitialization(DataSource dataSource,
                                          DataSourceProperties properties, ApplicationContext applicationContext) {
        this.dataSource = dataSource;
        this.properties = properties;
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() {
        CustomDataSourceInitializer initializer = getDataSourceInitializer();
        if (initializer != null) {
            boolean schemaCreated = this.dataSourceInitializer.createSchema();
            if (schemaCreated) {
                initialize(initializer);
            }
        }
    }

    private void initialize(CustomDataSourceInitializer initializer) {
        try {
            this.applicationContext.publishEvent(
                    new DataSourceSchemaCreatedEvent(initializer.getDataSource()));
            // The listener might not be registered yet, so don't rely on it.
            if (!this.initialized) {
                this.dataSourceInitializer.initSchema();
                this.initialized = true;
            }
        } catch (IllegalStateException ex) {
            logger.warn("Could not send event to complete DataSource initialization ("
                    + ex.getMessage() + ")");
        }
    }

    @Override
    public void onApplicationEvent(DataSourceSchemaCreatedEvent event) {
        // NOTE the event can happen more than once and
        // the event datasource is not used here
        CustomDataSourceInitializer initializer = getDataSourceInitializer();
        if (!this.initialized && initializer != null) {
            initializer.initSchema();
            this.initialized = true;
        }
    }

    private CustomDataSourceInitializer getDataSourceInitializer() {
        if (this.dataSourceInitializer == null) {
            if (dataSource != null) {
                this.dataSourceInitializer = new CustomDataSourceInitializer(dataSource, properties,
                        applicationContext);
            }
        }
        return this.dataSourceInitializer;
    }

}
