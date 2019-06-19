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

import uk.ac.ebi.ega.file.encryption.processor.models.EncryptJobParameters;
import uk.ac.ebi.ega.file.encryption.processor.persistence.entity.EncryptParametersEntity;
import uk.ac.ebi.ega.file.encryption.processor.persistence.repository.EncryptParametersRepository;
import uk.ac.ebi.ega.jobs.core.services.JobParameterService;

import java.io.File;
import java.util.Optional;

public class EncryptJobParameterService implements JobParameterService<EncryptJobParameters> {

    private EncryptParametersRepository repository;

    public EncryptJobParameterService(EncryptParametersRepository repository) {
        this.repository = repository;
    }

    @Override
    public void persist(String jobId, EncryptJobParameters jobParameters) {

        repository.save(new EncryptParametersEntity(jobId, jobParameters.getAccountId(),
                jobParameters.getStagingId(), jobParameters.getFilePath().toString(), jobParameters.getLastUpdate(), jobParameters.getMd5FilePath().toString()));
    }

    @Override
    public Optional<EncryptJobParameters> getParameters(String jobId) {

        final Optional<EncryptParametersEntity> optional = repository.findById(jobId);

        if (optional.isPresent()) {
            final EncryptParametersEntity jobParameters = optional.get();
            final File filePath = new File(jobParameters.getFilePath());
            final File md5FilePath = new File(jobParameters.getMd5FilePath());

            return Optional.of(new EncryptJobParameters(jobParameters.getAccountId(), jobParameters.getStagingId(),
                    filePath.toPath(), jobParameters.getSize(), jobParameters.getLastUpdate(), md5FilePath.toPath()));
        }
        return Optional.empty();
    }
}
