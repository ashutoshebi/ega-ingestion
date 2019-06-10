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
package uk.ac.ebi.ega.ukbb.temp.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.ega.fire.FireService;
import uk.ac.ebi.ega.fire.IFireService;
import uk.ac.ebi.ega.fire.properties.FireProperties;

import java.net.MalformedURLException;

//@Configuration
public class FireConfiguration {

    @Bean
    @ConfigurationProperties(prefix = "fire")
    public FireProperties fireProperties() {
        return new FireProperties();
    }

    @Bean
    public IFireService fireService(final FireProperties fireProperties) throws MalformedURLException {
        return new FireService(fireProperties);
    }

}
