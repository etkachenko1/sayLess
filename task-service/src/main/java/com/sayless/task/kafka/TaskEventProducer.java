package com.sayless.task.kafka;

import com.sayless.task.event.TaskCreatedEvent;
import com.sayless.task.event.TaskAssignedEvent;
import com.sayless.task.event.TaskCompletedEvent;
import com.sayless.task.event.TaskDeletedEvent;
import com.sayless.task.event.TaskUpdatedEvent;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TaskEventProducer {

    private static final String TOPIC = "task-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final Logger log = LoggerFactory.getLogger(TaskEventProducer.class);

    public TaskEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(TaskCreatedEvent event) {
        System.out.println("Publishing event: " + event.getTitle());

        kafkaTemplate.send(TOPIC, event.getTaskId(), event);
        log.info("Published task-event event {}", event.getTaskId());

    }
    public void publishAssigned(TaskAssignedEvent event) {
        System.out.println("Assigned event: " + event.getTitle());

        kafkaTemplate.send(TOPIC, event.getTaskId(), event);
        log.info("Published task-assigned event {}", event.getTaskId());

    }
    public void publishUpdated(TaskUpdatedEvent event) {
        System.out.println("Updated event: " + event.getTitle());

        kafkaTemplate.send(TOPIC, event.getTaskId(), event);
        log.info("Published task-updated event {}", event.getTaskId());

    }
    public void publishCompleted(TaskCompletedEvent event) {
        System.out.println("Completed event: " + event.getTitle());

        kafkaTemplate.send(TOPIC, event.getTaskId(), event);
        log.info("Published task-completed event {}", event.getTaskId());

    }
    public void publishDeleted(TaskDeletedEvent event) {
        kafkaTemplate.send(TOPIC, event.getTaskId(), event);
        log.info("Published task-deleted event {}", event.getTaskId());

    }

}