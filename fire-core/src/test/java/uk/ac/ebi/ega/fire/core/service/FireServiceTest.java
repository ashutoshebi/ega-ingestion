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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.ega.fire.core.config.HttpClientProperties;
import uk.ac.ebi.ega.fire.core.exception.FireServiceException;
import uk.ac.ebi.ega.fire.core.model.ErrorResponse;
import uk.ac.ebi.ega.fire.core.model.FireObjectRequest;
import uk.ac.ebi.ega.fire.core.model.FireObjectResponse;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
public class FireServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FireServiceTest.class);

    @PropertySource(value = "classpath:application-test.properties")
    @EnableConfigurationProperties
    @TestConfiguration
    static class FireCoreConfiguration {

        @ConfigurationProperties(prefix = "httpclient.connection")
        @Bean
        public HttpClientProperties initHttpClientProperties() {
            return new HttpClientProperties();
        }

        @Bean
        public CloseableHttpClient initHttpClient(@Value("${fire.credentials-file-path}") final String fireCredentialsFilePath,
                                                  final HttpClientProperties httpClientProperties) throws IOException {

            final Path fireCredentialsPath = Paths.get(fireCredentialsFilePath);

            if (!fireCredentialsPath.toFile().exists()) {
                throw new FileNotFoundException("File ".concat(fireCredentialsPath.toString()).concat(" not found"));
            }

            final String credentials = Files.readAllLines(fireCredentialsPath).get(0);
            final String base64EncodedCredentials = Base64.getEncoder()
                    .encodeToString(credentials.getBytes());

            final ConnectionConfig connectionConfig = ConnectionConfig.custom()
                    .setBufferSize(httpClientProperties.getBufferSize())
                    .build();

            final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
            connectionManager.setMaxTotal(httpClientProperties.getMaxTotal());
            connectionManager.setDefaultMaxPerRoute(httpClientProperties.getDefaultMaxPerRoute());

            final RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectTimeout(httpClientProperties.getTimeout() * 1000)
                    .setConnectionRequestTimeout(httpClientProperties.getTimeout() * 1000)
                    .setSocketTimeout(httpClientProperties.getTimeout() * 1000)
                    .build();

            return HttpClients.custom()
                    .setDefaultConnectionConfig(connectionConfig)
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig)
                    .setDefaultHeaders(Collections.singleton(new BasicHeader("Authorization", "Basic ".concat(base64EncodedCredentials))))
                    .build();
        }

        @Bean
        public IFireService initFireService(final CloseableHttpClient httpClient,
                                            @Value("${fire.url}") final String fireURL) {
            return new FireService(httpClient, fireURL);
        }

        @Bean
        public ObjectMapper initObjectMapper() {
            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            //More properties can be set as per need
            return objectMapper;
        }
    }

    @Autowired
    private IFireService fireService;

    @Autowired
    private ObjectMapper objectMapper;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final String FILE_SIZE_HEADER = "x-fire-size";
    private static final String FILE_MD5_HEADER = "x-fire-md5";

    /**
     * Tests for successful file upload to server.
     */
    @Test
    public void upload_WhenPassValidRequestData_ThenUploadFileToFIRE() throws IOException, NoSuchAlgorithmException, FireServiceException {
        final byte[] content = "This is a test file to upload on FIRE".getBytes();
        final File tmpFileToUpload = createFileWithContent("test-file-to-upload.txt", content);

        final MessageDigest messageDigest = getMD5MessageDigest();
        messageDigest.update(content);

        final String md5 = getMD5(messageDigest);

        final Map<String, String> headers = new HashMap<>(2);
        headers.put(FILE_SIZE_HEADER, String.valueOf(tmpFileToUpload.length()));
        headers.put(FILE_MD5_HEADER, md5);

        final FireObjectRequest fireObjectRequest = new FireObjectRequest(tmpFileToUpload, headers);

        fireService.upload(fireObjectRequest, responseHandler(tmpFileToUpload), bytesTransferred -> LOGGER.info("bytes transferred={}", bytesTransferred));
    }

    /**
     * Tests for error from server when wrong MD5 pass.
     */
    @Test
    public void upload_WhenPassWrongMD5InRequestData_thenReceivesErrorResponse() throws IOException, FireServiceException {
        final byte[] content = "This is a test file to upload on FIRE".getBytes();
        final File tmpFileToUpload = createFileWithContent("test-file-to-upload.txt", content);

        final Map<String, String> headers = new HashMap<>(2);
        headers.put(FILE_SIZE_HEADER, String.valueOf(tmpFileToUpload.length()));
        headers.put(FILE_MD5_HEADER, "WrongMD=75a1e608e6f1cee5a7d8e3d0");

        final FireObjectRequest fireObjectRequest = new FireObjectRequest(tmpFileToUpload, headers);

        fireService.upload(fireObjectRequest, httpResponse -> {
            assertNotNull(httpResponse);
            assertEquals(400, httpResponse.getStatusLine().getStatusCode());

            final ErrorResponse errorResponse = objectMapper.readValue(EntityUtils.toString(httpResponse.getEntity()), ErrorResponse.class);

            assertEquals(400, errorResponse.getStatusCode());
            assertNotNull(errorResponse.getStatusMessage());
            assertNotNull(errorResponse.getDetail());
            assertEquals("POST", errorResponse.getHttpMethod());

            return (CloseableHttpResponse) httpResponse;
        }, bytesTransferred -> LOGGER.info("Bytes Transferred={}", bytesTransferred));
    }

    /**
     * Tests the successful multiple upload in parallel. Check for HttpClient thread safety.
     */
    @Test
    public void upload_WhenPassTwoFilesToUpload_ThenUploadsFilesToFIRE() throws IOException, NoSuchAlgorithmException, ExecutionException, InterruptedException {
        final byte[] content1 = "This is a test file1 to upload on FIRE".getBytes();
        final File tmpFileToUpload1 = createFileWithContent("test-file1-to-upload.txt", content1);

        final MessageDigest messageDigest1 = getMD5MessageDigest();
        messageDigest1.update(content1);

        final String md51 = getMD5(messageDigest1);

        final Map<String, String> headers1 = new HashMap<>(2);
        headers1.put(FILE_SIZE_HEADER, String.valueOf(tmpFileToUpload1.length()));
        headers1.put(FILE_MD5_HEADER, md51);

        final FireObjectRequest fireObjectRequest1 = new FireObjectRequest(tmpFileToUpload1, headers1);

        final byte[] content2 = "This is a test file2 to upload on FIRE. File will be uploaded in parallel.".getBytes();
        final File tmpFileToUpload2 = createFileWithContent("test-file2-to-upload.txt", content2);

        final MessageDigest messageDigest2 = getMD5MessageDigest();
        messageDigest2.update(content2);

        final String md52 = getMD5(messageDigest2);

        final Map<String, String> headers2 = new HashMap<>(2);
        headers2.put(FILE_SIZE_HEADER, String.valueOf(tmpFileToUpload2.length()));
        headers2.put(FILE_MD5_HEADER, md52);

        final FireObjectRequest fireObjectRequest2 = new FireObjectRequest(tmpFileToUpload2, headers2);

        final ExecutorService executor = Executors.newFixedThreadPool(2);

        final Future<Boolean> future1 = executor.submit(() -> {
            try {
                fireService.upload(fireObjectRequest1, responseHandler(tmpFileToUpload1), bytesTransferred -> LOGGER.info("File1 bytes transferred={}", bytesTransferred));
                return true;
            } catch (FireServiceException e) {
                LOGGER.error("Error while uploading test file.", e);
                return false;
            }
        });

        final Future<Boolean> future2 = executor.submit(() -> {
            try {
                fireService.upload(fireObjectRequest2, responseHandler(tmpFileToUpload2), bytesTransferred -> LOGGER.info("File2 bytes transferred={}", bytesTransferred));
                return true;
            } catch (FireServiceException e) {
                LOGGER.error("Error while uploading test file.", e);
                return false;
            }
        });

        assertTrue(future1.get());
        assertTrue(future2.get());
        executor.shutdown();
    }

    private File createFileWithContent(final String fileName, final byte[] content) throws IOException {
        final File tmpFileToUpload = temporaryFolder.newFile(fileName);
        try (final OutputStream outputStream = new FileOutputStream(tmpFileToUpload)) {
            outputStream.write(content);
            outputStream.flush();
        }
        return tmpFileToUpload;
    }

    private ResponseHandler<CloseableHttpResponse> responseHandler(final File tmpFileToUpload) {
        return httpResponse -> {
            assertNotNull(httpResponse);
            assertEquals(200, httpResponse.getStatusLine().getStatusCode());

            final FireObjectResponse fireObjectResponse = objectMapper.readValue(EntityUtils.toString(httpResponse.getEntity()), FireObjectResponse.class);

            assertEquals(tmpFileToUpload.length(), fireObjectResponse.getObjectSize());
            assertNotNull(fireObjectResponse.getCreateTime());
            assertTrue(fireObjectResponse.getObjectId() > 0);
            assertNotNull(fireObjectResponse.getFireOid());

            return (CloseableHttpResponse) httpResponse;
        };
    }

    private MessageDigest getMD5MessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("MD5");
    }

    private String getMD5(final MessageDigest messageDigest) {
        return DatatypeConverter.printHexBinary(messageDigest.digest()).toLowerCase();
    }
}
