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
package uk.ac.ebi.ega.ingestion.file.discovery.controller.dto.resources.assemblers;

import org.springframework.beans.BeanUtils;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.StagingAreaController;
import uk.ac.ebi.ega.ingestion.file.discovery.models.StagingArea;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.dto.resources.StagingAreaResource;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

public class StagingAreaResourceAssembler extends ResourceAssemblerSupport<StagingArea, StagingAreaResource> {

    private final Class<?> controllerClass;

    public StagingAreaResourceAssembler() {
        super(StagingAreaController.class, StagingAreaResource.class);
        this.controllerClass = StagingAreaController.class;
    }

    @Override
    public StagingAreaResource toResource(StagingArea stagingArea) {
        StagingAreaResource resource = createResourceWithId(stagingArea.getId(), stagingArea);
        BeanUtils.copyProperties(stagingArea,resource);
        resource.add(linkTo(controllerClass).slash(stagingArea.getId()).slash("files").withRel("files"));
        return resource;
    }

}