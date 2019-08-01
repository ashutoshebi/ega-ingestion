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

import org.springframework.context.MessageSource;
import org.springframework.data.rest.webmvc.RepositoryRestExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.FileNotFoundException;

@ControllerAdvice
public class RestControllerAdvice extends RepositoryRestExceptionHandler {

    /**
     * Creates a new {@link RepositoryRestExceptionHandler} using the given {@link MessageSource}.
     *
     * @param messageSource must not be {@literal null}.
     */
    public RestControllerAdvice(MessageSource messageSource) {
        super(messageSource);
    }

    /**
     * @return ResponseEntity with 404 http status code.
     */
    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Path does not exists")
    @ExceptionHandler(FileNotFoundException.class)
    public void contentNotFoundExceptionHandler() {

    }
}
