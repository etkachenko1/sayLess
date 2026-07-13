package com.sayless.task.kafka;

import com.sayless.task.event.TaskCreatedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TaskEventProducer {

    private static final String TOPIC = "task-created";

    private final KafkaTemplate<String, TaskCreatedEvent> kafkaTemplate;
    private static final Logger log = LoggerFactory.getLogger(TaskEventProducer.class);

    public TaskEventProducer(KafkaTemplate<String, TaskCreatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TaskCreatedEvent event) {
        System.out.println("Publishing event: " + event.getTitle());

        kafkaTemplate.send(TOPIC, event.getTaskId(), event);
        log.info("Published task-event-created event {}", event.getTaskId());

    }
   
}