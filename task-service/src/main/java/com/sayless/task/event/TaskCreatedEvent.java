package com.sayless.task.event;

public class TaskCreatedEvent {

    private String taskId;
    private String title;
    private String assignedToId;
    private String createdById;

    public TaskCreatedEvent() {
    }

    public TaskCreatedEvent(String taskId,
                            String title,
                            String assignedToId,
                            String createdById) {
        this.taskId = taskId;
        this.title = title;
        this.assignedToId = assignedToId;
        this.createdById = createdById;
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

    public String getAssignedToId() {
        return assignedToId;
    }

    public void setAssignedToId(String assignedToId) {
        this.assignedToId = assignedToId;
    }

    public String getCreatedById() {
        return createdById;
    }

    public void setCreatedById(String createdById) {
        this.createdById = createdById;
    }
}