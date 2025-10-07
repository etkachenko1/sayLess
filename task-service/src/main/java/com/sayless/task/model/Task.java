package com.sayless.task.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.Objects;
import java.time.Instant;

@Document(collection = "tasks")
public class Task {
    @Id
    private String id;
    private String title;
    private String description;
    private Status status;
    private String assignedTo;
    private String createdBy;
    
    private Instant deadline;
    private Instant createdAt;
    private Instant updatedAt;
    
    public enum Status {TODO, IN_PROGRESS, DONE}

    public Task(){}
    public Task(String id, String title, String description, Status status, String assignedTo, String createdBy, Instant deadline, Instant createdAt, Instant updatedAt){
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.assignedTo = assignedTo;
        this.createdBy = createdBy;
        this.deadline = deadline;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId(){ return id;}
    public void setId(String id) {this.id = id;}

    public String getTitle() {return title;}
    public void setTitle(String title) {this.title = title;}

    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}

    public Status getStatus() {return status;}
    public void setStatus(Status status) {this.status = status;}

    public String getAssignedTo() {return assignedTo;}
    public void setAssignedTo(String assignedTo) {this.assignedTo = assignedTo;}

    public String getCreatedBy() {return createdBy;}
    public void setCreatedBy(String createdBy) {this.createdBy = createdBy;}
    
    public Instant getDeadline() {return deadline;}
    public void setDeadline(Instant deadline) {this.deadline = deadline;}

    public Instant getCreatedAt() {return createdAt;}
    public void setCreatedAt(Instant createdAt) {this.createdAt = createdAt;}

    public Instant getUpdatedAt() {return updatedAt;}
    public void setUpdatedAt(Instant updatedAt) {this.updatedAt = updatedAt;}


    @Override
    public String toString() {
        return "Task{" + "id='" +id + '\'' + ", title = '" + title + '\'' 
        + ", description='" + description + '\'' 
        + ", status=" + status 
        + ", assignedTo= '" + assignedTo + '\'' 
        + ", createdBy= '" + createdBy + '\''
        + ", deadline= " + deadline
        + ", createdAt=" + createdAt 
        + ", updatedAt=" + updatedAt + '}';

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true; //same object
        if(!(o instanceof Task task)) return false; //same type
        return Objects.equals(id, task.id); //otherwise compare id
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
