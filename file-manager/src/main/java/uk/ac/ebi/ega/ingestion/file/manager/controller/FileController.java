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
package uk.ac.ebi.ega.ingestion.file.manager.controller;

import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.LinkBuilder;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.ega.ingestion.commons.models.IFileDetails;
import uk.ac.ebi.ega.ingestion.file.manager.dto.FileTreeWrapper;
import uk.ac.ebi.ega.ingestion.file.manager.models.FileHierarchyModel;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.EncryptedObject;
import uk.ac.ebi.ega.ingestion.file.manager.reports.FileDetailsTsv;
import uk.ac.ebi.ega.ingestion.file.manager.services.IFileManagerService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;
import static uk.ac.ebi.ega.ingestion.file.manager.controller.ControllerUtils.extractVariablePath;

@RequestMapping(value = "/files")
@RestController
public class FileController {

    private final IFileManagerService fileManagerService;

    public FileController(final IFileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @GetMapping(value = "/tree/{accountId}/{locationId}/**", produces = MediaTypes.HAL_JSON_VALUE)
    public Resource<FileTreeWrapper> getFileHierarchy(@PathVariable String accountId,
                                                      @PathVariable String locationId, HttpServletRequest request)
            throws FileNotFoundException {

        final LinkBuilder linkBuilder =
                linkTo(FileController.class).slash("tree").slash(accountId).slash(locationId);

        final Optional<Path> path = extractVariablePath(request).map(s -> Paths.get("/" + s));
        final List<FileHierarchyModel> fileHierarchyModels =
                fileManagerService.findAllFilesAndFoldersInPath(accountId, locationId, path);
        final Resource<FileTreeWrapper> resource =
                new Resource<>(FileTreeWrapper.create(fileHierarchyModels, linkBuilder),
                        path.map(path1 -> linkBuilder.slash(path1)).orElse(linkBuilder).withSelfRel());

        path.map(path1 -> fileManagerService.findParentOfPath(accountId, locationId, path1)
                .map(fileHierarchyModel -> linkBuilder.slash(fileHierarchyModel.getOriginalPath()))
                .orElse(linkBuilder))
                .ifPresent(link -> resource.add(link.withRel("parent")));

        return resource;
    }

    @GetMapping(value = "/list/{accountId}/{locationId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public PagedResources<IFileDetails> getAllFiles(@PathVariable String accountId,
                                                    @PathVariable String locationId,
                                                    @QuerydslPredicate(root = EncryptedObject.class) Predicate predicate,
                                                    Pageable pageable,
                                                    PagedResourcesAssembler assembler) {
        return assembler.toResource(
                fileManagerService.findAllFiles(accountId, locationId, predicate, pageable),
                linkTo(methodOn(FileController.class).getAllFiles(accountId, locationId, predicate,
                        pageable, assembler)).withSelfRel());
    }

    @RequestMapping(value = "/tsv/{accountId}/{locationId}/**", method = RequestMethod.GET)
    @Transactional(value = "fileManager_transactionManager", readOnly = true)
    public void tsvReport(@PathVariable String accountId,
                          @PathVariable String locationId,
                          HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        try (final Stream<? extends IFileDetails> stream = fileManagerService
                .findAllFiles(accountId, locationId, extractVariablePath(request))) {
            new FileDetailsTsv("file_details.tsv").stream(response, stream);
        }
    }

}
