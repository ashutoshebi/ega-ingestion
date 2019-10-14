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

import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import uk.ac.ebi.ega.fire.core.exception.FireServiceException;
import uk.ac.ebi.ega.fire.core.listener.ProgressListener;
import uk.ac.ebi.ega.fire.core.model.FireObjectRequest;

public interface IFireService {
    /**
     * @param fireObjectRequest FireObjectRequest object contains details of file to upload.
     * @param responseHandler ResponseHandler to handle HttpResponse.
     * @param progressListener Implementation of ProgressListener to get progress of bytes processed/transferred.
     *
     * @throws FireServiceException will be thrown when issue occurs while uploading file.
     */
    void upload(final FireObjectRequest fireObjectRequest, final ResponseHandler<CloseableHttpResponse> responseHandler,
                final ProgressListener progressListener) throws FireServiceException;
}
