package com.sayless.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultKafkaConsumerFactoryCustomizer javaTimeAwareJsonDeserializer() {
        return this::configureValueDeserializer;
    }

    @SuppressWarnings("unchecked")
    private void configureValueDeserializer(DefaultKafkaConsumerFactory<?, ?> consumerFactory) {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        JsonDeserializer<Object> valueDeserializer = new JsonDeserializer<>(objectMapper);
        valueDeserializer.trustedPackages("com.sayless.notification.event");
        ((DefaultKafkaConsumerFactory<Object, Object>) consumerFactory).setValueDeserializer(valueDeserializer);
    }
}
