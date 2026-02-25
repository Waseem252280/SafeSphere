package com.example.safesphere.offlineMessageMachenism;
import androidx.annotation.NonNull;
import androidx.room.Dao;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;

import java.util.List;

@Entity(tableName = "pending_chats")
public class PendingChatEntity {
    @PrimaryKey
    @NonNull
    public String messageId;
    public String chatRoomId;
    public String senderId;
    public String receiverId;
    public String message;
    public String messageType;
    public long timestamp;
    public String senderLocalPath;
}
