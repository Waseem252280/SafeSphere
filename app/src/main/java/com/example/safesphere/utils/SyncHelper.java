package com.example.safesphere.utils;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.Map;

public class SyncHelper {
    private static final String TAG = "SyncHelper";
    private ListenerRegistration listener;

    public void startSync() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        DatabaseReference realtimeDb = FirebaseDatabase.getInstance().getReference("users");

        // ✅ SIRF Firestore se data utha kar Realtime DB mein dalna hai
        listener = firestore.collection("users")
                .addSnapshotListener((querySnapshot, e) -> {
                    if (e != null || querySnapshot == null) {
                        Log.e(TAG, "Listen failed", e);
                        return;
                    }

                    for (DocumentChange change : querySnapshot.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.REMOVED) continue;

                        String userId = change.getDocument().getId();
                        Map<String, Object> firestoreData = change.getDocument().getData();

                        // ✅ Realtime DB ko update karein
                        // Isse blinking Realtime DB mein hogi aur Firestore console free ho jayega
                        realtimeDb.child(userId).updateChildren(firestoreData)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Sync to RTDB Success: " + userId))
                                .addOnFailureListener(err -> Log.e(TAG, "Sync to RTDB Failed", err));
                    }
                });
    }

    public void stopSync() {
        if (listener != null) listener.remove();
    }
}