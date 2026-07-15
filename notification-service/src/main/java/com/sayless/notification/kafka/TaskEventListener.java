package com.sayless.notification.kafka;

import com.sayless.notification.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
//spring kafka's class-level listener
@KafkaListener(topics = "task-events", groupId = "notifications") 

public class TaskEventListener {
    private static final Logger log = LoggerFactory.getLogger(TaskEventListener.class);

    @KafkaHandler
    public void onCreated(TaskCreatedEvent event) {
        log.info("New task '{}' created and assigned to {} by {}", event.getTitle(), event.getAssignedToName(), event.getCreatedByName());
    }

    @KafkaHandler
    public void onAssigned(TaskAssignedEvent event) {
        log.info("New task '{}' assigned to you by {}", event.getTitle(), event.getCreatedByName());

    }

    @KafkaHandler
    public void onUpdated(TaskUpdatedEvent event) {
        log.info("Task '{}' is updated by {}", event.getTitle(), event.getUpdatedByName());

    }

    @KafkaHandler
    public void onCompleted(TaskCompletedEvent event) {
        log.info("Task '{}' is completed by {}" , event.getTitle(), event.getCompletedByName());

    }


}
