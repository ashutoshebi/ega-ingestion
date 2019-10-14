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
package uk.ac.ebi.ega.fire.core.service;

import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.fire.core.exception.FireServiceException;
import uk.ac.ebi.ega.fire.core.listener.ProgressListener;
import uk.ac.ebi.ega.fire.core.model.FileBodyInterceptor;
import uk.ac.ebi.ega.fire.core.model.FireObjectRequest;

import java.io.IOException;
import java.time.LocalDateTime;

public class FireService implements IFireService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FireService.class);

    private final String fireURL;
    private final CloseableHttpClient httpClient;

    public FireService(final CloseableHttpClient httpClient, final String fireURL) {
        this.httpClient = httpClient;
        this.fireURL = fireURL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void upload(final FireObjectRequest fireObjectRequest,
                       final ResponseHandler<CloseableHttpResponse> responseHandler,
                       final ProgressListener progressListener) throws FireServiceException {
        LOGGER.info("Data received to upload file = {}", fireObjectRequest);
        try {
            doUpload(fireObjectRequest, responseHandler, progressListener);
        } catch (Exception e) {
            throw new FireServiceException(e.getCause() != null ? e.getCause().getMessage() : e.getMessage(), e);
        }
    }

    private void doUpload(final FireObjectRequest fireObjectRequest,
                          final ResponseHandler<CloseableHttpResponse> responseHandler, final ProgressListener progressListener) throws IOException {
        final FileBodyInterceptor fileBodyInterceptor = new FileBodyInterceptor(fireObjectRequest.getFileToUpload(), progressListener);

        // build multipart upload request
        final HttpEntity httpEntity = MultipartEntityBuilder.create()
                .addPart("file", fileBodyInterceptor)
                .build();

        // Build request builder
        final RequestBuilder requestBuilder = RequestBuilder
                .post(fireURL)
                .setEntity(httpEntity);

        // Map headers to request builders
        fireObjectRequest.getHeaders().forEach(requestBuilder::addHeader);

        LOGGER.info("Process for file {} started at {}. File length is {}", fireObjectRequest.getFileToUpload().getAbsolutePath(),
                LocalDateTime.now(), fireObjectRequest.getFileToUpload().length());

        //Resource references are not supported in this version. Closes ClosableHttpResponse once processed by responseHandler.
        try (final CloseableHttpResponse ignored = httpClient.execute(requestBuilder.build(), responseHandler)) {
            LOGGER.info("Process for file {} ended at {}. Total {} bytes transferred", fireObjectRequest.getFileToUpload().getAbsolutePath(),
                    LocalDateTime.now(), fileBodyInterceptor.getBytesWritten());
        }
    }
}