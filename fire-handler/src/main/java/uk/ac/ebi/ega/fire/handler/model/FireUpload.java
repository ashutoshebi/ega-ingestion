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
package uk.ac.ebi.ega.fire.handler.model;

/**
 * Temporary class to store file details to upload file to Fire. Can be moved to common library.
 */
public class FireUpload {
    private String fileToUploadPath;
    private String md5;
    private String firePath;

    public FireUpload() {
    }

    public FireUpload(final String fileToUploadPath, final String md5, final String firePath) {
        this.fileToUploadPath = fileToUploadPath;
        this.md5 = md5;
        this.firePath = firePath;
    }

    public String getFileToUploadPath() {
        return fileToUploadPath;
    }

    public void setFileToUploadPath(String fileToUploadPath) {
        this.fileToUploadPath = fileToUploadPath;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getFirePath() {
        return firePath;
    }

    public void setFirePath(String firePath) {
        this.firePath = firePath;
    }
}
