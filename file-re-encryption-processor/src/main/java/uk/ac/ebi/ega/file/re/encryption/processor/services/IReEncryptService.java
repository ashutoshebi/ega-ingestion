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

import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.JobExecution;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.Result;
import uk.ac.ebi.ega.file.re.encryption.processor.jobs.core.exceptions.JobNotRegistered;
import uk.ac.ebi.ega.file.re.encryption.processor.models.ReEncryptJobParameters;

import java.util.Optional;

public interface IReEncryptService {

    Optional<JobExecution<ReEncryptJobParameters>> createJob(String id, String dosId, String resultPath,
                                                             String encryptedPassword) throws JobNotRegistered;

    Result reEncrypt(JobExecution<ReEncryptJobParameters> jobExecution);

    Optional<JobExecution<ReEncryptJobParameters>> getUnfinishedJob();

}
