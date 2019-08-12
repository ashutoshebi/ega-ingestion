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
package uk.ac.ebi.ega.ingestion.file.manager.dto.resources.assemblers;

import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import uk.ac.ebi.ega.ingestion.file.manager.controller.FileTreeController;
import uk.ac.ebi.ega.ingestion.file.manager.dto.FileTreeDTO;
import uk.ac.ebi.ega.ingestion.file.manager.models.FileHierarchyModel;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

public class FileHierarchyResourceAssembler extends ResourceAssemblerSupport<FileHierarchyModel, FileTreeDTO> {

    private final Class<?> controllerClass;

    public FileHierarchyResourceAssembler() {
        super(FileTreeController.class, FileTreeDTO.class);
        this.controllerClass = FileTreeController.class;
    }

    @Override
    public FileTreeDTO toResource(final FileHierarchyModel fileHierarchyModel) {

        final FileTreeDTO fileTreeDTO = FileTreeDTO.file(fileHierarchyModel);

        fileTreeDTO.add(linkTo(controllerClass).
                slash(fileHierarchyModel.getAccountId()).
                slash(fileHierarchyModel.getStagingAreaId()).
                slash(fileHierarchyModel.getOriginalPath()).
                withRel("self"));
        return fileTreeDTO;
    }
}
