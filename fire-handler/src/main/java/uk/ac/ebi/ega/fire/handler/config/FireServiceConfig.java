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
package uk.ac.ebi.ega.fire.handler.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import uk.ac.ebi.ega.fire.handler.kafka.listener.FireEventListener;
import uk.ac.ebi.ega.fire.handler.service.FireHandlerService;
import uk.ac.ebi.ega.fire.handler.service.IFireHandlerService;
import uk.ac.ebi.ega.fire.ingestion.service.FireService;
import uk.ac.ebi.ega.fire.ingestion.service.IFireServiceNew;
import uk.ac.ebi.ega.fire.properties.HttpClientProperties;
import uk.ac.ebi.ega.ingestion.commons.messages.FireArchiveResult;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Collections;

@Configuration
public class FireServiceConfig {

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
    public IFireServiceNew initFireService(final CloseableHttpClient httpClient,
                                           @Value("${fire.url}") final String fireURL) {
        return new FireService(httpClient, fireURL);
    }

    @Bean
    public IFireHandlerService initFireHandlerService(final IFireServiceNew fireService,
                                                      final KafkaTemplate<String, FireArchiveResult> kafkaTemplate,
                                                      @Value("${spring.kafka.fire.queue.name}") final String topicName) {
        return new FireHandlerService(fireService, kafkaTemplate, topicName);
    }

    @Bean
    public FireEventListener initFireEventListener(final IFireHandlerService fireHandlerService) {
        return new FireEventListener(fireHandlerService);
    }
}
