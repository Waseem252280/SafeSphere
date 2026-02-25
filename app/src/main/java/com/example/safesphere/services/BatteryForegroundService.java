package com.example.safesphere.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.safesphere.R;
import com.example.safesphere.adapter.FamilyAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class BatteryForegroundService extends Service {

    private static final String CHANNEL_ID = "battery_channel";
    private Handler handler;
    private int lastBattery = -1;
    public static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotification();

        handler = new Handler();
        handler.post(batteryRunnable);
        isRunning = true;
    }

    private final Runnable batteryRunnable = new Runnable() {
        @Override
        public void run() {
            int battery = getBatteryPercentage();

            if (battery != lastBattery && battery != -1) {
                lastBattery = battery;
                updateBatteryToFirestore(battery);
            }

            // 🔁 check every 60 seconds (safe & reliable)
            handler.postDelayed(this, 60 * 1000);
        }
    };

    private int getBatteryPercentage() {
        BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        if (bm == null) return -1;
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
    }

    private void updateBatteryToFirestore(int battery) {

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        Map<String, Object> map = new HashMap<>();
        map.put("batteryPercentage", battery);
        map.put("lastBatteryUpdate", System.currentTimeMillis());
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update(map);
    }

    private void createNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Battery Sync",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class)
                    .createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SafeSphere running")
                .setContentText("Sharing battery status with family")
                .setSmallIcon(R.drawable.ic_battery)
                .setOngoing(true)
                .build();

        startForeground(101, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacks(batteryRunnable);
        }
        isRunning = false;
    }

}
