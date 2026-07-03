package com.sayless.task.kafka;

import com.sayless.task.event.TaskCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TaskEventProducer {

    private static final String TOPIC = "task-created";

    private final KafkaTemplate<String, TaskCreatedEvent> kafkaTemplate;

    public TaskEventProducer(KafkaTemplate<String, TaskCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TaskCreatedEvent event) {
        kafkaTemplate.send(TOPIC, event.getTaskId(), event);
    }
}