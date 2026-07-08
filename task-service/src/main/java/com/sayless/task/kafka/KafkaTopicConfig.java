package com.sayless.task.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic taskCreatedTopic() {
        return TopicBuilder.name("task-created")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
