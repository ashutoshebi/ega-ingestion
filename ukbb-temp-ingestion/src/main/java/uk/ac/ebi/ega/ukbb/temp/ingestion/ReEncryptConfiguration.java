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

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.ac.ebi.ega.ukbb.temp.ingestion.properties.ReEncryptProperties;

import java.util.Optional;

@Configuration
public class ReEncryptConfiguration {

    // TODO bjuhasz: use this class in the UkbbTempIngestionApplication class
    //@Bean
    public CommandLineRunner clr() {
        return args -> {
            Optional<ReEncryptOptions> var = ReEncryptOptions.parse(args);
            if (var.isPresent()) {
                ReEncryptOptions options = var.get();
                if (options.isDataset()) {
                    //fileReEncryptService().reEncryptDataset(options.getEgaId());
                } else {
                    //fileReEncryptService().reEncryptFiles(options.getEgaId());
                }
            } else {
                System.exit(1);
            }
        };
    }

    @Bean
    @ConfigurationProperties(prefix = "ega.ukbb.temp.ingestion.config")
    public ReEncryptProperties reEncryptProperties() {
        return new ReEncryptProperties();
    }

}
