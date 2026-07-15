package com.sayless.notification.event;

public class TaskCompletedEvent {
    private String taskId;
    private String title;
    private String completedById;
    private String completedByName;
    private String createdById;
    private String createdByName;
    public TaskCompletedEvent(){}

    public TaskCompletedEvent(String taskId,
                            String title,
                            String completedById,
                            String completedByName,
                            String createdById,
                            String createdByName){
        this.taskId = taskId;
        this.title = title;
        this.completedById = completedById;
        this.completedByName = completedByName;
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
