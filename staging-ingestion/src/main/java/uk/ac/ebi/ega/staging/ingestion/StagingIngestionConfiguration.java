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
package uk.ac.ebi.ega.staging.ingestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import uk.ac.ebi.ega.ingestion.commons.messages.NewFileEvent;
import uk.ac.ebi.ega.staging.ingestion.listener.StagingIngestionListener;
import uk.ac.ebi.ega.staging.ingestion.service.StagingIngestionService;
import uk.ac.ebi.ega.staging.ingestion.service.StagingIngestionServiceImpl;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StagingIngestionConfiguration {

    @Value("${staging.ingestion.internal.area}")
    private String internalArea;

    @Value("${new.file.queue}")
    private String newFileQueue;

    @Bean
    public StagingIngestionListener listener(StagingIngestionService service,
                                             KafkaTemplate<String, NewFileEvent> kafkaTemplate) {
        return new StagingIngestionListener(service, newFileQueue, kafkaTemplate);
    }

    @Bean
    public StagingIngestionService stagingIngestionService() throws FileNotFoundException {
        final Path path = Paths.get(internalArea);
        if (!path.toFile().exists()) {
            throw new FileNotFoundException(path.toString());
        }
        return new StagingIngestionServiceImpl(path);
    }

}
