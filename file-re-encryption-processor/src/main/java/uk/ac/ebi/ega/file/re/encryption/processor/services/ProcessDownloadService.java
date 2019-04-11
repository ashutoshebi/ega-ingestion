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
package uk.ac.ebi.ega.file.re.encryption.processor.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.file.re.encryption.processor.messages.DownloadBoxFileProcess;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.entity.HistoricProcessDownloadBoxFile;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.entity.ProcessDownloadBoxFile;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.repository.HistoricProcessDownloadBoxFileRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.repository.ProcessDownloadBoxFileRepository;

import java.time.LocalDateTime;

public class ProcessDownloadService implements ProcessService {

    private static final Logger logger = LoggerFactory.getLogger(ProcessDownloadService.class);

    private String instanceId;

    private ProcessDownloadBoxFileRepository repository;

    private HistoricProcessDownloadBoxFileRepository historicRepository;

    public ProcessDownloadService(String instanceId, ProcessDownloadBoxFileRepository processDownloadBoxFileRepository) {
        this.instanceId = instanceId;
        this.repository = processDownloadBoxFileRepository;
    }

    @Override
    public void lock(String key, DownloadBoxFileProcess data) {
        repository.save(new ProcessDownloadBoxFile(LocalDateTime.now().toString(), instanceId, data.getResultPath(),
                data.getDosId(),
                data.getPassword()));
        repository.save(new ProcessDownloadBoxFile(key, instanceId, data.getResultPath(), data.getDosId(), data.getPassword()));
    }

    @Override
    public void unlock(String key) {
        final ProcessDownloadBoxFile process = repository.findById(key).orElseThrow(RuntimeException::new);
        historicRepository.save(new HistoricProcessDownloadBoxFile(process.getId(), instanceId,
                process.getResultPath(), process.getDosId()));
        repository.deleteById(key);
    }

}
