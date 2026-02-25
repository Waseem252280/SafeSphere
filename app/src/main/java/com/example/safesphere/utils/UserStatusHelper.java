//package com.example.safesphere.utils;
//
//import androidx.annotation.NonNull;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ServerValue;
//import com.google.firebase.database.ValueEventListener;
//import java.util.HashMap;
//import java.util.Map;
//
//public class UserStatusHelper {
//    private static ValueEventListener connectivityListener;
//    private static DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
//
//    public static void detectUserStatus() {
//        FirebaseAuth auth = FirebaseAuth.getInstance();
//        if (auth.getCurrentUser() == null) return;
//
//        String uid = auth.getCurrentUser().getUid();
//        DatabaseReference statusRef = FirebaseDatabase.getInstance().getReference("status").child(uid);
//
//        statusRef.child("loggedIn").addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                // Agar user naya hai to loggedIn node create karein
//                if (!snapshot.exists()) {
//                    statusRef.child("loggedIn").setValue(true);
//                    return;
//                }
//
//                Boolean isLoggedIn = snapshot.getValue(Boolean.class);
//                if (Boolean.TRUE.equals(isLoggedIn)) {
//                    startPresenceSystem(statusRef);
//                } else {
//                    stopPresenceSystem();
//                    statusRef.child("online").setValue(false);
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {}
//        });
//    }
//
//    private static void startPresenceSystem(DatabaseReference statusRef) {
//        if (connectivityListener != null) return;
//
//        connectivityListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                boolean connected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
//                if (connected) {
//                    statusRef.child("online").setValue(true);
//                    Map<String, Object> offlineMap = new HashMap<>();
//                    offlineMap.put("online", false);
//                    offlineMap.put("lastSeen", ServerValue.TIMESTAMP);
//                    statusRef.onDisconnect().updateChildren(offlineMap);
//                } else {
//                    statusRef.child("online").setValue(false);
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {}
//        };
//        connectedRef.addValueEventListener(connectivityListener);
//    }
//
//    private static void stopPresenceSystem() {
//        if (connectivityListener != null) {
//            connectedRef.removeEventListener(connectivityListener);
//            connectivityListener = null;
//        }
//    }
//}





package com.example.safesphere.utils;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class UserStatusHelper {

    private static ValueEventListener connectivityListener;
    private static DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");

    public static void detectUserStatus() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        DatabaseReference statusRef = FirebaseDatabase.getInstance().getReference("status").child(uid);

        // Ensure this runs on main thread listener but initialization in background thread
        statusRef.child("loggedIn").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {
                    // Create loggedIn node for new user
                    statusRef.child("loggedIn").setValue(true);
                }

                Boolean isLoggedIn = snapshot.getValue(Boolean.class);
                if (Boolean.TRUE.equals(isLoggedIn)) {
                    startPresenceSystem(statusRef);
                } else {
                    stopPresenceSystem();
                    statusRef.child("online").setValue(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Optional logging
            }
        });
    }

    private static void startPresenceSystem(DatabaseReference statusRef) {
        if (connectivityListener != null) return;

        connectivityListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);

                if (Boolean.TRUE.equals(connected)) {
                    // Online
                    statusRef.child("online").setValue(true);

                    Map<String, Object> offlineMap = new HashMap<>();
                    offlineMap.put("online", false);
                    offlineMap.put("lastSeen", ServerValue.TIMESTAMP);

                    // Safe disconnect update
                    statusRef.onDisconnect().updateChildren(offlineMap);
                } else {
                    // Offline
                    statusRef.child("online").setValue(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Optional logging
            }
        };

        connectedRef.addValueEventListener(connectivityListener);
    }

    private static void stopPresenceSystem() {
        if (connectivityListener != null) {
            connectedRef.removeEventListener(connectivityListener);
            connectivityListener = null;
        }
    }
}
