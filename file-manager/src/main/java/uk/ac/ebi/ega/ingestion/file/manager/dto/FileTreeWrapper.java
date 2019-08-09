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

import java.util.ArrayList;
import java.util.Collection;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileTreeWrapper extends ResourceSupport {

    private final Collection<FileTreeDTO> files;
    private final Collection<FileTreeDTO> folders;

    public FileTreeWrapper() {
        super();
        this.files = new ArrayList<>();
        this.folders = new ArrayList<>();
    }

    public void addFile(final FileTreeDTO file) {
        files.add(file);
    }

    public void addFolder(final FileTreeDTO folder) {
        folders.add(folder);
    }

    public Collection<FileTreeDTO> getFiles() {
        return files;
    }

    public Collection<FileTreeDTO> getFolders() {
        return folders;
    }
}
