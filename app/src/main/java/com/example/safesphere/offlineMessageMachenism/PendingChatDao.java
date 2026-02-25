package com.example.safesphere.offlineMessageMachenism;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PendingChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PendingChatEntity chat);

    @Query("SELECT * FROM pending_chats WHERE chatRoomId = :roomId")
    List<PendingChatEntity> getPendingMessages(String roomId);

    @Query("DELETE FROM pending_chats WHERE messageId = :msgId")
    void deleteById(String msgId);

    @Query("UPDATE pending_chats SET message=:text WHERE messageId=:id")
    void updateMessage(String id, String text);

    @Query("SELECT * FROM pending_chats WHERE messageId = :msgId LIMIT 1")
    PendingChatEntity getByMessageId(String msgId);


}
