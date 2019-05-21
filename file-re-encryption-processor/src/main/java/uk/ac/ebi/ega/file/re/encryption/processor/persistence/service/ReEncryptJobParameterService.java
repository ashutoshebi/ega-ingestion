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
package uk.ac.ebi.ega.file.re.encryption.processor.persistence.service;

import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.services.JobParameterService;
import uk.ac.ebi.ega.file.re.encryption.processor.models.ReEncryptJobParameters;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.entity.ReEncryptParametersEntity;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.repository.ReEncryptParametersRepository;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;

import java.util.Optional;

public class ReEncryptJobParameterService implements JobParameterService<ReEncryptJobParameters> {

    private ReEncryptParametersRepository repository;

    private IPasswordEncryptionService passwordService;

    public ReEncryptJobParameterService(ReEncryptParametersRepository repository, IPasswordEncryptionService passwordService) {
        this.repository = repository;
        this.passwordService = passwordService;
    }

    @Override
    public void persist(String jobId, ReEncryptJobParameters jobParameters) {
        repository.save(new ReEncryptParametersEntity(jobId, jobParameters.getResultPath(),
                jobParameters.getDosId(), jobParameters.getEncryptedPassword()));
    }

    @Override
    public Optional<ReEncryptJobParameters> getParameters(String jobId) {
        final Optional<ReEncryptParametersEntity> optional = repository.findById(jobId);
        if (optional.isPresent()) {
            final ReEncryptParametersEntity jobParameters = optional.get();
            return Optional.of(new ReEncryptJobParameters(passwordService, jobParameters.getDosId(),
                    jobParameters.getResultPath(), jobParameters.getEncryptedPassword()));
        }
        return Optional.empty();
    }

}
