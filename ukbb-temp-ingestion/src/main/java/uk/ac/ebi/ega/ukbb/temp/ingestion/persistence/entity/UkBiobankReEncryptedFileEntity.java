/*
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
 */
package uk.ac.ebi.ega.ukbb.temp.ingestion.persistence.entity;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uk.ac.ebi.ega.ukbb.temp.ingestion.exceptions.TerminateProgramException;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Class representing the ukbiobank.re_encrypted_files database table.
 */

@Entity
@Table(schema = "UKBIOBANK", name = "RE_ENCRYPTED_FILES")
@EntityListeners(AuditingEntityListener.class)
public class UkBiobankReEncryptedFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "re_encrypted_file_id_seq")
    @SequenceGenerator(name = "re_encrypted_file_id_seq",
            schema = "UKBIOBANK",
            sequenceName = "re_encrypted_files_re_encrypted_file_id_seq",
            allocationSize = 1)
    @Column(name = "re_encrypted_file_id")
    private long reEncryptedFileId;

    // The path of the original, encrypted file.
    // "original_file_path REFERENCES ukbiobank.files(file_path)"
    private String originalFilePath;

    // The path of the newly re-encrypted file.
    private String newReEncryptedFilePath;

    // The MD5 of the original, encrypted file.
    private String originalEncryptedMd5;

    // The MD5 of the original, encrypted file, after it has been decrypted.
    // "unencryptedMd5 REFERENCES ukbiobank.files(md5_checksum)"
    private String unencryptedMd5;

    // The MD5 of the newly re-encrypted file.
    private String newReEncryptedMd5;

    private long unencryptedSize;

    private Long fireId;

    private String resultStatusMessage;

    private String resultStatusException;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    UkBiobankReEncryptedFileEntity() {
    }

    public UkBiobankReEncryptedFileEntity(Path originalFilePath) {
        this.originalFilePath = originalFilePath.toString();
        start();
    }

    public boolean isFinishedSuccessfully() {
        return resultStatusMessage == null && endTime != null;
    }

    public long getReEncryptedFileId() {
        return reEncryptedFileId;
    }

    public void finish(Path outputFilePath, String originalEncryptedMd5, String unencryptedMd5,
                       String newReEncryptedMd5, long unencryptedSize, Long fireId) {
        this.newReEncryptedFilePath = outputFilePath.toString();
        this.resultStatusMessage = null;
        this.resultStatusException = null;
        this.originalEncryptedMd5 = originalEncryptedMd5;
        this.unencryptedMd5 = unencryptedMd5;
        this.unencryptedSize = unencryptedSize;
        this.fireId = fireId;
        this.newReEncryptedMd5 = newReEncryptedMd5;
        this.endTime = LocalDateTime.now();
    }

    public void finish(TerminateProgramException e) {
        StringWriter stringWriter = new StringWriter();
        e.printStackTrace(new PrintWriter(stringWriter));
        this.resultStatusMessage = e.getMessage();
        this.resultStatusException = stringWriter.toString();
        this.endTime = LocalDateTime.now();
    }

    public void start() {
        this.resultStatusMessage = "processing";
        this.startTime = LocalDateTime.now();
    }
}
