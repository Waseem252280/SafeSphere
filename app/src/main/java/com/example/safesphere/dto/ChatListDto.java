package com.example.safesphere.dto;

import java.util.Map;

public class ChatListDto {

    private String chatRoomId;

    private String chatUserId;
    private String chatUserName;
    private String chatUserImage;
    private String senderId;

    private String lastMessage;
    private String lastMessageType;
    private long lastMessageTime;
    private String lastMessageStatus;

    private int unreadCount;

    // realtime status
    private boolean online;
    private long lastSeen;

    private long lastMessageSize;
    private long mediaDuration;
    private Map<String, Boolean> deletedFor;
    private boolean listen;


    private String email;

    private boolean typing;
    private boolean recording;

    private String lastMessageId;

    public String getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(String lastMessageId) {
        this.lastMessageId = lastMessageId;
    }


    public boolean isTyping() { return typing; }
    public void setTyping(boolean typing) { this.typing = typing; }

    public boolean isRecording() { return recording; }
    public void setRecording(boolean recording) { this.recording = recording; }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ChatListDto() {}

    // 👉 getters & setters

    public String getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public String getChatUserId() {
        return chatUserId;
    }

    public void setChatUserId(String chatUserId) {
        this.chatUserId = chatUserId;
    }

    public String getChatUserName() {
        return chatUserName;
    }

    public void setChatUserName(String chatUserName) {
        this.chatUserName = chatUserName;
    }

    public String getChatUserImage() {
        return chatUserImage;
    }

    public void setChatUserImage(String chatUserImage) {
        this.chatUserImage = chatUserImage;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessageType() {
        return lastMessageType;
    }

    public void setLastMessageType(String lastMessageType) {
        this.lastMessageType = lastMessageType;
    }

    public long getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(long lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public String getLastMessageStatus() {
        return lastMessageStatus;
    }

    public void setLastMessageStatus(String lastMessageStatus) {
        this.lastMessageStatus = lastMessageStatus;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public long getLastMessageSize() {
        return lastMessageSize;
    }

    public void setLastMessageSize(long lastMessageSize) {
        this.lastMessageSize = lastMessageSize;
    }

    public long getMediaDuration() {
        return mediaDuration;
    }

    public void setMediaDuration(long mediaDuration) {
        this.mediaDuration = mediaDuration;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public Map<String, Boolean> getDeletedFor() {
        return deletedFor;
    }

    public void setDeletedFor(Map<String, Boolean> deletedFor) {
        this.deletedFor = deletedFor;
    }

    public boolean isListen() {
        return listen;
    }

    public void setListen(boolean isListen) {
        listen = isListen;
    }
}
