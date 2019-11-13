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
package uk.ac.ebi.ega.fire.handler.kafka.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import uk.ac.ebi.ega.fire.exceptions.FireServiceException;
import uk.ac.ebi.ega.fire.handler.service.IFireHandlerService;
import uk.ac.ebi.ega.ingestion.commons.messages.FireEvent;

public class FireEventListener {

    private IFireHandlerService fireHandlerService;

    public FireEventListener(final IFireHandlerService fireHandlerService) {
        this.fireHandlerService = fireHandlerService;
    }

    @KafkaListener(id = "${spring.kafka.client-id}", topics = "${spring.kafka.file.archive.queue.name}",
            groupId = "${spring.kafka.client.group-id}", clientIdPrefix = "${spring.kafka.client-id.prefix}", autoStartup = "true")
    public void listen(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) final String key, final FireEvent fireEvent,
                       final Acknowledgment acknowledgment) throws FireServiceException {
        fireHandlerService.upload(fireEvent, key);
        /* Sending acknowledgment once request has been executed.
           Acknowledgment will be sent in all cases.
         */
        acknowledgment.acknowledge();
    }
}
