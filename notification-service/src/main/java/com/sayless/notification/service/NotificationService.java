package com.sayless.notification.service;

import com.sayless.notification.event.*;
import com.sayless.notification.model.Notification;
import com.sayless.notification.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

// determines who gets the notification
//each method resolves the recepient of notification differently.
// - assignment notifications to assignedToId
// - completion notifications to createdById
// - accepted request notifications to requesterId
//guards against self notifications

@Service
public class NotificationService {

    private final NotificationRepository repo;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository repo, SimpMessagingTemplate messagingTemplate) {
        this.repo = repo;
        this.messagingTemplate = messagingTemplate;
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
        Notification saved = repo.save(new Notification(userId, message, type));
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", saved);
    }

}