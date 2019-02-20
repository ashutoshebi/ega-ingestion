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

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.exceptions.StagingAreaNotFoundException;
import uk.ac.ebi.ega.ingestion.file.discovery.controller.exceptions.StagingFileNotFoundException;
import uk.ac.ebi.ega.ingestion.file.discovery.persistence.exceptions.StagingAreaAlreadyExistsException;

import java.io.FileNotFoundException;

@RestControllerAdvice
public class FileDiscoveryRestControllerAdvice {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({StagingAreaNotFoundException.class, StagingFileNotFoundException.class})
    public void handleNotFound() {

    }

    @ExceptionHandler({FileNotFoundException.class})
    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Staging area path does not exists")
    public void fileNotFound() {

    }

    @ExceptionHandler({StagingAreaAlreadyExistsException.class})
    @ResponseStatus(value = HttpStatus.CONFLICT, reason = "Staging area id already exists")
    public void stagingAreaAlreadyExists() {

    }

    @ExceptionHandler({ConstraintViolationException.class})
    @ResponseStatus(value = HttpStatus.CONFLICT, reason = "Staging area path is already in use")
    public void constraintViolationException() {

    }

}
