package com.sayless.friend.model;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Document(collection = "friends")
public class Friends {
    @Id
    private String id;

    @Indexed
    private String requesterId;

    @Indexed
    private String receiverId;
    private Status status;
    private Instant createdAt;

    public enum Status {
        PENDING,
        ACCEPTED,
        DECLINED
    }
    public Friends(){}
    public Friends(String requesterId, String receiverId, Status status){
        this.requesterId = requesterId;
        this.receiverId = receiverId;
        this.status = status;
        this.createdAt = Instant.now();
    }
    
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public String getReceiverId() { return receiverId;}
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setStatus(Instant createdAt) { this.createdAt = createdAt; }
    
}
