package com.sayless.notification.event;

public class FriendRequestAcceptedEvent {
    private String requestId;
    private String requesterId;
    private String requesterName;
    private String accepterId;
    private String accepterName;

    public FriendRequestAcceptedEvent(){}
    public FriendRequestAcceptedEvent(String requestId, String requesterId, 
        String requesterName, String accepterId, String accepterName) {
        this.requestId = requestId;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.accepterName = accepterName;
        this.accepterId = accepterId;
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
    public String getAccepterId() { 
        return accepterId; 
    }
    public void setAccepterId(String accepterId) { 
        this.accepterId = accepterId; 
    }
    public String getAccepterName() { 
        return accepterName; 
    }
    public void setAccepterName(String accepterName) { 
        this.accepterName = accepterName; 
    }


}
