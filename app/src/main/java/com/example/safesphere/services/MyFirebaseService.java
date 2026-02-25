package com.example.safesphere.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.safesphere.utils.FirebaseUtil;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseService extends FirebaseMessagingService {

    private static final String TAG = "FCM";

    // 🔹 Jab Firebase naya token generate karega (auto call hota hai)
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        saveTokenToFirestore(token);
    }

    // 🔹 Ye method Firestore me token save karta hai
    private static void saveTokenToFirestore(String token) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.w(TAG, "User not logged in, token not saved!");
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // ✅ Document exist karta hai → token update karo
                Map<String, Object> data = new HashMap<>();
                data.put("deviceToken", token);

                userRef.update(data)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Token successfully updated in Firestore"))
                        .addOnFailureListener(e -> Log.e(TAG, "Error updating token", e));
            } else {
                // ❌ Document exist nahi karta → skip karo
                Log.w(TAG, "User document does not exist, skipping token save.");
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to fetch user document", e));
    }

    // 🔹 Ye method tum manually Activity/Fragment me call kar sakte ho
    public static Task<Void> updateTokenManually() {
        TaskCompletionSource<Void> tcs = new TaskCompletionSource<>();

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    String userId = FirebaseUtil.getCurrentUser().getUid(); // apne util se user id lo
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .update("deviceToken", token)
                            .addOnSuccessListener(aVoid -> tcs.setResult(null))
                            .addOnFailureListener(tcs::setException);
                })
                .addOnFailureListener(tcs::setException);

        return tcs.getTask();
    }

}
