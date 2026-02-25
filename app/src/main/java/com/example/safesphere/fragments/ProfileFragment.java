package com.example.safesphere.fragments;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.safesphere.R;
import com.example.safesphere.activity.HomeActivity;
import com.example.safesphere.auth.AuthActivity;
import com.example.safesphere.dto.UserDto;
import com.example.safesphere.utils.FirebaseUtil;
import com.example.safesphere.utils.SharedPrefferanceUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {

    private TextView tvFullName, tvEmail, tvGender;
    private CircleImageView profileImage;
    private View btnLogout, btnEditProfile;
    private UserDto user;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).hideNavigation();
        }

        initViews(view);
        loadUserData();
        setUserDataToViews();
        setupClickListeners();
    }

    /**
     * Initialize all UI elements.
     */
    private void initViews(View view) {
        tvFullName = view.findViewById(R.id.tvFullName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvGender = view.findViewById(R.id.tvGender);
        profileImage = view.findViewById(R.id.profileImage);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

    }

    /**
     * Load user data from SharedPreferences.
     */
    private void loadUserData() {
        user = SharedPrefferanceUtil.isUserLoggedIn(requireContext());
    }

    /**
     * Set user details into TextViews and ImageView.
     */
    private void setUserDataToViews() {
        if (user != null) {
            tvFullName.setText(user.getFullName());
            tvEmail.setText(user.getEmail());
            tvGender.setText(user.getGender());

            if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
                Picasso.get()
                        .load(user.getPhotoUrl())
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.ic_person);
            }
        } else {
            Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Set all click listeners for buttons.
     */
    private void setupClickListeners() {
        btnLogout.setOnClickListener(v -> handleLogout());
        btnEditProfile.setOnClickListener(v -> openEditProfile());
        profileImage.setOnClickListener(v -> requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new FullScreenImageFragment(user.getPhotoUrl()))
                        .addToBackStack(null)
                        .commit()
                );
    }

    /**
     * Handle logout and redirect to Login screen.
     */
//    private void handleLogout() {
//        FirebaseAuth auth = FirebaseAuth.getInstance();
//        if (auth.getCurrentUser() != null) {
//            String uid = auth.getCurrentUser().getUid();
//            DatabaseReference statusRef = FirebaseDatabase.getInstance()
//                    .getReference("status")
//                    .child(uid);
//
//            // ✅ Sab kuch ek saath update karein
//            Map<String, Object> map = new HashMap<>();
//            map.put("online", false);
//            map.put("loggedIn", false); // <--- Ye master switch hai
//            map.put("lastSeen", ServerValue.TIMESTAMP);
//
//            statusRef.updateChildren(map)
//                    .addOnSuccessListener(aVoid -> {
//                        // Database update hone ke baad Auth se sign out karein
//                        auth.signOut();
//
//                        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
//
//                        // Redirect to login screen
//                        Intent intent = new Intent(requireContext(), AuthActivity.class);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//                        startActivity(intent);
//                        requireActivity().finish();
//                    })
//                    .addOnFailureListener(e -> {
//                        Toast.makeText(requireContext(), "Logout failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                    });
//        }
//    }


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



    //    private void handleLogout() {
////        UserStatusHelper.setOfflineOnLogout();
////        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
////        FirebaseUtil.signOut(requireActivity());
////        // ✅ Redirect to LoginActivity if exists
////        Intent intent = new Intent(requireContext(), AuthActivity.class);
////        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
////        startActivity(intent);
////        requireActivity().finish();
//            // Update status in Firebase before logging out
//            FirebaseAuth auth = FirebaseAuth.getInstance();
//            if (auth.getCurrentUser() != null) {
//                String uid = auth.getCurrentUser().getUid();
//                DatabaseReference statusRef = FirebaseDatabase.getInstance()
//                        .getReference("status")
//                        .child(uid);
//
//                Map<String, Object> map = new HashMap<>();
//                map.put("online", false);
//                map.put("lastSeen", ServerValue.TIMESTAMP);
//
//                statusRef.updateChildren(map)
//                        .addOnSuccessListener(aVoid -> {
//                            // Now sign out the user
//                            FirebaseAuth.getInstance().signOut();
//                            // Redirect to login screen
//                            Intent intent = new Intent(requireContext(), AuthActivity.class);
//                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
//                            startActivity(intent);
//                            requireActivity().finish();
//                        })
//                        .addOnFailureListener(e -> {
//                            // Handle failure if needed
//                            Toast.makeText(requireContext(), "Logout failed", Toast.LENGTH_SHORT).show();
//                        });
//            }
//    }

    /**
     * Handle Edit Profile button click.
     */
    private void openEditProfile() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new UpdateProfileFragment())
                .addToBackStack(null)
                .commit();
    }


}
