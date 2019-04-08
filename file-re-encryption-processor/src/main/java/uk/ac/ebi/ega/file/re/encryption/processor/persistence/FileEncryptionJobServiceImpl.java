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
package uk.ac.ebi.ega.file.re.encryption.processor.persistence;

import uk.ac.ebi.ega.file.encryption.processor.models.FileEncryptionJob;
import uk.ac.ebi.ega.file.encryption.processor.persistence.repository.FileEncryptionJobImpl;
import uk.ac.ebi.ega.file.encryption.processor.persistence.repository.FileEncryptionJobImplRepository;
import uk.ac.ebi.ega.file.encryption.processor.persistence.repository.FileEncryptionLogFailed;
import uk.ac.ebi.ega.file.encryption.processor.persistence.repository.FileEncryptionLogFailedRepository;
import uk.ac.ebi.ega.file.encryption.processor.persistence.repository.FileEncryptionLogSuccessful;
import uk.ac.ebi.ega.file.encryption.processor.persistence.repository.FileEncryptionLogSuccessfulRepository;
import uk.ac.ebi.ega.file.encryption.processor.services.FileEncryptionJobService;
import uk.ac.ebi.ega.file.re.encryption.processor.models.FileEncryptionJob;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.repository.FileEncryptionJobImpl;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.repository.FileEncryptionLogFailedRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.repository.FileEncryptionLogSuccessful;
import uk.ac.ebi.ega.file.re.encryption.processor.persistence.repository.FileEncryptionLogSuccessfulRepository;
import uk.ac.ebi.ega.file.re.encryption.processor.services.FileEncryptionJobService;

import java.io.File;

public class FileEncryptionJobServiceImpl implements FileEncryptionJobService {

    private FileEncryptionJobImplRepository repository;

    private FileEncryptionLogSuccessfulRepository logSuccessfulRepository;

    private FileEncryptionLogFailedRepository logFailedRepository;

    public FileEncryptionJobServiceImpl(FileEncryptionJobImplRepository repository,
                                        FileEncryptionLogSuccessfulRepository logSuccessfulRepository,
                                        FileEncryptionLogFailedRepository logFailedRepository) {
        this.repository = repository;
        this.logSuccessfulRepository = logSuccessfulRepository;
        this.logFailedRepository = logFailedRepository;
    }

    @Override
    public FileEncryptionJob startJob(String instanceId, String account, String stagingAreaId, File filePath,
                                      File filePathMd5) {
        return repository.save(new FileEncryptionJobImpl(instanceId, account, stagingAreaId, filePath.getAbsolutePath(),
                filePathMd5.getAbsolutePath()));
    }

    @Override
    public void endJob(FileEncryptionJob fileEncryptionJob, String md5, String encryptedMd5) {
        repository.deleteById(fileEncryptionJob.getId());
        logSuccessfulRepository.save(new FileEncryptionLogSuccessful(fileEncryptionJob, md5, encryptedMd5));
    }

    @Override
    public void endJob(FileEncryptionJob fileEncryptionJob, Exception e) {
        repository.deleteById(fileEncryptionJob.getId());
        logFailedRepository.save(new FileEncryptionLogFailed(fileEncryptionJob, e));
    }

}
