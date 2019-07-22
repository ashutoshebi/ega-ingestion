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
package uk.ac.ebi.ega.file.encryption.processor.persistence.services;

import uk.ac.ebi.ega.file.encryption.processor.models.IngestionProcess;
import uk.ac.ebi.ega.file.encryption.processor.persistence.entity.EncryptParametersEntity;
import uk.ac.ebi.ega.file.encryption.processor.persistence.repository.EncryptParametersRepository;
import uk.ac.ebi.ega.file.encryption.processor.utils.FileToProcess;
import uk.ac.ebi.ega.jobs.core.services.JobParameterService;

import java.io.File;
import java.util.Optional;

public class EncryptJobParameterService implements JobParameterService<IngestionProcess> {

    private EncryptParametersRepository repository;

    public EncryptJobParameterService(EncryptParametersRepository repository) {
        this.repository = repository;
    }

    @Override
    public void persist(String jobId, IngestionProcess jobParameters) {
        repository.save(new EncryptParametersEntity(jobId, jobParameters));
    }

    @Override
    public Optional<IngestionProcess> getParameters(String jobId) {
        final Optional<EncryptParametersEntity> optional = repository.findById(jobId);
        if (optional.isPresent()) {
            final EncryptParametersEntity jobParameters = optional.get();
            return Optional.of(new IngestionProcess(
                    jobParameters.getAccountId(),
                    jobParameters.getStagingId(),
                    new FileToProcess(
                            new File(jobParameters.getGpgPath()),
                            new File(jobParameters.getGpgStagingPath()),
                            jobParameters.getGpgSize(),
                            jobParameters.getGpgLastModified()),
                    new FileToProcess(
                            new File(jobParameters.getMd5Path()),
                            new File(jobParameters.getMd5StagingPath()),
                            jobParameters.getMd5Size(),
                            jobParameters.getMd5LastModified()),
                    new FileToProcess(
                            new File(jobParameters.getGpgMd5Path()),
                            new File(jobParameters.getGpgMd5StagingPath()),
                            jobParameters.getGpgMd5Size(),
                            jobParameters.getGpgMd5LastModified()),
                    new File(jobParameters.getResultPath())
            ));
        }
        return Optional.empty();
    }
}
