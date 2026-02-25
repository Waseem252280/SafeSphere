package com.example.safesphere.dto;

import com.google.firebase.database.PropertyName;

import java.util.Map;

public class ChatDto {

    private String messageId;
    private String senderId;
    private String receiverId;
    private String message;            // text message / media url
    private String messageType;        // text | image | video | audio
    private long timestamp;
    private String status;              // sent | delivered | seen
    private Map<String, Boolean> deletedFor;

    //Voice message status by mic (ONLY for audio)
    private boolean listen;

    // 🔥 Dual Local Paths for Syncing (ONLY for media)
    private String senderLocalPath;
    private String receiverLocalPath;

    // Download states (ONLY for media)
    private boolean downloading;
    private boolean downloadFailed;

    private Boolean typing;    // Firebase se null safe
    private Boolean recording; // Firebase se null safe

    // file handling
    private String fileName;
    private String fileExtension;

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Boolean getTyping() {
        return typing;
    }

    public void setTyping(Boolean typing) {
        this.typing = typing;
    }

    public Boolean getRecording() {
        return recording;
    }

    public void setRecording(Boolean recording) {
        this.recording = recording;
    }


    public String getMessageType() { return messageType; }

    public void setMessageType(String messageType) { this.messageType = messageType; }

    // Required empty constructor for Firebase
    public ChatDto() {}

    public ChatDto(String messageId,
                   String senderId,
                   String receiverId,
                   String message,
                   String messageType,
                   long timestamp,
                   String status) {

        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.messageType = messageType;
        this.timestamp = timestamp;
        this.status = status;
    }

    // -------- GETTERS & SETTERS (SAME NAMES) --------

    public boolean isListen() {
        return Boolean.TRUE.equals(listen);
    }

    public void setListen(boolean listen) {
        this.listen = listen;
    }

    public boolean isDownloading() {
        return Boolean.TRUE.equals(downloading);
    }

    public void setDownloading(boolean downloading) {
        this.downloading = downloading;
    }

    public boolean isDownloadFailed() {
        return Boolean.TRUE.equals(downloadFailed);
    }

    public void setDownloadFailed(boolean downloadFailed) {
        this.downloadFailed = downloadFailed;
    }

    public Map<String, Boolean> getDeletedFor() {
        return deletedFor;
    }

    public void setDeletedFor(Map<String, Boolean> deletedFor) {
        this.deletedFor = deletedFor;
    }

    public String getSenderLocalPath() {
        return senderLocalPath;
    }

    public void setSenderLocalPath(String senderLocalPath) {
        this.senderLocalPath = senderLocalPath;
    }

    public String getReceiverLocalPath() {
        return receiverLocalPath;
    }

    public void setReceiverLocalPath(String receiverLocalPath) {
        this.receiverLocalPath = receiverLocalPath;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
