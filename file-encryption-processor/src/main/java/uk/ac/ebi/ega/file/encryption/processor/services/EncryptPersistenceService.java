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
package uk.ac.ebi.ega.file.encryption.processor.services;

import uk.ac.ebi.ega.file.encryption.processor.models.EncryptJobParameters;
import uk.ac.ebi.ega.file.encryption.processor.persistent.services.EncryptJobParameterService;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.persistence.repository.JobExecutionRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.persistence.repository.JobRunRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.services.BasicExecutorPersistenceService;

public class EncryptPersistenceService extends BasicExecutorPersistenceService {

    private static final String ENCRYPT_JOB = "encrypt-job";

    public EncryptPersistenceService(JobExecutionRepository jobExecutionRepository,
                                     JobRunRepository jobRunRepository, String instanceId,
                                     EncryptJobParameterService encryptJobParameterService) {
        super(jobExecutionRepository, jobRunRepository, instanceId);
        registerJobParameterServices(ENCRYPT_JOB, EncryptJobParameters.class, encryptJobParameterService);
    }
}
