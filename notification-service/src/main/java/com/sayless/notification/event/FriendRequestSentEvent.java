package com.sayless.notification.event;


public class FriendRequestSentEvent {
    private String requestId;
    private String requesterId;
    private String requesterName;
    private String receiverId;

    public FriendRequestSentEvent(){}
    public FriendRequestSentEvent(String requestId, String requesterId, 
        String requesterName, String receiverId) {
        this.requestId = requestId;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.receiverId = receiverId;
    }

    public String getRequestId() { 
        return requestId; 
    }
    public void setRequestId(String requestId) { 
        this.requestId = requestId; 
    }
    public String getRequesterId() { 
        return requesterId; 
    }
    public void setRequesterId(String requesterId) { 
        this.requesterId = requesterId; 
    }
    public String getRequesterName() { 
        return requesterName; 
    }
    public void setRequesterName(String requesterName) { 
        this.requesterName = requesterName; 
    }
    public String getReceiverId() { 
        return receiverId; 
    }
    public void setReceiverId(String receiverId) { 
        this.receiverId = receiverId; 
    }


}
