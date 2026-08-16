package com.sayless.notification.kafka;

import com.sayless.notification.event.*;
import com.sayless.notification.service.NotificationService;

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
    private final NotificationService notificationService;
    
    public TaskEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }


    @KafkaHandler
    public void onCreated(TaskCreatedEvent event) {
        log.info("New task '{}' created and assigned to {} by {}", event.getTitle(), event.getAssignedToName(), event.getCreatedByName());
        notificationService.notifyTaskCreated(event);
    }

    @KafkaHandler
    public void onAssigned(TaskAssignedEvent event) {
        log.info("New task '{}' assigned to you by {}", event.getTitle(), event.getCreatedByName());
        notificationService.notifyTaskAssigned(event);
    }

    @KafkaHandler
    public void onUpdated(TaskUpdatedEvent event) {
        log.info("Task '{}' is updated by {}", event.getTitle(), event.getUpdatedByName());
        notificationService.notifyTaskUpdated(event);
    }

    @KafkaHandler
    public void onCompleted(TaskCompletedEvent event) {
        log.info("Task '{}' is completed by {}" , event.getTitle(), event.getCompletedByName());
        notificationService.notifyTaskCompleted(event);
    }

    @KafkaHandler
    public void onDeleted(TaskDeletedEvent event) {
        log.info("Task {} was deleted", event.getTaskId());
        notificationService.notifyTaskDeleted(event);
    }

}
