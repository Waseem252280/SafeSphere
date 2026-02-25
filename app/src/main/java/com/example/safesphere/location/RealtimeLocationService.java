package com.example.safesphere.location;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.safesphere.R;
import com.google.android.gms.location.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RealtimeLocationService extends Service {


    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                for (android.location.Location location : locationResult.getLocations()) {
                        /** must be uncomment to update location on db **/
                                        updateLocationToFirebase(location);
                }
            }
        };
    }

    // ✅ Normal Notification banane ka method (Jo pehle miss tha)
    private Notification buildNormalNotification() {
        return new NotificationCompat.Builder(this, "loc_channel")
                .setContentTitle("SafeSphere Active")
                .setContentText("Protecting you in the background...")
                .setSmallIcon(R.drawable.ic_location)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        createNotificationChannel();
//
//        // Pehle default notification dikha dein
//        startForeground(1, buildNormalNotification());
//
//        // Phir check karein GPS status
//        checkGpsStatusAndNotify();
//        startLocationUpdates();
//
//        // Agar GpsStatusReceiver ne signal bheja ho
//        if (intent != null && "CHECK_GPS_STATUS".equals(intent.getAction())) {
//            checkGpsStatusAndNotify();
//        }
//
//        return START_STICKY;
//    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        // Fix: Android 14+ requirements for location type
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, buildNormalNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(1, buildNormalNotification());
        }

        checkGpsStatusAndNotify();
        startLocationUpdates();

        if (intent != null && "CHECK_GPS_STATUS".equals(intent.getAction())) {
            checkGpsStatusAndNotify();
        }

        return START_STICKY;
    }

    private void checkGpsStatusAndNotify() {
        android.location.LocationManager locationManager = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);

        if (!isGpsEnabled) {
            updateStatusToFirestore("GPS Disabled");
            showGpsWarningNotification();
        } else {
            // ✅ Ab ye error nahi dega kyunke method niche add kar diya hai
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, buildNormalNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(1, buildNormalNotification());
            }
            updateStatusToFirestore("Online");
        }
    }

    //    private FusedLocationProviderClient fusedLocationClient;
//    private LocationCallback locationCallback;
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
//
//        locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//                if (locationResult == null) return;
//                for (android.location.Location location : locationResult.getLocations()) {
//                    updateLocationToFirebase(location);
//                }
//            }
//        };
//        checkGpsStatusAndNotify();
//    }

    private void updateLocationToFirebase(android.location.Location loc) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // Get City and Address Name
        String cityName = "Unknown";
        String locationName = "Unknown Address";
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
            if (!addresses.isEmpty()) {
                cityName = addresses.get(0).getLocality();
                locationName = addresses.get(0).getAddressLine(0);
            }
        } catch (Exception e) { e.printStackTrace(); }

        Map<String, Object> updates = new HashMap<>();
        updates.put("latitude", loc.getLatitude());
        updates.put("longitude", loc.getLongitude());
        updates.put("locationName", locationName);
        updates.put("status", "Online (" + cityName + ")");
        updates.put("lastSeen", com.google.firebase.Timestamp.now());

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .update(updates);
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(3000)          // 3 sec
                .setFastestInterval(1000)   // fastest
                .setSmallestDisplacement(0) // movement optional
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // 💡 Add failure listener to debug
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                    .addOnFailureListener(e -> {
                        android.util.Log.e("LOC_ERROR", "Location update failed: " + e.getMessage());
                    });
        }
    }

    private void updateStatusToFirestore(String status) {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update("status", status);
        }
    }

    private void showGpsWarningNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        // Jab user click kare to HomeActivity khule jahan GPS Dialog chal sakay
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                this, 0, new Intent(this, com.example.safesphere.activity.HomeActivity.class),
                android.app.PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, "loc_channel")
                .setContentTitle("⚠️ GPS is OFF")
                .setContentText("Emergency tracking is disabled. Click to fix.")
                .setSmallIcon(R.drawable.ic_location)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();

        manager.notify(1, notification);
    }
    private void createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel servicetChannel = new NotificationChannel(
                    "loc_channel", "Location Service Channel", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(servicetChannel);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Jab app kill ho, tab ye trigger hoga
        Intent restartServiceIntent = new Intent(getApplicationContext(), this.getClass());
        restartServiceIntent.setPackage(getPackageName());

        // PendingIntent ke zariye system ko batayein ke 1 second baad service dubara start kare
        android.app.PendingIntent restartServicePendingIntent = android.app.PendingIntent.getService(
                getApplicationContext(), 1, restartServiceIntent,
                android.app.PendingIntent.FLAG_ONE_SHOT | android.app.PendingIntent.FLAG_IMMUTABLE);

        android.app.AlarmManager alarmService = (android.app.AlarmManager) getApplicationContext().getSystemService(android.content.Context.ALARM_SERVICE);
        alarmService.set(android.app.AlarmManager.ELAPSED_REALTIME, android.os.SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent);

        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Service kill hote hi broadcast bhej dein
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restart_service");
        broadcastIntent.setClass(this, ServiceRestartReceiver.class);
        this.sendBroadcast(broadcastIntent);
    }
}