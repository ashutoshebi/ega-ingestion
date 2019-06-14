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
package uk.ac.ebi.ega.file.re.encryption.processor.models;

import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.jobs.core.JobParameters;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;

public class ReEncryptJobParameters implements JobParameters {

    private IPasswordEncryptionService passwordService;

    private String dosId;

    private String resultPath;

    private String encryptedPassword;

    public ReEncryptJobParameters(IPasswordEncryptionService passwordService, String dosId, String resultPath,
                                  String encryptedPassword) {
        this.passwordService = passwordService;
        this.dosId = dosId;
        this.resultPath = resultPath;
        this.encryptedPassword = encryptedPassword;
    }

    public String getDosId() {
        return dosId;
    }

    public String getResultPath() {
        return resultPath;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public char[] getPassword() throws AlgorithmInitializationException {
        return passwordService.decrypt(encryptedPassword);
    }

}
