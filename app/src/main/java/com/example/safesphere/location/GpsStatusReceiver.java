package com.example.safesphere.location;
import android.content.Intent;

import com.example.safesphere.location.RealtimeLocationService;

public class GpsStatusReceiver extends android.content.BroadcastReceiver {
    @Override
    public void onReceive(android.content.Context context, android.content.Intent intent) {
        if (android.location.LocationManager.PROVIDERS_CHANGED_ACTION.equals(intent.getAction())) {
            // Service ko batayein ke check kare
            Intent serviceIntent = new Intent(context, RealtimeLocationService.class);
            serviceIntent.setAction("CHECK_GPS_STATUS");
            context.startService(serviceIntent);
        }
    }
}