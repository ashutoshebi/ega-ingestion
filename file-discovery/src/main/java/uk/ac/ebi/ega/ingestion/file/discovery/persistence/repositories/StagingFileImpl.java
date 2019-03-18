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
package uk.ac.ebi.ega.ingestion.file.discovery.persistence.repositories;


import uk.ac.ebi.ega.ingestion.file.discovery.message.FileEvent;
import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingFile;
import uk.ac.ebi.ega.ingestion.file.discovery.utils.Hash;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.xml.bind.DatatypeConverter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity(name = "STAGING_AREA_FILES")
public class StagingFileImpl implements StagingFile {

    @Id
    private String id;

    private String stagingAreaId;

    private String relativePath;

    private long fileSize;

    private LocalDateTime updateDate;

    protected StagingFileImpl() {
    }

    public StagingFileImpl(String id, String stagingAreaId, String relativePath, long fileSize,
                           LocalDateTime updateDate) {
        this.id = id;
        this.stagingAreaId = stagingAreaId;
        this.relativePath = relativePath;
        this.fileSize = fileSize;
        this.updateDate = updateDate;
    }

    public StagingFileImpl(String stagingAreaId, String relativePath, long fileSize, LocalDateTime updateDate) {
        this(calculateId(stagingAreaId, relativePath), stagingAreaId, relativePath, fileSize, updateDate);
    }

    public StagingFileImpl(FileEvent fileEvent) {
        this(calculateId(fileEvent.getLocationId(), fileEvent.getRelativePath()), fileEvent.getLocationId(),
                fileEvent.getRelativePath(), fileEvent.getSize(),
                Instant.ofEpochMilli(fileEvent.getLastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime());
    }

    private static String calculateId(String stagingAreaId, String relativePath) {
        return (DatatypeConverter.printHexBinary(Hash.getMd5().digest(stagingAreaId.getBytes())) +
                DatatypeConverter.printHexBinary(Hash.getSha256().digest(relativePath.getBytes()))).toLowerCase();
    }

    public String getId() {
        return id;
    }

    public String getStagingAreaId() {
        return stagingAreaId;
    }

    @Override
    public String getRelativePath() {
        return relativePath;
    }

    @Override
    public Long getFileSize() {
        return fileSize;
    }

    @Override
    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

}