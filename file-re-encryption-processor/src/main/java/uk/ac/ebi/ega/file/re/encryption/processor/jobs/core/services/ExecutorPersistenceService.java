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
package uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.services;

import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.JobExecution;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.JobParameters;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;

import java.util.Optional;

public interface ExecutorPersistenceService {

    <T extends JobParameters> void assignExecution(String jobId, String jobName, T jobParameters);

    void saveResult(String jobId, Result execute);

    <T extends JobParameters> Optional<JobExecution<T>> getAssignedExecution(String jobName, Class<T> parameterClass);
}
