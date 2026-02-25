package com.example.safesphere.dto;

public class UserStatusDto {

    private boolean online;
    private boolean typing;
    private long lastSeen;

    public UserStatusDto() {
    }

    public UserStatusDto(boolean online, boolean typing, long lastSeen) {
        this.online = online;
        this.typing = typing;
        this.lastSeen = lastSeen;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isTyping() {
        return typing;
    }

    public void setTyping(boolean typing) {
        this.typing = typing;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
}
