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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.rest.core.event.AfterCreateEvent;
import org.springframework.data.rest.core.event.BeforeCreateEvent;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.PersistentEntityResourceAssembler;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.ac.ebi.ega.encryption.core.encryption.exceptions.AlgorithmInitializationException;
import uk.ac.ebi.ega.encryption.core.services.IPasswordEncryptionService;
import uk.ac.ebi.ega.encryption.core.utils.io.IOUtils;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.DownloadBoxJob;
import uk.ac.ebi.ega.ingestion.file.manager.services.IDownloadBoxJobService;
import uk.ac.ebi.ega.ingestion.file.manager.services.IKeyGenerator;

import javax.validation.Valid;

@RepositoryRestController
public class DownloadBoxJobController implements ApplicationEventPublisherAware {

    private final Logger logger = LoggerFactory.getLogger(DownloadBoxJobController.class);

    @Autowired
    private IDownloadBoxJobService service;

    @Autowired
    private IKeyGenerator passwordGenerator;

    @Autowired
    private IPasswordEncryptionService passwordEncryptionService;

    private ApplicationEventPublisher publisher;

    @PostMapping(value = "/downloadBoxJobs")
    @ResponseBody
    public PersistentEntityResource postDownloadBox(@Valid @RequestBody Resource<DownloadBoxJob> downloadBoxJobResource,
                                                    PersistentEntityResourceAssembler assembler)
            throws AlgorithmInitializationException {
        DownloadBoxJob downloadBoxJob = downloadBoxJobResource.getContent();
        downloadBoxJob.setPassword(passwordEncryptionService.encrypt(IOUtils.convertToBytes(passwordGenerator.generateKey())));

        publisher.publishEvent(new BeforeCreateEvent(downloadBoxJob));
        downloadBoxJob = service.createJob(downloadBoxJob);
        publisher.publishEvent(new AfterCreateEvent(downloadBoxJob));
        return assembler.toFullResource(downloadBoxJob);
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

}