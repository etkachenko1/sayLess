package com.sayless.notification.service;

import com.sayless.notification.event.*;
import com.sayless.notification.model.Notification;
import com.sayless.notification.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

// determines who gets the notification
//each method resolves the recepient of notification differently.
// - assignment notifications to assignedToId
// - completion notifications to createdById
// - accepted request notifications to requesterId
//guards against self notifications

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repo;
    private final MongoTemplate mongoTemplate;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database:NOT_SET}")
    private String mongoDatabaseProperty;

    public NotificationService(NotificationRepository repo, MongoTemplate mongoTemplate) {
        this.repo = repo;
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void logResolvedMongoUri() {
        log.info("### spring.data.mongodb.uri property = {}", mongoUri);
        log.info("### spring.data.mongodb.database property = {}", mongoDatabaseProperty);
        log.info("### MongoTemplate is ACTUALLY connected to database = {}", mongoTemplate.getDb().getName());
    }

    public void notifyTaskCreated(TaskCreatedEvent event) {
        if (event.getAssignedToId().equals(event.getCreatedById())) return;
        save(event.getAssignedToId(),
             event.getCreatedByName() + " assigned you \"" + event.getTitle() + "\"",
             Notification.Type.TASK_ASSIGNED);
    }

    public void notifyTaskAssigned(TaskAssignedEvent event) {
        if (event.getAssignedToId().equals(event.getCreatedById())) return;
        save(event.getAssignedToId(),
             event.getCreatedByName() + " assigned you \"" + event.getTitle() + "\"",
             Notification.Type.TASK_ASSIGNED);
    }

    public void notifyTaskCompleted(TaskCompletedEvent event) {
        if (event.getCompletedById().equals(event.getCreatedById())) return;
        save(event.getCreatedById(),
             event.getCompletedByName() + " completed \"" + event.getTitle() + "\"",
             Notification.Type.TASK_COMPLETED);
    }

    public void notifyTaskUpdated(TaskUpdatedEvent event) {
        if (event.getAssignedToId() == null || event.getUpdatedById().equals(event.getAssignedToId())) return;
        save(event.getAssignedToId(),
             event.getUpdatedByName() + " updated \"" + event.getTitle() + "\"",
             Notification.Type.TASK_UPDATED);
    }

    public void notifyFriendRequestSent(FriendRequestSentEvent event) {
        save(event.getReceiverId(),
             event.getRequesterName() + " sent you a friend request",
             Notification.Type.FRIEND_REQUEST_SENT);
    }

    public void notifyFriendRequestAccepted(FriendRequestAcceptedEvent event) {
        save(event.getRequesterId(),
             event.getAccepterName() + " accepted your friend request",
             Notification.Type.FRIEND_REQUEST_ACCEPTED);
    }

    private void save(String userId, String message, Notification.Type type) {
        log.info("### Attempting to save notification for userId={} message='{}'", userId, message);
        try {
            Notification saved = repo.save(new Notification(userId, message, type));
            log.info("### Save call returned successfully, generated id={}", saved.getId());
        } catch (Exception e) {
            log.error("### Save call threw an exception", e);
            throw e;
        }
    }
    
}
