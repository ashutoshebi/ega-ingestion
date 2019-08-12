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

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;
import uk.ac.ebi.ega.encryption.core.services.PasswordEncryptionService;
import uk.ac.ebi.ega.fire.ingestion.service.IFireService;
import uk.ac.ebi.ega.fire.ingestion.service.IProFilerDatabaseService;
import uk.ac.ebi.ega.fire.ingestion.service.OldFireService;
import uk.ac.ebi.ega.fire.ingestion.service.ProFilerDatabaseService;
import uk.ac.ebi.ega.ingestion.file.manager.kafka.message.DownloadBoxFileProcess;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.DownloadBoxFileJobRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.DownloadBoxJobRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.FileHierarchyRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.HistoricDownloadBoxFileJobRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.HistoricDownloadBoxJobRepository;
import uk.ac.ebi.ega.ingestion.file.manager.services.DatasetService;
import uk.ac.ebi.ega.ingestion.file.manager.services.DownloadBoxJobService;
import uk.ac.ebi.ega.ingestion.file.manager.services.FileManagerService;
import uk.ac.ebi.ega.ingestion.file.manager.services.IDatasetService;
import uk.ac.ebi.ega.ingestion.file.manager.services.IDownloadBoxJobService;
import uk.ac.ebi.ega.ingestion.file.manager.services.IFileManagerService;
import uk.ac.ebi.ega.ingestion.file.manager.services.IKeyGenerator;
import uk.ac.ebi.ega.ingestion.file.manager.services.IMailingService;
import uk.ac.ebi.ega.ingestion.file.manager.services.MailingService;
import uk.ac.ebi.ega.ingestion.file.manager.services.key.RandomKeyGenerator;

import javax.persistence.EntityManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileManagerConfiguration {

    @Value("${spring.kafka.download-box.queue.name}")
    private String downloadBoxQueueName;

    @Value("${file.manager.download.box.password.size}")
    private int passwordKeySize;

    @Value("${file.manager.encryption.password.encryption.key}")
    private char[] passwordEncryptionKey;

    @Value("${file.manager.mail.alert}")
    private String mailAlert;

    @Bean
    public IKeyGenerator keyGenerator() {
        return new RandomKeyGenerator(passwordKeySize);
    }

    @Bean
    public IMailingService mailingService(JavaMailSender javaMailSender,
                                          IPasswordEncryptionService passwordEncryptionService) {
        return new MailingService(javaMailSender, passwordEncryptionService, mailAlert);
    }

    @Bean
    public IDownloadBoxJobService downloadBoxJobService(DownloadBoxJobRepository downloadBoxJobRepository,
                                                        DownloadBoxFileJobRepository downloadBoxFileJobRepository,
                                                        HistoricDownloadBoxJobRepository historicBoxJobRepository,
                                                        HistoricDownloadBoxFileJobRepository historicBoxFileJobRepository,
                                                        IDatasetService datasetService, IMailingService mailingService,
                                                        KafkaTemplate<String, DownloadBoxFileProcess> kafkaTemplate) {
        return new DownloadBoxJobService(downloadBoxJobRepository, downloadBoxFileJobRepository,
                historicBoxJobRepository, historicBoxFileJobRepository,
                datasetService, mailingService, kafkaTemplate, downloadBoxQueueName);
    }

    @Bean
    public IDatasetService datasetService(@Qualifier("pea_jdbc_template") NamedParameterJdbcTemplate jdbcTemplate) {
        return new DatasetService(jdbcTemplate);
    }

    @Bean
    public IProFilerDatabaseService proFilerDatabaseService(@Qualifier("fire_jdbc_template")
                                                                    NamedParameterJdbcTemplate jdbcTemplate) {
        return new ProFilerDatabaseService(jdbcTemplate);
    }

    @Bean
    public IFireService fireIngestion(@Value("${fire.staging.path}") String fireStagingPath,
                                      IProFilerDatabaseService proFilerDatabaseService) throws FileNotFoundException {
        return new OldFireService(assertAndGetPath(fireStagingPath), proFilerDatabaseService);
    }

    private Path assertAndGetPath(String path) throws FileNotFoundException {
        final File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException(file.getAbsolutePath());
        }
        return file.toPath();
    }

    @Bean
    public IFileManagerService fileManagerService(IFireService fireIngestion,
                                                  @Value("${file.manager.fire.relative.path}") String fireBoxRelativePath,
                                                  FileHierarchyRepository fileHierarchyRepository, EntityManager entityManager) {
        return new FileManagerService(fireIngestion, Paths.get(fireBoxRelativePath), fileHierarchyRepository, entityManager);
    }

    @Bean
    public IPasswordEncryptionService passwordEncryptionService() {
        return new PasswordEncryptionService(passwordEncryptionKey);
    }
}
