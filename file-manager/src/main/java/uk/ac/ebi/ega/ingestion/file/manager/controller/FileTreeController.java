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

import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import uk.ac.ebi.ega.ingestion.file.manager.dto.FileTreeBoxDTO;
import uk.ac.ebi.ega.ingestion.file.manager.dto.FileTreeDTO;
import uk.ac.ebi.ega.ingestion.file.manager.dto.FileTreeWrapper;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileHierarchy;
import uk.ac.ebi.ega.ingestion.file.manager.services.IFileManagerService;
import uk.ac.ebi.ega.ingestion.file.manager.utils.FileStructureType;

import javax.servlet.http.HttpServletRequest;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@RequestMapping(value = "/file/tree")
@RestController
public class FileTreeController {

    private static final String REL = "self";
    private final IFileManagerService fileManagerService;

    public FileTreeController(final IFileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @GetMapping(value = "/{accountId}/{locationId}/**", produces = MediaTypes.HAL_JSON_VALUE)
    public Resource<FileTreeWrapper> getFileHierarchy(@PathVariable String accountId,
                                                      @PathVariable String locationId, HttpServletRequest request) throws FileNotFoundException {

        final Link link = linkTo(FileTreeController.class).withSelfRel();
        final Path path = Paths.get(extractFilePath(request));
        final String baseURI = new StringBuilder(link.getHref()).append("/").append(accountId).append("/")
                .append(locationId).toString();

        final FileTreeBoxDTO fileTreeBoxDTO = new FileTreeBoxDTO(FileStructureType.FILE.name(), new ArrayList<>());
        final FileTreeBoxDTO folderIngestionBoxDTO = new FileTreeBoxDTO(FileStructureType.FOLDER.name(), new ArrayList<>());
        final FileTreeWrapper fileTreeWrapper = new FileTreeWrapper(fileTreeBoxDTO, folderIngestionBoxDTO);

        final Optional<List<FileHierarchy>> optionalFileHierarchies = fileManagerService.findAllByPath(path);
        final List<FileHierarchy> fileHierarchies = optionalFileHierarchies.orElseThrow(FileNotFoundException::new);

        fileHierarchies.forEach(fileHierarchy -> {

                    final Link selfLink = new Link(new StringBuilder(baseURI).append(path).append("/")
                            .append(fileHierarchy.getName()).toString(), REL);

                    if (FileStructureType.FILE.equals(fileHierarchy.getFileType())) {
                        final FileTreeDTO fileTreeDTO = new FileTreeDTO(fileHierarchy.getAccountId(), fileHierarchy.getStagingAreaId(),
                                fileHierarchy.getFileDetails().getPlainSize(), fileHierarchy.getFileDetails().getEncryptedSize(), fileHierarchy.getName(),
                                fileHierarchy.getFileDetails().getPlainMd5(), fileHierarchy.getUpdateDate(), fileHierarchy.getFileDetails().getStatus());
                        fileTreeDTO.add(selfLink);
                        fileTreeBoxDTO.getFileTreeDTOS().add(fileTreeDTO);
                    } else {
                        final FileTreeDTO fileTreeDTO = new FileTreeDTO(fileHierarchy.getAccountId(), fileHierarchy.getStagingAreaId(),
                                fileHierarchy.getName());
                        fileTreeDTO.add(selfLink);
                        folderIngestionBoxDTO.getFileTreeDTOS().add(fileTreeDTO);
                    }
                }
        );
        return new Resource<>(fileTreeWrapper, new Link(baseURI, REL));
    }

    private String extractFilePath(HttpServletRequest request) {
        final String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        final String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        final String variablePath = new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);
        return !StringUtils.isEmpty(variablePath) ? "/" + variablePath : "";
    }
}
