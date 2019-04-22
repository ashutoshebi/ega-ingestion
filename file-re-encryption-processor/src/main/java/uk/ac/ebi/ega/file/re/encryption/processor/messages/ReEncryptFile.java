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
package uk.ac.ebi.ega.file.re.encryption.processor.messages;

public class ReEncryptFile {

    private String resultPath;

    private String dosId;

    private String password;

    public ReEncryptFile() {
    }

    public ReEncryptFile(String resultPath, String dosId, String password) {
        this.resultPath = resultPath;
        this.dosId = dosId;
        this.password = password;
    }

    public String getResultPath() {
        return resultPath;
    }

    public void setResultPath(String resultPath) {
        this.resultPath = resultPath;
    }

    public String getDosId() {
        return dosId;
    }

    public void setDosId(String dosId) {
        this.dosId = dosId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "ReEncryptFile{" +
                "resultPath='" + resultPath + '\'' +
                ", dosId='" + dosId + '\'' +
                ", password='******'" +
                '}';
    }

}
