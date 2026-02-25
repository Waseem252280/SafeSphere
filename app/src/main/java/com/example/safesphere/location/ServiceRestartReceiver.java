package com.example.safesphere.location;

import android.content.Intent;

import androidx.core.content.ContextCompat;

import com.example.safesphere.activity.HomeActivity;

public class ServiceRestartReceiver extends android.content.BroadcastReceiver {
//    @Override
//    public void onReceive(android.content.Context context, android.content.Intent intent) {
//
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//            context.startForegroundService(new android.content.Intent(context, RealtimeLocationService.class));
//        } else {
//            context.startService(new android.content.Intent(context, RealtimeLocationService.class));
//        }
//    }

    @Override
    public void onReceive(android.content.Context context, android.content.Intent intent) {
        Intent serviceIntent = new android.content.Intent(context, RealtimeLocationService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // Foreground service start karein
            ContextCompat.startForegroundService(context, serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
