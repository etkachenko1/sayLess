package com.sayless.notification.kafka;
import com.sayless.notification.event.*;
import com.sayless.notification.service.NotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@KafkaListener(topics = "friend-events", groupId = "notifications")
public class FriendEventListener {
    private static final Logger log = LoggerFactory.getLogger(FriendEventListener.class);
    private final NotificationService notificationService;
    
    public FriendEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }


    @KafkaHandler
    public void onRequestSent(FriendRequestSentEvent event) {
        log.info("{} sent you a friend request", event.getRequesterName());
        notificationService.notifyFriendRequestSent(event);

    }

    @KafkaHandler
    public void onRequestAccepted(FriendRequestAcceptedEvent event) {
        log.info("{} accepted your friend request", event.getAccepterName());
        notificationService.notifyFriendRequestAccepted(event);

    }
    
}
