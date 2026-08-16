package com.sayless.notification.event;

public class TaskDeletedEvent {

    private String taskId;
    private String createdById;
    private String assignedToId;

    public TaskDeletedEvent() {
    }

    public TaskDeletedEvent(String taskId, String createdById, String assignedToId) {
        this.taskId = taskId;
        this.createdById = createdById;
        this.assignedToId = assignedToId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getCreatedById() {
        return createdById;
    }

    public void setCreatedById(String createdById) {
        this.createdById = createdById;
    }

    public String getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(String assignedToId) {
        this.assignedToId = assignedToId;
    }
}
