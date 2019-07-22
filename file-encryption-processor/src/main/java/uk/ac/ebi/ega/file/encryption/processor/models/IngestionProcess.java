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
package uk.ac.ebi.ega.file.encryption.processor.models;

import uk.ac.ebi.ega.encryption.core.utils.io.FileUtils;
import uk.ac.ebi.ega.file.encryption.processor.exceptions.SkipIngestionException;
import uk.ac.ebi.ega.file.encryption.processor.pipelines.exceptions.SystemErrorException;
import uk.ac.ebi.ega.file.encryption.processor.utils.FileToProcess;
import uk.ac.ebi.ega.ingestion.commons.messages.IngestionEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class IngestionProcess {

    private String accountId;

    private String locationId;

    private FileToProcess encryptedFile;

    private FileToProcess plainMd5File;

    private FileToProcess encryptedMd5File;

    private File outputFile;

    public IngestionProcess(String accountId, String locationId, FileToProcess encryptedFile,
                            FileToProcess plainMd5File, FileToProcess encryptedMd5File, File outputFile) {
        this.accountId = accountId;
        this.locationId = locationId;
        this.encryptedFile = encryptedFile;
        this.plainMd5File = plainMd5File;
        this.encryptedMd5File = encryptedMd5File;
        this.outputFile = outputFile;
    }

    public IngestionProcess(String jobId, IngestionEvent data, Path stagingRoot) {
        this(
                data.getAccountId(),
                data.getLocationId(),
                new FileToProcess(data.getRootPath(), data.getEncryptedFile(),
                        stagingRoot.resolve(jobId + ".gpg")),
                new FileToProcess(data.getRootPath(), data.getPlainMd5File(),
                        stagingRoot.resolve(jobId + ".md5")),
                new FileToProcess(data.getRootPath(), data.getEncryptedMd5File(),
                        stagingRoot.resolve(jobId + ".gpg.md5")),
                stagingRoot.resolve(jobId + ".cip").toFile());
    }

    public String getAccountId() {
        return accountId;
    }

    public String getLocationId() {
        return locationId;
    }

    public FileToProcess getEncryptedFile() {
        return encryptedFile;
    }

    public FileToProcess getPlainMd5File() {
        return plainMd5File;
    }

    public FileToProcess getEncryptedMd5File() {
        return encryptedMd5File;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void moveFilesToStaging() throws SkipIngestionException, SystemErrorException {
        this.encryptedFile.assertFileHasNotChangedOrMoved();
        this.plainMd5File.assertFileHasNotChangedOrMoved();
        this.encryptedMd5File.assertFileHasNotChangedOrMoved();
        this.encryptedFile.moveFileToStaging();
        this.plainMd5File.moveFileToStaging();
        this.encryptedMd5File.moveFileToStaging();
    }

    public void rollback() {
        this.encryptedFile.rollbackFileToStaging();
        this.plainMd5File.rollbackFileToStaging();
        this.encryptedMd5File.rollbackFileToStaging();
    }

    public void rollbackEncryptedFileDeleteMd5s() {
        this.encryptedFile.rollbackFileToStaging();
        this.plainMd5File.deleteStagingFile();
        this.encryptedMd5File.deleteStagingFile();
    }

    public void deleteFiles() {
        this.encryptedFile.deleteStagingFile();
        this.plainMd5File.deleteStagingFile();
        this.encryptedMd5File.deleteStagingFile();
    }

    public String getEncryptedMd5() throws IOException {
        return getMd5(encryptedMd5File);
    }

    public String getPlainMd5() throws IOException {
        return getMd5(plainMd5File);
    }

    private static String getMd5(FileToProcess file) throws IOException {
        if(file.getFile().exists()){
            return new String(FileUtils.readPasswordFile(file.getFile().toPath()));
        }else{
            return new String(FileUtils.readPasswordFile(file.getStagingFile().toPath()));
        }
    }
}
