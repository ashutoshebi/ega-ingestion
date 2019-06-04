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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.ac.ebi.ega.database.commons.services.AuditService;
import uk.ac.ebi.ega.database.commons.services.PeaService;
import uk.ac.ebi.ega.database.commons.services.ProFilerService;
import uk.ac.ebi.ega.database.commons.services.ReEncryptService;
import uk.ac.ebi.ega.file.re.encrypt.properties.FileReEncryptProperties;
import uk.ac.ebi.ega.file.re.encrypt.services.FileReEncryptService;
import uk.ac.ebi.ega.file.re.encrypt.services.IKeyGenerator;
import uk.ac.ebi.ega.fire.FireService;
import uk.ac.ebi.ega.fire.properties.FireProperties;

import java.util.Optional;

@Configuration
public class FileReEncryptConfiguration {

    @Autowired
    @Qualifier("re_encrypt_jdbc_template")
    private NamedParameterJdbcTemplate reEncryptTemplate;

    @Autowired
    @Qualifier("pro_filer_jdbc_template")
    private NamedParameterJdbcTemplate proFilerTemplate;

    @Bean
    public CommandLineRunner clr() {
        return args -> {
            Optional<FileReEncryptOptions> var = FileReEncryptOptions.parse(args);
            if (var.isPresent()) {
                FileReEncryptOptions options = var.get();
                if (options.isDataset()) {
                    fileReEncryptService().reEncryptDataset(options.getEgaId());
                } else {
                    fileReEncryptService().reEncryptFiles(options.getEgaId());
                }
            } else {
                System.exit(1);
            }
        };
    }

    @Bean
    public FileReEncryptService fileReEncryptService() {
        return new FileReEncryptService(fileReEncryptProperties(), reEncryptService(), proFilerService(), peaService(),
                fireService(), keyGenerator);
    }

    @Bean
    public ReEncryptService reEncryptService() {
        return new ReEncryptService(reEncryptTemplate);
    }

    @Bean
    public ProFilerService proFilerService() {
        return new ProFilerService(proFilerTemplate);
    }

    @Bean
    public FireService fireService() {
        return new FireService(fireProperties());
    }

    @Bean
    @ConfigurationProperties(prefix = "file-re-encrypt.config")
    public FileReEncryptProperties fileReEncryptProperties() {
        return new FileReEncryptProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "file-re-encrypt.fire")
    public FireProperties fireProperties() {
        return new FireProperties();
    }

}
