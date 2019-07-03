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
package uk.ac.ebi.ega.ingestion.file.discovery.controller;

import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.dto.resources.StagingFileResource;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.dto.resources.assemblers.StagingFileResourceAssembler;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.exceptions.StagingFileNotFoundException;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.repositories.StagingFileImpl;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.repositories.StagingFileRepository;

@RestController
@RequestMapping("/staging/files")
public class StagingFilesController {

    @Autowired
    private StagingFileRepository repository;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedResources<StagingFileResource> getAll(@QuerydslPredicate(root = StagingFileImpl.class)
                                                              Predicate predicate,
                                                      Pageable pageable,
                                                      PagedResourcesAssembler assembler,
                                                      StagingFileResourceAssembler stagingFileResourceAssembler) {
        return assembler.toResource(repository.findAll(predicate, pageable), stagingFileResourceAssembler);
    }

    @GetMapping(path = "/{fileId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public StagingFileResource getFile(@PathVariable String fileId, StagingFileResourceAssembler assembler)
            throws StagingFileNotFoundException {
        return assembler.toResource(repository.findById(fileId).orElseThrow(StagingFileNotFoundException::new));
    }

}
