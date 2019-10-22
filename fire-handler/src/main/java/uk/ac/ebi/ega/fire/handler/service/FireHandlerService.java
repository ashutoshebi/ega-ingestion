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
package uk.ac.ebi.ega.fire.handler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import uk.ac.ebi.ega.fire.exceptions.ClientProtocolException;
import uk.ac.ebi.ega.fire.exceptions.FireServiceException;
import uk.ac.ebi.ega.fire.handler.model.FireResponse;
import uk.ac.ebi.ega.fire.handler.model.FireUpload;
import uk.ac.ebi.ega.fire.handler.model.Result;
import uk.ac.ebi.ega.fire.ingestion.service.IFireServiceNew;
import uk.ac.ebi.ega.fire.models.FireObjectRequest;
import uk.ac.ebi.ega.fire.models.IFireResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.FileAlreadyExistsException;

public class FireHandlerService implements IFireHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FireHandlerService.class);

    private final IFireServiceNew fireService;
    private final KafkaTemplate<String, Result> kafkaTemplate;
    private final String fireResponseTopic;

    public FireHandlerService(final IFireServiceNew fireService,
                              final KafkaTemplate<String, Result> kafkaTemplate,
                              final String fireResponseTopic) {
        this.fireService = fireService;
        this.kafkaTemplate = kafkaTemplate;
        this.fireResponseTopic = fireResponseTopic;
    }

    @Retryable(maxAttemptsExpression = "#{${request.attempts.max}}",
            backoff = @Backoff(delayExpression = "#{${request.attempts.delay}}",
                    multiplierExpression = "#{${request.attempts.multiplier}}"),
            value = {FireServiceException.class}
    )
    @Override
    public void upload(final FireUpload fireUpload, final String key) throws FireServiceException {
        try {
            doUpload(fireUpload, key);
        } catch (FireServiceException fse) {
            LOGGER.error("Rethrowing FireServiceException for request retrial");
            throw fse;
        } catch (FileAlreadyExistsException fae) {
            LOGGER.error(fae.getMessage(), fae);
            reportResult(key, Result.exists("File ".concat(fireUpload.getFileToUploadPath()).concat(" already exists on fire with given fire path")));
        } catch (FileNotFoundException | ClientProtocolException e) {
            LOGGER.error(e.getMessage(), e);
            reportResult(key, Result.failure("Error while uploading file "
                    .concat(fireUpload.getFileToUploadPath())
                    .concat(". Check whether request is properly constructed & file to upload exists")));
        }//catch clause can be modified in case error handling changes.
    }

    private void doUpload(final FireUpload fireUpload, final String key) throws FireServiceException, FileNotFoundException, FileAlreadyExistsException, ClientProtocolException {
        final FireObjectRequest fireObjectRequest = new FireObjectRequest(new File(fireUpload.getFileToUploadPath()), fireUpload.getMd5(), fireUpload.getFirePath());
        final IFireResponse fireResponse = fireService.upload(fireObjectRequest, bytesTransferred -> {/* Add logic to work with count of byte transferred */});
        final FireResponse fireResponseModel = new FireResponse(fireResponse.getFireOid(), fireResponse.getPath().orElse(""), fireResponse.isPublished());
        reportResult(key, Result.success(fireResponseModel));
    }

    @Recover
    public void recoverFromFixedRetry(final FireServiceException fse, final FireUpload fireUpload, final String key) {
        LOGGER.info("Recovering from fixed retry");
        reportResult(key, Result.retry("Unable to process file ".concat(fireUpload.getFileToUploadPath()).concat(" request after several retries")));
    }

    private void reportResult(final String key, final Result result) {
        LOGGER.info("Data sent to kafka topic={}, key={}, data={}", fireResponseTopic, key, result);
        kafkaTemplate.send(fireResponseTopic, key, result);
    }
}
