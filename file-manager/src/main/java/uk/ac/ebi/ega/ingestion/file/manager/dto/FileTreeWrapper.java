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

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileTreeWrapper extends ResourceSupport {

    private FileTreeBoxDTO file;
    private FileTreeBoxDTO folder;

    public FileTreeWrapper() {
        super();
    }

    public FileTreeWrapper(final FileTreeBoxDTO file,
                           final FileTreeBoxDTO folder) {
        super();
        this.file = file;
        this.folder = folder;
    }

    public FileTreeBoxDTO getFile() {
        return file;
    }

    public FileTreeBoxDTO getFolder() {
        return folder;
    }
}
