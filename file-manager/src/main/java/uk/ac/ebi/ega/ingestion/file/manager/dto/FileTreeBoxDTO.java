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
import org.springframework.hateoas.ResourceSupport;

import java.util.Collection;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileTreeBoxDTO extends ResourceSupport {

    private String fileType;
    private Collection<FileTreeDTO> fileTreeDTOS;

    public FileTreeBoxDTO() {
        super();
    }

    public FileTreeBoxDTO(final String fileType, final Collection<FileTreeDTO> fileTreeDTOS) {
        super();
        this.fileType = fileType;
        this.fileTreeDTOS = fileTreeDTOS;
    }

    public String getFileType() {
        return fileType;
    }

    public Collection<FileTreeDTO> getFileTreeDTOS() {
        return fileTreeDTOS;
    }
}
