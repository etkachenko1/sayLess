package com.sayless.notification.kafka;
import com.sayless.notification.event.FriendRequestSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class FriendEventListener {
      private static final Logger log = LoggerFactory.getLogger(FriendEventListener.class);

    @KafkaListener(topics = "friend-events", groupId = "notifications")
    public void consume(FriendRequestSentEvent event) {
        log.info("{} sent a friend request", event.getRequesterName());
    }
    
}
