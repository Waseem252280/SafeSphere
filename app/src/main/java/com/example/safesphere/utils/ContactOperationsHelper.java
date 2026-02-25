package com.example.safesphere.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import com.example.safesphere.dto.TrustedContact;
import com.example.safesphere.dto.UserDto;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.List;

public class ContactOperationsHelper {
    private final Context context;
    private final FirebaseFirestore db;
    private final String uid;

    // Callback Interface updated with boolean status
    public interface ContactCallback {
        void onFinished(boolean success);
    }

    public ContactOperationsHelper(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.uid = FirebaseAuth.getInstance().getUid();
    }

    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    /**
     * Naya contact add karne ke liye
     */
    public void addContact(String name, String phone, ContactCallback callback) {
        if (!isInternetAvailable()) {
            showToast("Please connect to the internet first");
            callback.onFinished(false);
            return;
        }

        TrustedContact newContact = new TrustedContact(name, phone);
        db.collection("users").document(uid)
                .update("trustedList", FieldValue.arrayUnion(newContact))
                .addOnSuccessListener(aVoid -> {
                    // Local Storage Update
                    List<TrustedContact> list = SharedPrefferanceUtil.getTrustedContacts(context);
                    list.add(newContact);
                    SharedPrefferanceUtil.saveTrustedContacts(context, list);

                    callback.onFinished(true);
                })
                .addOnFailureListener(e -> {
                    showToast("Error adding contact: " + e.getMessage());
                    callback.onFinished(false);
                });
    }

    /**
     * Contact ko delete karne ke liye
     */
    public void deleteContact(TrustedContact contact, ContactCallback callback) {
        if (!isInternetAvailable()) {
            showToast("No internet connection");
            callback.onFinished(false);
            return;
        }

        db.collection("users").document(uid)
                .update("trustedList", FieldValue.arrayRemove(contact))
                .addOnSuccessListener(aVoid -> {
                    // Local Storage se remove karna (Phone number base par)
                    SharedPrefferanceUtil.deleteContact(context, contact.getPhone());
                    callback.onFinished(true);
                })
                .addOnFailureListener(e -> {
                    showToast("Error deleting: " + e.getMessage());
                    callback.onFinished(false);
                });
    }

    /**
     * Contact ko update karne ke liye (Batch use kiya hai taaki safety rahe)
     */
    public void updateContact(TrustedContact oldContact, String newName, String newPhone, ContactCallback callback) {
        if (!isInternetAvailable()) {
            showToast("No internet connection");
            callback.onFinished(false);
            return;
        }

        TrustedContact updatedContact = new TrustedContact(newName, newPhone);
        WriteBatch batch = db.batch();
        DocumentReference ref = db.collection("users").document(uid);

        // Firestore Array mein update ka seedha tareeka nahi hota,
        // isliye pehle purana remove karte hain aur fir naya add karte hain.
        batch.update(ref, "trustedList", FieldValue.arrayRemove(oldContact));
        batch.update(ref, "trustedList", FieldValue.arrayUnion(updatedContact));

        batch.commit().addOnSuccessListener(aVoid -> {
            // Local Sync logic
            List<TrustedContact> list = SharedPrefferanceUtil.getTrustedContacts(context);
            // Purana phone number match karke remove karo
            list.removeIf(c -> c.getPhone().equals(oldContact.getPhone()));
            // Naya wala add karo
            list.add(updatedContact);
            SharedPrefferanceUtil.saveTrustedContacts(context, list);

            callback.onFinished(true);
        }).addOnFailureListener(e -> {
            showToast("Update failed: " + e.getMessage());
            callback.onFinished(false);
        });
    }

    /**
     * Server se local storage sync karne ke liye
     */
    public void syncFromServer(ContactCallback callback) {
        if (!isInternetAvailable() || uid == null) {
            callback.onFinished(false);
            return;
        }

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                UserDto user = doc.toObject(UserDto.class);
                if (user != null && user.getTrustedList() != null) {
                    SharedPrefferanceUtil.saveTrustedContacts(context, user.getTrustedList());
                    callback.onFinished(true);
                } else {
                    callback.onFinished(false);
                }
            }
        }).addOnFailureListener(e -> callback.onFinished(false));
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}