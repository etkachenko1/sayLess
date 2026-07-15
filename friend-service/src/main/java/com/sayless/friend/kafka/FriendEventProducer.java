package com.sayless.friend.kafka;

import com.sayless.friend.event.FriendRequestSentEvent;
import com.sayless.friend.event.FriendRequestAcceptedEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FriendEventProducer {

    private static final String TOPIC = "friend-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final Logger log = LoggerFactory.getLogger(FriendEventProducer.class);


    public FriendEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishRequestSent(FriendRequestSentEvent event) {
        kafkaTemplate.send(TOPIC, event.getRequestId(), event);
        log.info("Published friend-request-sent event {}", event.getRequestId());

    }
    public void publishRequestAccepted(FriendRequestAcceptedEvent event) {
        kafkaTemplate.send(TOPIC, event.getRequestId(), event);
        log.info("Published friend-request-accepted event {}", event.getRequestId());
    }
   
}