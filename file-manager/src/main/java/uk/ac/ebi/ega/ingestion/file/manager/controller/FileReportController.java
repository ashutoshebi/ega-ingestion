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

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.ega.ingestion.file.manager.models.FileHierarchyModel;
import uk.ac.ebi.ega.ingestion.file.manager.services.IFileManagerService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static uk.ac.ebi.ega.ingestion.file.manager.controller.ControllerUtils.extractVariablePath;

@RequestMapping(value = "/file/report")
@RestController
public class FileReportController {

    private final IFileManagerService fileManagerService;

    public FileReportController(final IFileManagerService fileManagerService) {
        this.fileManagerService = fileManagerService;
    }

    @RequestMapping(value = "/tsv/{accountId}/{locationId}/**", method = RequestMethod.GET)
    @Transactional(value = "fileManager_transactionManager", readOnly = true)
    public void generateTSVFileUsingStream(@PathVariable String accountId,
                                           @PathVariable String locationId,
                                           HttpServletRequest request,
                                           HttpServletResponse response) throws IOException {
        final Path extractedPath = Paths.get(extractVariablePath(request));
        if (extractedPath.toString().isEmpty()) {
            try (final Stream<FileHierarchyModel> fileHierarchyModelStream = fileManagerService.findAllFiles(accountId, locationId)) {
                writeResponse(fileHierarchyModelStream, response);
            }
        } else {
            try (final Stream<FileHierarchyModel> fileHierarchyModelStream = fileManagerService.findAllFiles(accountId, locationId, extractedPath)) {
                writeResponse(fileHierarchyModelStream, response);
            }
        }
    }

    private void writeResponse(final Stream<FileHierarchyModel> fileHierarchyModelStream,
                               final HttpServletResponse response) throws IOException {
        response.setContentType("application/tsv");
        response.addHeader("Content-Disposition", "attachment; filename=file_details.tsv");
        response.setCharacterEncoding("UTF-8");

        try (final PrintWriter out = response.getWriter()) {
            out.write(FileHierarchyModel.getColumnNames());
            fileHierarchyModelStream.forEach(fileHierarchyModel -> {
                out.write(fileHierarchyModel.toStringFileDetails());
            });
            out.flush();
        }
    }
}
