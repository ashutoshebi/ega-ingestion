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

import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.persistence.repository.JobExecutionRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.persistence.repository.JobRunRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.services.BasicExecutorPersistenceService;
import uk.ac.ebi.ega.file.re.encryption.processor.models.ReEncryptJobParameters;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.service.ReEncryptJobParameterService;

public class ReEncryptPersistenceService extends BasicExecutorPersistenceService {

    private static final String RE_ENCRYPT_JOB = "re-encrypt-job";

    public ReEncryptPersistenceService(JobExecutionRepository jobExecutionRepository,
                                       JobRunRepository jobRunRepository, String instanceId,
                                       ReEncryptJobParameterService reEncryptJobParameterService) {
        super(jobExecutionRepository, jobRunRepository, instanceId);
        registerJobParameterServices(RE_ENCRYPT_JOB, ReEncryptJobParameters.class, reEncryptJobParameterService);
    }

}
