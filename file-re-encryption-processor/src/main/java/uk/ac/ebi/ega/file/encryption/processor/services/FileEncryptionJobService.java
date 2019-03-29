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

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.ega.file.encryption.processor.models.FileEncryptionJob;

import java.io.File;

public interface FileEncryptionJobService {

    @Transactional(isolation = Isolation.SERIALIZABLE)
    FileEncryptionJob startJob(String instanceId, String account, String stagingAreaId, File filePath,
                               File filePathMd5);

    @Transactional
    void endJob(FileEncryptionJob fileEncryptionJob, Exception e);

    @Transactional
    void endJob(FileEncryptionJob fileEncryptionJob, String md5, String encryptedMd5);

}
