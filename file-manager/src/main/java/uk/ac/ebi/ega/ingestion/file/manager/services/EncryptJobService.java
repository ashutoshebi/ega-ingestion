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
package uk.ac.ebi.ega.ingestion.file.manager.services;

import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.ega.fire.ingestion.model.FireIngestionModel;
import uk.ac.ebi.ega.fire.ingestion.service.IFireIngestion;
import uk.ac.ebi.ega.fire.ingestion.service.IFireIngestionModelMapper;
import uk.ac.ebi.ega.ingestion.file.manager.kafka.message.EncryptComplete;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.EncryptJob;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.EncryptJobRepository;

public class EncryptJobService implements IEncryptJobService {

    private final EncryptJobRepository encryptJobRepository;
    private final IFireIngestion fireIngestion;
    private final IFireIngestionModelMapper<EncryptComplete> fireIngestionModelMapper;

    public EncryptJobService(final EncryptJobRepository encryptJobRepository,
                             final IFireIngestion fireIngestion,
                             final IFireIngestionModelMapper<EncryptComplete> fireIngestionModelMapper) {
        this.encryptJobRepository = encryptJobRepository;
        this.fireIngestion = fireIngestion;
        this.fireIngestionModelMapper = fireIngestionModelMapper;
    }

    @Transactional(transactionManager = "fileManagerFireChainedTransactionManager",
            rollbackFor = Exception.class)
    @Override
    public void notify(final String jobId, final EncryptComplete encryptComplete) {

        // Persist data received on message queue in FileManager database
        final EncryptJob encryptJob = EncryptJob.newInstance(jobId, encryptComplete);
        persistEncryptJob(encryptJob);

        // Add entries into FIRE database
        final FireIngestionModel fireIngestionModel = fireIngestionModelMapper.map(encryptComplete);
        fireIngestion.ingest(fireIngestionModel);
    }

    private void persistEncryptJob(final EncryptJob encryptJob) {
        encryptJobRepository.save(encryptJob);
    }
}
