package com.example.safesphere.offlineMessageMachenism;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.safesphere.offlineMessageMachenism.PendingChatDao;
import com.example.safesphere.offlineMessageMachenism.PendingChatEntity;

@Database(entities = {PendingChatEntity.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    public abstract PendingChatDao pendingChatDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "safesphere_db").build();
        }
        return instance;
    }
}