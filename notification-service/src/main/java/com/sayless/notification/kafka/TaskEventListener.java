package com.sayless.notification.kafka;

import com.sayless.notification.event.TaskCreatedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TaskEventListener {

    @KafkaListener(topics = "task-created", groupId = "notifications")
    public void consume(TaskCreatedEvent event) {

        System.out.println("=================================");
        System.out.println("NEW TASK EVENT");
        System.out.println("Task: " + event.getTitle());
        System.out.println("Assigned to: " + event.getAssignedToName());
        System.out.println("Created by: " + event.getCreatedByName());
        System.out.println("=================================");

    }
}
