package com.example.safesphere.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import javax.annotation.Nullable;

public class FamilyLiveData {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private MutableLiveData<Integer> familyCountLiveData;

    public FamilyLiveData() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        familyCountLiveData = new MutableLiveData<>();
        listenFamilyCircle();
    }

    private void listenFamilyCircle() {
        String currentUserId = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(currentUserId)
                .collection("family_circle")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            familyCountLiveData.setValue(0);
                            return;
                        }

                        if (value != null) {
                            int count = value.size();
                            familyCountLiveData.setValue(count);
                        }
                    }
                });
    }

    public LiveData<Integer> getFamilyCountLiveData() {
        return familyCountLiveData;
    }
}

