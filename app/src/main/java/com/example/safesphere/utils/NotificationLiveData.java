package com.example.safesphere.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.safesphere.R;
import com.example.safesphere.activity.HomeActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import javax.annotation.Nullable;

public class NotificationLiveData {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private MutableLiveData<Integer> notificationsCountLiveData;
    private Context context; // Add context here

    // Constructor me context pass karein
    public NotificationLiveData(Context context) {
        this.context = context;
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        notificationsCountLiveData = new MutableLiveData<>();
        listenForNotifications();
    }

    private void listenForNotifications() {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("notifications")
                .whereEqualTo("recieverId", currentUserId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            notificationsCountLiveData.setValue(0);
                            return;
                        }

                        if (value != null) {
                            int count = value.size();
                            Integer previousCount = notificationsCountLiveData.getValue();
                            if (previousCount != null && count > previousCount) {
                                for (DocumentSnapshot doc : value.getDocuments()) {
                                    String recieverId = doc.getString("recieverId");
                                    if (recieverId.equals(currentUserId)) {
                                        playNotificationSound();
                                    }
                                }
                            }
                            notificationsCountLiveData.setValue(count);
                        }
                    }
                });
    }

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(context, notification); // use context here
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // LiveData observer ke liye getter
    public LiveData<Integer> getNotificationsCountLiveData() {
        return notificationsCountLiveData;
    }
}
