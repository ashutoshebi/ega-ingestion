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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import uk.ac.ebi.ega.ingestion.file.manager.dto.FileIngestionBoxDTO;
import uk.ac.ebi.ega.ingestion.file.manager.dto.FileIngestionDTO;
import uk.ac.ebi.ega.ingestion.file.manager.dto.FileIngestionWrapper;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.FileHierarchy;
import uk.ac.ebi.ega.ingestion.file.manager.services.IFileManagerService;
import uk.ac.ebi.ega.ingestion.file.manager.utils.FileStructureType;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

@RequestMapping(value = "/file-ingestion")
@RestController
public class FileIngestionController {

    private static final String REL = "self";
    private final IFileManagerService fileManagerService;

    public FileIngestionController(final IFileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @PostMapping("/{accountId}/{locationId}/**") //TODO needs to be removed. Should be called from Kafka listner
    public String createFileTree(@PathVariable String accountId,
                                 @PathVariable String locationId, HttpServletRequest request) {
        final String path = extractFilePath(request);
        fileManagerService.createFileDirectoryStructure(path, accountId, locationId);
        return "Work Done!!!";
    }

    @GetMapping(value = "/{accountId}/{locationId}/**", produces = MediaTypes.HAL_JSON_VALUE)
    public Resource<FileIngestionWrapper> getAllFilesOfUser(@PathVariable String accountId,
                                                            @PathVariable String locationId, HttpServletRequest request) {

        final Link link = linkTo(FileIngestionController.class).withSelfRel();
        final String path = extractFilePath(request);
        final String baseURI = new StringBuilder(link.getHref()).append("/").append(accountId).append("/")
                .append(locationId).toString();

        final FileIngestionBoxDTO fileIngestionBoxDTO = FileIngestionBoxDTO.newInstance(FileStructureType.FILE.name(), new ArrayList<>());
        final FileIngestionBoxDTO folderIngestionBoxDTO = FileIngestionBoxDTO.newInstance(FileStructureType.FOLDER.name(), new ArrayList<>());
        final FileIngestionWrapper fileIngestionWrapper = FileIngestionWrapper.newInstance(fileIngestionBoxDTO, folderIngestionBoxDTO);

        final List<FileHierarchy> fileHierarchies = fileManagerService.findAll(path);

        fileHierarchies.forEach(fileHierarchy -> {

                    final Link selfLink = new Link(new StringBuilder(baseURI).append(path).append("/")
                            .append(fileHierarchy.getFiledetails()).toString(), REL);

                    if (FileStructureType.FILE.equals(fileHierarchy.getFileType())) {//TODO replace static values with dynamic

                        final FileIngestionDTO fileIngestionDTO = FileIngestionDTO.newInstance(fileHierarchy.getAccountId(), fileHierarchy.getStagingAreaId(),
                                fileHierarchy.getPlainSize(), fileHierarchy.getEncryptedSize(), fileHierarchy.getFiledetails(), fileHierarchy.getPlainMd5(),
                                fileHierarchy.getUpdateDate(), fileHierarchy.getStatus());
                        fileIngestionDTO.add(selfLink);
                        fileIngestionBoxDTO.getFileIngestionDTOS().add(fileIngestionDTO);
                    } else {
                        final FileIngestionDTO fileIngestionDTO = FileIngestionDTO.newInstance(fileHierarchy.getAccountId(), fileHierarchy.getStagingAreaId(),
                                fileHierarchy.getFiledetails());
                        fileIngestionDTO.add(selfLink);
                        folderIngestionBoxDTO.getFileIngestionDTOS().add(fileIngestionDTO);
                    }
                }
        );
        return new Resource<>(fileIngestionWrapper, new Link(baseURI, REL));
    }

    private String extractFilePath(HttpServletRequest request) {
        final String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        final String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        final String variablePath = new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, path);
        return !StringUtils.isEmpty(variablePath) ? "/" + variablePath : "";
    }
}
