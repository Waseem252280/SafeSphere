package com.example.safesphere.dto;

public class NotificationDto {

    private String id; // Firestore document ID
    private String senderId;
    private String relationRequested;
    private String status;
    private String senderName;
    private String senderPhoto;
    private String senderGender;
    private String recieverId;

    public NotificationDto() {}

    public NotificationDto(String senderId, String relationRequested, String status, String senderName,
                           String senderPhoto, String senderGender, String recieverId) {
        this.senderId = senderId;
        this.relationRequested = relationRequested;
        this.status = status;
        this.senderName = senderName;
        this.senderPhoto = senderPhoto;
        this.senderGender = senderGender;
        this.recieverId = recieverId;
    }

    // Getters and setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getRelationRequested() { return relationRequested; }
    public void setRelationRequested(String relationRequested) { this.relationRequested = relationRequested; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderPhoto() { return senderPhoto; }
    public void setSenderPhoto(String senderPhoto) { this.senderPhoto = senderPhoto; }

    public String getSenderGender() { return senderGender; }
    public void setSenderGender(String senderGender) { this.senderGender = senderGender; }

    public String getRecieverId() { return recieverId; }
    public void setRecieverId(String recieverId) { this.recieverId = recieverId; }
}
