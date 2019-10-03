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
package uk.ac.ebi.ega.ingestion.file.manager.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.springframework.hateoas.ResourceSupport;
import uk.ac.ebi.ega.ingestion.commons.models.FileStatus;
import uk.ac.ebi.ega.ingestion.file.manager.models.FileHierarchyModel;

import java.time.LocalDateTime;

@JsonInclude(Include.NON_NULL)
public class FileDTO extends ResourceSupport {

    private String accountId;

    private String locationId;

    private String name;

    private String plainMd5;

    private Long version;

    private Long plainSize;

    private FileStatus status;

    private LocalDateTime modifiedDate;

    public FileDTO() {
    }

    public FileDTO(FileHierarchyModel model) {
        super();
    }

    public String getAccountId() {
        return accountId;
    }

    public String getLocationId() {
        return locationId;
    }

    public String getName() {
        return name;
    }

    public String getPlainMd5() {
        return plainMd5;
    }

    public Long getVersion() {
        return version;
    }

    public Long getPlainSize() {
        return plainSize;
    }

    public FileStatus getStatus() {
        return status;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public static FileDTO fromModel(FileHierarchyModel model) {
        final FileDTO fileDTO = new FileDTO();
        fileDTO.accountId = model.getAccountId();
        fileDTO.locationId = model.getStagingAreaId();
        fileDTO.name = model.getName();
        fileDTO.plainMd5 = model.getFileDetails().getPlainMd5();
        fileDTO.version = model.getFileDetails().getVersion();
        fileDTO.plainSize = model.getFileDetails().getPlainSize();
        fileDTO.status = model.getFileDetails().getStatus();
        fileDTO.modifiedDate = model.getFileDetails().getUpdatedDate();
        return fileDTO;
    }

}
