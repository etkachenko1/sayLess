package com.sayless.task.event;

public class TaskUpdatedEvent {
    private String taskId;
    private String title;
    private String assignedToId;
    private String updatedById;
    private String updatedByName;
    public TaskUpdatedEvent(){}

    public TaskUpdatedEvent(String taskId,
                            String title,
                            String assignedToId,
                            String updatedById,
                            String updatedByName){
        this.taskId = taskId;
        this.title = title;
        this.assignedToId = assignedToId;
        this.updatedById = updatedById;
        this.updatedByName = updatedByName; }

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

    public String getUpdatedById() {
        return updatedById;
    }

    public void setUpdatedById(String updatedById) {
        this.updatedById = updatedById;
    }

    public String getUpdatedByName() {
        return updatedByName;
    }

    public void setUpdatedByName(String updatedByName) {
        this.updatedByName = updatedByName;
    }
}


                            

