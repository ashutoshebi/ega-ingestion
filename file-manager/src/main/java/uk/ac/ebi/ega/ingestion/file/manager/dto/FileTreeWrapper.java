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
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkBuilder;
import org.springframework.hateoas.ResourceSupport;
import uk.ac.ebi.ega.ingestion.file.manager.models.FileHierarchyModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileTreeWrapper extends ResourceSupport {

    private final Collection<FolderDTO> folders;
    private final Collection<FileDTO> files;

    public FileTreeWrapper() {
        super();
        this.files = new ArrayList<>();
        this.folders = new ArrayList<>();
    }

    public void addFile(final FileDTO file) {
        files.add(file);
    }

    public void addFolder(final FolderDTO folder) {
        folders.add(folder);
    }

    public Collection<FileDTO> getFiles() {
        return files;
    }

    public Collection<FolderDTO> getFolders() {
        return folders;
    }

    public static FileTreeWrapper create(List<FileHierarchyModel> fileHierarchyModels, LinkBuilder builder) {
        final FileTreeWrapper fileTreeWrapper = new FileTreeWrapper();
        fileHierarchyModels.forEach(model -> {
            final Link link = builder.slash(model.getOriginalPath()).withSelfRel();
            switch (model.getFileType()) {
                case FILE:
                    final FileDTO fileDTO = FileDTO.fromModel(model);
                    fileDTO.add(link);
                    fileTreeWrapper.addFile(fileDTO);
                    break;
                case FOLDER:
                    final FolderDTO folderDTO = new FolderDTO(model.getName());
                    folderDTO.add(link);
                    fileTreeWrapper.addFolder(folderDTO);
                    break;
            }
        });
        return fileTreeWrapper;
    }

}
