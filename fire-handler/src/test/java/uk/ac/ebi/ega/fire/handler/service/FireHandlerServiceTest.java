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

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ega.fire.exceptions.ClientProtocolException;
import uk.ac.ebi.ega.fire.exceptions.FireServiceException;
import uk.ac.ebi.ega.fire.handler.config.KafkaConfiguration;
import uk.ac.ebi.ega.fire.handler.model.FireResponse;
import uk.ac.ebi.ega.fire.handler.model.FireUpload;
import uk.ac.ebi.ega.fire.handler.model.Result;
import uk.ac.ebi.ega.fire.ingestion.service.FireService;
import uk.ac.ebi.ega.fire.ingestion.service.IFireServiceNew;
import uk.ac.ebi.ega.fire.listener.ProgressListener;
import uk.ac.ebi.ega.fire.models.FireObjectRequest;
import uk.ac.ebi.ega.fire.models.IFireResponse;
import uk.ac.ebi.ega.fire.models.KeyValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.ac.ebi.ega.fire.handler.service.FireHandlerServiceTest.FireHandlerServiceTestConfig;

@DirtiesContext
@EmbeddedKafka(topics = {"${spring.kafka.fire.queue.name}"}, controlledShutdown = true)
@TestPropertySource("classpath:application-test.properties")
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
@SpringBootTest(classes = {FireHandlerApplicationTest.class, KafkaConfiguration.class, FireHandlerServiceTestConfig.class})
@RunWith(SpringRunner.class)
public class FireHandlerServiceTest {

    @TestConfiguration
    static class FireHandlerServiceTestConfig {

        @Bean
        public IFireServiceNew initIFireService() {
            return Mockito.mock(FireService.class);
        }

        @Bean
        public IFireHandlerService initFireHandlerService(final IFireServiceNew fireService, final KafkaTemplate<String, Result> kafkaTemplate,
                                                          @Value("${spring.kafka.fire.queue.name}") final String fireResponseTopic) {
            return new FireHandlerService(fireService, kafkaTemplate, fireResponseTopic);
        }

        @Bean
        public KafkaEventListenerTest initKafkaEventListener() {
            return new KafkaEventListenerTest();
        }
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    private IFireHandlerService fireHandlerService;

    @Autowired
    private IFireServiceNew fireService;

    @Value("${spring.kafka.client-id}")
    private String instanceId;

    //Add count as per test cases written i.e. those many times KafkaEventListener will be called.
    private static final CountDownLatch latch = new CountDownLatch(3);

    @Test
    public void upload_WhenPassValidRequest_ThenReturnsSuccessResponse() throws FireServiceException, IOException, ClientProtocolException, InterruptedException {

        final File tmpFile = temporaryFolder.newFile("test-file.txt");

        when(fireService.upload(any(FireObjectRequest.class), any(ProgressListener.class))).thenReturn(initFireResponse());

        final FireUpload fireUpload = new FireUpload(tmpFile.getAbsolutePath(), "dummy-md5", "dummy/fire/path");
        fireHandlerService.upload(fireUpload, "dummy-key");

        //Just wait for message to be delivered to KafkaListener.
        latch.await(2, TimeUnit.SECONDS);
    }

    @Test
    public void upload_WhenRetryToUploadFileFailsMultipleTimes_ThenExecutesRecoverMethodAfterRetriesFinish() throws FireServiceException, IOException, ClientProtocolException, InterruptedException {

        final File tmpFile = temporaryFolder.newFile("test-file.txt");

        when(fireService.upload(any(FireObjectRequest.class), any(ProgressListener.class))).thenThrow(new FireServiceException("Request retry test"));

        final FireUpload fireUpload = new FireUpload(tmpFile.getAbsolutePath(), "dummy-md5", "dummy/fire/path");
        fireHandlerService.upload(fireUpload, "dummy-key");

        //Just wait for message to be delivered to KafkaListener.
        latch.await(2, TimeUnit.SECONDS);
    }

    @Test
    public void upload_WhenRetryToUploadSameFile_ThenReturnsError() throws FireServiceException, IOException, ClientProtocolException, InterruptedException {

        final File tmpFile = temporaryFolder.newFile("test-file.txt");

        when(fireService.upload(any(FireObjectRequest.class), any(ProgressListener.class))).thenThrow(new FileAlreadyExistsException("File already exists"));

        final FireUpload fireUpload = new FireUpload(tmpFile.getAbsolutePath(), "dummy-md5", "dummy/fire/path");
        fireHandlerService.upload(fireUpload, "dummy-key");

        //Just wait for message to be delivered to KafkaListener.
        latch.await(2, TimeUnit.SECONDS);
    }

    private IFireResponse initFireResponse() {
        return new IFireResponse() {
            @Override
            public long getObjectId() {
                return 54321L;
            }

            @Override
            public String getFireOid() {
                return "fire-oid-12345";
            }

            @Override
            public long getObjectSize() {
                return 12345L;
            }

            @Override
            public String getCreateTime() {
                return LocalDateTime.now().toString();
            }

            @Override
            public Optional<List<KeyValue>> getMetadata() {
                final KeyValue keyValue = new KeyValue();
                keyValue.setKey("dummy-key");
                keyValue.setValue("dummy-value");
                return Optional.of(Collections.singletonList(keyValue));
            }

            @Override
            public Optional<String> getPath() {
                return Optional.of("dummy-path");
            }

            @Override
            public boolean isPublished() {
                return false;
            }
        };
    }

    static class KafkaEventListenerTest {
        @KafkaListener(id = "${spring.kafka.client-id}",
                topics = "${spring.kafka.fire.queue.name}",
                groupId = "${spring.kafka.client.group-id}",
                clientIdPrefix = "${spring.kafka.client-id.prefix}",
                containerFactory = "kafkaListenerContainerFactory",
                autoStartup = "true")
        public void listen(@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) final String key, final Result result,
                           final Acknowledgment acknowledgment) {

            assertEquals("dummy-key", key);
            assertNotNull(result);

            if (Result.Status.RETRY.equals(result.getStatus())) {
                assertNotNull(result.getMessage());
                latch.countDown();
            } else if (Result.Status.EXISTS.equals(result.getStatus())) {
                assertNotNull(result.getMessage());
                latch.countDown();
            } else if (Result.Status.SUCCESS.equals(result.getStatus())) {
                final FireResponse fireResponse = result.getResponseData();
                assertEquals("fire-oid-12345", fireResponse.getFireOid());
                assertEquals("dummy-path", fireResponse.getFirePath());
                assertFalse(fireResponse.isPublished());
                latch.countDown();
            } else {
                fail("Unexpected status");
            }
            acknowledgment.acknowledge();
        }
    }

    @AfterClass
    public static void afterAllTests() {
        assertEquals(0, latch.getCount());
    }
}
