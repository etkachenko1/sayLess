package com.sayless.notification.event;

import java.time.Instant;

public class TaskCompletedEvent {

    private String taskId;
    private String title;
    private String description;
    private Instant deadline;
    private String status;
    private String assignedToId;
    private String assignedToName;
    private String createdById;
    private String createdByName;
    private String completedById;
    private String completedByName;
    private Instant updatedAt;

    public TaskCompletedEvent() {
    }

    public TaskCompletedEvent(String taskId,
                            String title,
                            String description,
                            Instant deadline,
                            String status,
                            String assignedToId,
                            String assignedToName,
                            String createdById,
                            String createdByName,
                            String completedById,
                            String completedByName,
                            Instant updatedAt) {
        this.taskId = taskId;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.status = status;
        this.assignedToId = assignedToId;
        this.assignedToName = assignedToName;
        this.createdById = createdById;
        this.createdByName = createdByName;
        this.completedById = completedById;
        this.completedByName = completedByName;
        this.updatedAt = updatedAt;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(String assignedToId) {
        this.assignedToId = assignedToId;
    }

    public String getAssignedToName() {
        return assignedToName;
    }

    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }

    public String getCreatedById() {
        return createdById;
    }

    public void setCreatedById(String createdById) {
        this.createdById = createdById;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }

    public String getCompletedById() {
        return completedById;
    }

    public void setCompletedById(String completedById) {
        this.completedById = completedById;
    }

    public String getCompletedByName() {
        return completedByName;
    }

    public void setCompletedByName(String completedByName) {
        this.completedByName = completedByName;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
