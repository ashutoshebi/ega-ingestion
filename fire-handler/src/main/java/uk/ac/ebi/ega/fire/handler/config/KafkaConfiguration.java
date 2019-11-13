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

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import uk.ac.ebi.ega.ingestion.commons.messages.FireArchiveResult;
import uk.ac.ebi.ega.ingestion.commons.messages.FireEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static uk.ac.ebi.ega.ingestion.commons.util.MessageUtil.getJsonSerializer;
import static uk.ac.ebi.ega.ingestion.commons.util.MessageUtil.getStringJsonMessageConverter;

@Configuration
@EnableKafka
public class KafkaConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String consumerAutoOffsetReset;

    @Value("${spring.kafka.consumer.heartbeat-interval}")
    private Duration heartbeatInterval;

    @Value("${spring.kafka.consumer.session-timeout}")
    private Duration sessionTimeout;

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, FireEvent>>
    kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, FireEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(1);
        factory.getContainerProperties().setPollTimeout(600000);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setMessageConverter(getStringJsonMessageConverter());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, FireEvent> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean
    public ProducerFactory<String, FireArchiveResult> producerFactory() {
        DefaultKafkaProducerFactory<String, FireArchiveResult> factory =
                new DefaultKafkaProducerFactory<>(producerConfigs());
        factory.setValueSerializer(getJsonSerializer());
        return factory;
    }

    @Bean
    public KafkaTemplate<String, FireArchiveResult> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    private Map<String, Object> producerConfigs() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // introduce a delay on the send to allow more messages to accumulate
        properties.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        return properties;
    }

    private Map<String, Object> consumerConfigs() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1);
        properties.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, (int) heartbeatInterval.toMillis());
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, (int) sessionTimeout.toMillis());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerAutoOffsetReset);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return properties;
    }
}
