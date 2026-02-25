//package com.example.safesphere;
//
//import android.app.Application;
//
//import androidx.annotation.NonNull;
//
//import com.example.safesphere.utils.UserStatusHelper;
//import com.facebook.FacebookSdk;
//import com.facebook.appevents.AppEventsLogger;
//import com.google.firebase.FirebaseApp;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ServerValue;
//import com.google.firebase.database.ValueEventListener;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class MyApplication extends Application {
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        //offline data persistence
//        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
//
//        // Firebase
//        FirebaseApp.initializeApp(this);
//
//        FacebookSdk.sdkInitialize(getApplicationContext());
//        AppEventsLogger.activateApp(this);
//
//        UserStatusHelper.detectUserStatus();
//    }
//
//}



package com.example.safesphere;

import android.app.Application;

import com.example.safesphere.services.CloudinaryService;
import com.example.safesphere.utils.UserStatusHelper;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase & persistence
        FirebaseApp.initializeApp(this);
        // Firebase ko batana ke data local cache kare aur offline bhejta rahe
        CloudinaryService.initCloudinary(this);
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        // Facebook SDK
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);

        // Detect user status on background thread to avoid ANR
        new Thread(UserStatusHelper::detectUserStatus).start();
    }
}
