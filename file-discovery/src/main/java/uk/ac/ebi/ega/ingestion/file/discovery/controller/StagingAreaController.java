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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.dto.requests.StagingAreaPatchRequest;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.dto.requests.StagingAreaPostRequest;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.dto.resources.StagingAreaResource;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.dto.resources.StagingFileResource;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.dto.resources.assemblers.StagingAreaResourceAssembler;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.dto.resources.assemblers.StagingFileResourceAssembler;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.exceptions.StagingAreaNotFoundException;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.exceptions.StagingAreaAlreadyExistsException;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.repositories.StagingAreaImpl;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.repositories.StagingFileImpl;
import uk.ac.ebi.ega.ingestion.file.discovery.services.StagingAreaService;
import uk.ac.ebi.ega.ingestion.file.discovery.services.FilePollingService;

import javax.validation.Valid;
import java.io.FileNotFoundException;

@RestController
@RequestMapping("/staging/areas")
public class StagingAreaController {

    @Autowired
    private StagingAreaService service;

    @Autowired
    private FilePollingService filePollingService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedResources<StagingAreaResource> getAllStagingAreas(@QuerydslPredicate(root = StagingAreaImpl.class)
                                                                          Predicate predicate,
                                                                  Pageable pageable,
                                                                  PagedResourcesAssembler assembler,
                                                                  StagingAreaResourceAssembler areaAssembler) {
        return assembler.toResource(service.findAll(predicate, pageable), areaAssembler);
    }

    @GetMapping(path = "/{stagingId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public StagingAreaResource getAllStagingAreas(@PathVariable String stagingId, StagingAreaResourceAssembler assembler)
            throws StagingAreaNotFoundException {
        return assembler.toResource(service.findById(stagingId).orElseThrow(StagingAreaNotFoundException::new));
    }

    @GetMapping(path = "/{stagingId}/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedResources<StagingFileResource> getAllFilesByStagingArea(@PathVariable String stagingId,
                                                                        @QuerydslPredicate(root = StagingFileImpl.class)
                                                                                Predicate predicate,
                                                                        Pageable pageable,
                                                                        PagedResourcesAssembler assembler,
                                                                        StagingFileResourceAssembler fileAssembler) {
        return assembler.toResource(service.findAllFilesByStagingId(stagingId, predicate, pageable), fileAssembler);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public StagingAreaResource newStagingArea(@Valid @RequestBody StagingAreaPostRequest request,
                                              StagingAreaResourceAssembler assembler)
            throws FileNotFoundException, StagingAreaAlreadyExistsException {
        return assembler.toResource(filePollingService.newStagingArea(request));
    }

    @PatchMapping(path = "/{stagingId}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public StagingAreaResource patchStagingArea(@PathVariable String stagingId,
                                                @Valid @RequestBody StagingAreaPatchRequest request,
                                                StagingAreaResourceAssembler assembler)
            throws StagingAreaNotFoundException {
        return assembler.toResource(filePollingService.updateStagingArea(stagingId, request.getDiscoveryEnabled(),
                request.getIngestionEnabled(), request.getDiscoveryPollingPeriod(),
                request.getIngestionPollingPeriod()));
    }

}
