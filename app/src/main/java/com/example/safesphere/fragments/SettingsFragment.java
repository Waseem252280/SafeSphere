package com.example.safesphere.fragments;

import com.example.safesphere.R;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.safesphere.auth.AuthActivity;
import com.example.safesphere.utils.SharedPrefferanceUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private CheckBox checkboxSafe;
    private CardView cardAddContact, cardManageContacts, cardEditProfile, cardLogout;
    private FirebaseFirestore db;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        initViews(view);
        fetchUserSafetyStatus();
        setupClickListeners();

        return view;
    }

    private void initViews(View v) {
        checkboxSafe = v.findViewById(R.id.checkbox_safe);
        // In IDs ko apne XML Cards mein lazmi add kar lena
        cardAddContact = v.findViewById(R.id.cardAddContact);
        cardManageContacts = v.findViewById(R.id.cardManageContacts);
        cardEditProfile = v.findViewById(R.id.cardEditProfile);
        cardLogout = v.findViewById(R.id.cardLogout);
    }

    private void setupClickListeners() {
        // 1. Mark as Safe logic
        checkboxSafe.setOnClickListener(v -> {
            boolean isChecked = checkboxSafe.isChecked();
            handleSafetyToggle(isChecked);
        });

        // 2. Add New Trusted Contact
        cardAddContact.setOnClickListener(v -> {
            // Click logic here
            // 2. Add New Trusted Contact
                // Hum null pass kar rahe hain kyunke hum Naya contact add kar rahe hain, update nahi.
                AddContactFragment addFragment = AddContactFragment.newInstance(null);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                                android.R.anim.slide_in_left, android.R.anim.slide_out_right) // Smooth Transition
                        .replace(R.id.fragment_container, addFragment) // R.id.fragment_container aapki main activity ka container hona chahiye
                        .addToBackStack(null) // Back button dabane par wapas settings pe aane ke liye
                        .commit();
        });

        // 3. Manage Trusted Contacts
        cardManageContacts.setOnClickListener(v -> {
            ContactListFragment listFragment = new ContactListFragment();

            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            android.R.anim.slide_in_left,
                            android.R.anim.slide_out_right,
                            android.R.anim.slide_in_left,
                            android.R.anim.slide_out_right
                    ) // Smooth sliding animation
                    .replace(R.id.fragment_container, listFragment) // ID wahi jo aapki activity mein hai
                    .addToBackStack(null) // Back button se wapas settings par aane ke liye
                    .commit();
        });

        // 4. Edit Profile
        cardEditProfile.setOnClickListener(v -> openEditProfile());

        // 5. Logout
        cardLogout.setOnClickListener(v -> {
            handleLogout();
        });
    }

    private void openEditProfile() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new UpdateProfileFragment())
                .addToBackStack(null)
                .commit();
    }

    private void handleLogout() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            DatabaseReference statusRef = FirebaseDatabase.getInstance()
                    .getReference("status")
                    .child(uid);

            // ✅ Step 1: Sabse pehle database ko "Offline" aur "LoggedOut" mode mein force karein
            Map<String, Object> map = new HashMap<>();
            map.put("online", false);
            map.put("loggedIn", false); // Ye master switch hai jo UserStatusHelper ko rok dega
            map.put("lastSeen", ServerValue.TIMESTAMP);

            statusRef.updateChildren(map)
                    .addOnCompleteListener(task -> {
                        // ✅ Step 2: Database update ho gaya, ab Auth sign out karein
                        auth.signOut();

                        // Step 3: Local storage (SharedPrefs) clear karein (Agar aapka helper method hai)
                        SharedPrefferanceUtil.clearUserData(requireContext());
                        SharedPrefferanceUtil.clearTrustedContactData(requireContext());

                        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();

                        // Step 4: Activity transition
                        Intent intent = new Intent(requireContext(), AuthActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        requireActivity().finish();
                    });
        }
    }


    private void fetchUserSafetyStatus() {
        if (currentUserId == null) return;

        db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isSafe = documentSnapshot.getBoolean("isSafe");
                        checkboxSafe.setChecked(isSafe != null && isSafe);
                    }
                });
    }

    private void handleSafetyToggle(boolean shouldBeSafe) {
        if (!isInternetAvailable()) {
            checkboxSafe.setChecked(!shouldBeSafe); // Revert state
            showToast("Please connect to the internet");
            return;
        }

        if (currentUserId != null) {
            db.collection("users").document(currentUserId)
                    .update("isSafe", shouldBeSafe)
                    .addOnSuccessListener(aVoid -> {
                        String status = shouldBeSafe ? "Safe mode enabled" : "Safe mode disabled";
                        showToast(status);
                    })
                    .addOnFailureListener(e -> {
                        checkboxSafe.setChecked(!shouldBeSafe); // Revert on failure
                        showToast("Update failed: " + e.getMessage());
                    });
        }
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        showToast("Logged out successfully");
        // Navigation logic aap khud add kar lena
    }

    // Internet Connectivity Helper
    private boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return capabilities != null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }
}