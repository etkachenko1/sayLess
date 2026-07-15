package com.sayless.notification.kafka;

import com.sayless.notification.event.TaskCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TaskEventListener {
    private static final Logger log = LoggerFactory.getLogger(TaskEventListener.class);

    @KafkaListener(topics = "task-events", groupId = "notifications")
    public void consume(TaskCreatedEvent event) {
        log.info("New task '{}' assigned to {} by {}", event.getTitle(), event.getAssignedToName(), event.getCreatedByName());
    }
}
