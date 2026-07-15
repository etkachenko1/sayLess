package com.sayless.notification.event;

public class TaskAssignedEvent {
    private String taskId;
    private String title;
    private String assignedToId;
    private String assignedToName;
    private String createdById;
    private String createdByName;
    public TaskAssignedEvent(){}

    public TaskAssignedEvent(String taskId,
                            String title,
                            String assignedToId,
                            String assignedToName,
                            String createdById,
                            String createdByName){
        this.taskId = taskId;
        this.title = title;
        this.assignedToId = assignedToId;
        this.assignedToName = assignedToName;
        this.createdById = createdById;
        this.createdByName = createdByName; }

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
}

