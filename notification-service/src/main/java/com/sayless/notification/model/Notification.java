package com.sayless.notification.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "notifications")
public class Notification {
    @Id
    private String id;
    private String userId;
    private String message;
    private Type type;
    private boolean read;
    private Instant createdAt;

    public enum Type {
        TASK_ASSIGNED, TASK_COMPLETED, TASK_UPDATED,
        FRIEND_REQUEST_SENT, FRIEND_REQUEST_ACCEPTED
    }

    public Notification() {}

    public Notification(String userId, String message, Type type) {
        this.userId = userId;
        this.message = message;
        this.type = type;
        this.read = false;
        this.createdAt = Instant.now();
    }

    public String getId() {
        return id; 
    }
    public void setId(String id) { 
        this.id = id; 
    }
    public String getUserId() { 
        return userId; 
    }
    public void setUserId(String userId) { 
        this.userId = userId; 
    }
    public String getMessage() { 
        return message; 
    }
    public void setMessage(String message) { 
        this.message = message; 
    }
    public Type getType() { 
        return type; 
    }
    public void setType(Type type) { 
        this.type = type; 
    }
    public boolean isRead() { 
        return read; 
    }
    public void setRead(boolean read) { 
        this.read = read; 
    }
    public Instant getCreatedAt() { 
        return createdAt; 
    }
    public void setCreatedAt(Instant createdAt) { 
        this.createdAt = createdAt; 
    }
}