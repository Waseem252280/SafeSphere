//package com.example.safesphere.fragments;
//
//import android.animation.ObjectAnimator;
//import android.animation.PropertyValuesHolder;
//import android.app.AlertDialog;
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.animation.AccelerateDecelerateInterpolator;
//import android.view.animation.Animation;
//import android.view.animation.AnimationUtils;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.widget.AppCompatButton;
//import androidx.cardview.widget.CardView;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.safesphere.R;
//import com.example.safesphere.activity.HomeActivity;
//import com.example.safesphere.adapter.FamilyAdapter;
//import com.example.safesphere.dto.FamilyCircleDto;
//import com.example.safesphere.dto.FamilyMember;
//import com.example.safesphere.dto.UserDto;
//import com.example.safesphere.utils.BadgeUtil;
//import com.example.safesphere.utils.FamilyLiveData;
//import com.example.safesphere.utils.NotificationLiveData;
//import com.google.android.material.floatingactionbutton.FloatingActionButton;
//import com.google.android.material.textfield.TextInputEditText;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.google.firebase.firestore.DocumentSnapshot;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.ListenerRegistration;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.regex.Pattern;
//
//public class HomeFragment extends Fragment {
//    private final Map<String, ValueEventListener> userListeners = new HashMap<>();
//
//
//    private AppCompatButton familyCircleNotifications, familyMembersViewMap;
//    private Animation animateCardIcons;
////    private ImageView chatIcon, videoIcon, locationIcon, groupIcon;
//    private FloatingActionButton addFamilyMemberFab;
//    private TextView familyMembersCount;
//
//    private RecyclerView recyclerView;
//    private FamilyAdapter adapter;
//    private List<FamilyMember> familyMembers;
//    private Bundle bundle = new Bundle();
//
//    private boolean isSharingLocation = false;
//
//    private CardView cardEmergencySection;
//    private View layoutAddContact, layoutSOS;
//    private SharedPreferences sharedPreferences;
//    private Button btnSOS;
//
//    // Fragment ke top par add karein
//    private final Map<String, ListenerRegistration> firestoreListeners = new HashMap<>();
//
//
//    public HomeFragment() {
//        // Required empty public constructor
//    }
//
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.fragment_home, container, false);
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        sharedPreferences = requireContext().getSharedPreferences("SafeSphereData", Context.MODE_PRIVATE);
//
//        initViews(view);
//         // Status check karein
//        checkTrustedContactStatus();
//        setupClickListeners();
//        loadFamilyMembers();
//
//        if(getActivity() instanceof HomeActivity){
//            ((HomeActivity) getActivity()).checkGPSSettings();
//        }
//
//    }
//
//    // 🔹 Initialize Views
//    private void initViews(View view) {
//        familyMembersCount = view.findViewById(R.id.familyMemberCount);
//        familyCircleNotifications = view.findViewById(R.id.familyCircleNotifications);
//
//        animateCardIcons = AnimationUtils.loadAnimation(requireContext(), R.anim.card_icon_anim);
//
//        addFamilyMemberFab = view.findViewById(R.id.addFamilyMemberFab);
//
//        recyclerView = view.findViewById(R.id.familyMembersRecyclerView);
//
//        familyMembersViewMap = view.findViewById(R.id.familyMembersViewMap);
//
//        cardEmergencySection = view.findViewById(R.id.cardEmergencySection);
//
//        cardEmergencySection = view.findViewById(R.id.cardEmergencySection);
//        layoutAddContact = view.findViewById(R.id.layoutAddContact);
//        layoutSOS = view.findViewById(R.id.layoutSOS);
//        btnSOS = view.findViewById(R.id.btnSOS);
//
//        // Pulse animation sirf tab start karein jab contact na ho
//        if (sharedPreferences.getString("trusted_phone", null) == null) {
//            startEmergencyPulse();
//        }
//
//        // 🔹 RecyclerView list aur adapter
//        familyMembers = new ArrayList<>();
//        adapter = new FamilyAdapter(
//                requireContext(),
//                familyMembers,
//                new FamilyAdapter.OnFamilyActionListener() {
//
//                    @Override
//                    public void onMessageClick(FamilyMember member) {
//                        ChatFragment chatFragment = ChatFragment.newInstance(member);
//
//                        requireActivity()
//                                .getSupportFragmentManager()
//                                .beginTransaction()
//                                .replace(R.id.fragment_container, chatFragment)
//                                .addToBackStack(null)
//                                .commit();
//                    }
//
//                    @Override
//                    public void onLocationClick(FamilyMember member) {
//
//                        // 🔥 Multiple users ka map banana
//                        HashMap<String, Boolean> userIds = new HashMap<>();
//
//                        // clicked member
//                        userIds.put(member.getUserId(), true);
//                        // optional: current user bhi add karo
//                        String currentUserId =
//                                FirebaseAuth.getInstance().getCurrentUser().getUid();
//                        userIds.put(currentUserId, true);
//
//                        UserLocationFragment fragment =
//                                UserLocationFragment.newInstance(userIds);
//
//                        requireActivity()
//                                .getSupportFragmentManager()
//                                .beginTransaction()
//                                .replace(R.id.fragment_container, fragment)
//                                .addToBackStack(null)
//                                .commit();
//                    }
//                }
//        );
//
//
//        recyclerView.setAdapter(adapter);
//
//        // 🔹 Layout manager (vertical list)
//        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//
//        // Live family members count
//        FamilyLiveData familyLiveData = new FamilyLiveData();
//        familyLiveData.getFamilyCountLiveData().observe(getViewLifecycleOwner(), count -> {
//            familyMembersCount.setText(String.valueOf(count)+" members connected");
//        });
//
//        // Show notification badge (example count: 3)
//        NotificationLiveData notificationLiveData = new NotificationLiveData(getContext());
//        notificationLiveData.getNotificationsCountLiveData().observe(getViewLifecycleOwner(), count -> {
//            // UI update
//            BadgeUtil.attachBadge(requireContext(), familyCircleNotifications, count);
//        });
//
//        if (getActivity() instanceof HomeActivity) {
//            ((HomeActivity) getActivity()).showNavigation();
//        }
//    }
//
//
//    private void openUserLocationFragment(){
//        // 🔥 Multiple users ka map banana
//        HashMap<String, Boolean> userIds = new HashMap<>();
//
//        // all member
//        for(FamilyMember member: familyMembers) {
//            userIds.put(member.getUserId(), true);
//        }
//
//        // optional: current user bhi add karo
//        String currentUserId =
//                FirebaseAuth.getInstance().getCurrentUser().getUid();
//        userIds.put(currentUserId, true);
//
//        UserLocationFragment fragment =
//                UserLocationFragment.newInstance(userIds);
//
//        requireActivity()
//                .getSupportFragmentManager()
//                .beginTransaction()
//                .replace(R.id.fragment_container, fragment)
//                .addToBackStack(null)
//                .commit();
//    }
//
//    private void startEmergencyPulse() {
//        // 1. Zoom Animation (Scale X and Y)
//        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.05f);
//        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.05f);
//
//        // 2. Transparency Animation (Halka sa fade effect)
//        PropertyValuesHolder alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.8f);
//
//        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(
//                cardEmergencySection, scaleX, scaleY, alpha);
//
//        animator.setDuration(800); // Animation ki speed (0.8 seconds)
//        animator.setRepeatCount(ObjectAnimator.INFINITE); // Chalta rahega jab tak click na ho
//        animator.setRepeatMode(ObjectAnimator.REVERSE); // Zoom in phir wapis Zoom out
//        animator.setInterpolator(new AccelerateDecelerateInterpolator());
//
//        animator.start();
//
//        // Jab user click kare toh animation stop kar do
//        cardEmergencySection.setOnClickListener(v -> {
//            animator.cancel(); // Animation khatam
//            cardEmergencySection.setScaleX(1f); // Reset size
//            cardEmergencySection.setScaleY(1f);
//            cardEmergencySection.setAlpha(1f); // Reset transparency
//
//            // Yahan apna click logic (Dialog open karna) likhen
//            showAddContactDialog();
//        });
//    }
//
//    private void showAddContactDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
//        View view = getLayoutInflater().inflate(R.layout.dialog_add_contact, null);
//        builder.setView(view);
//
//        AlertDialog dialog = builder.create();
//        if (dialog.getWindow() != null) {
//            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
//        }
//
//        TextInputEditText etName = view.findViewById(R.id.etContactName);
//        TextInputEditText etPhone = view.findViewById(R.id.etContactPhone);
//        Button btnSave = view.findViewById(R.id.btnSaveContact);
//
//        btnSave.setOnClickListener(v -> {
//            String name = etName.getText().toString().trim();
//            String phone = etPhone.getText().toString().trim();
//
//            if (validatePakistaniPhone(phone)) {
//                if (!name.isEmpty()) {
//                    saveTrustedContact(name, phone, dialog);
//                } else {
//                    etName.setError("Enter Name");
//                }
//            } else {
//                etPhone.setError("Invalid Pakistani Format (e.g. 03xxxxxxxxx)");
//            }
//        });
//
//        dialog.show();
//    }
//
//    private boolean validatePakistaniPhone(String phone) {
//        // Regex for: 03xxxxxxxxx or +923xxxxxxxxx or 923xxxxxxxxx
//        String pattern = "^(03|\\+923|923)\\d{9}$";
//        return Pattern.compile(pattern).matcher(phone).matches();
//    }
//
//    private void saveTrustedContact(String name, String phone, AlertDialog dialog) {
//        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//
//        Map<String, Object> contactData = new HashMap<>();
//        contactData.put("trustedName", name);
//        contactData.put("trustedPhone", phone);
//
//        // 1. Save to Firestore
//        FirebaseFirestore.getInstance().collection("users")
//                .document(currentUserId)
//                .update(contactData)
//                .addOnSuccessListener(aVoid -> {
//                    // 2. Save to SharedPreferences for Offline usage
//                    SharedPreferences.Editor editor = sharedPreferences.edit();
//                    editor.putString("trusted_name", name);
//                    editor.putString("trusted_phone", phone);
//                    editor.apply();
//
//                    // 3. UI Update
//                    checkTrustedContactStatus();
//                    dialog.dismiss();
//                    Toast.makeText(getContext(), "Trusted Contact Added!", Toast.LENGTH_SHORT).show();
//                })
//                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show());
//    }
//
//    private void checkTrustedContactStatus() {
//        String savedPhone = sharedPreferences.getString("trusted_phone", null);
//        if (savedPhone != null) {
//            layoutAddContact.setVisibility(View.GONE);
//            layoutSOS.setVisibility(View.VISIBLE);
//        } else {
//            startEmergencyPulse();
//            layoutAddContact.setVisibility(View.VISIBLE);
//            layoutSOS.setVisibility(View.GONE);
//        }
//    }
//
//    private void sendEmergencyAlert() {
//        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//
//        // 1. Update DB (Safe = false)
//        FirebaseFirestore.getInstance().collection("users")
//                .document(currentUserId)
//                .update("isSafe", false);
//
//        // 2. Logic for sending Location SMS (Offline Support)
//        String phone = sharedPreferences.getString("trusted_phone", "");
//        String name = sharedPreferences.getString("trusted_name", "");
//
//        if (!phone.isEmpty()) {
//            Toast.makeText(getContext(), "Sending SOS Alert to " + name, Toast.LENGTH_LONG).show();
//            // Yahan aap SMS Manager ya Location API use kar ke message bhej sakte hain
//        }
//    }
//
//    private void listenUserBatteryUpdates(FamilyMember member) {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        String batKey = "battery_" + member.getUserId();
//
//        if (firestoreListeners.containsKey(batKey)) {
//            firestoreListeners.get(batKey).remove();
//        }
//
//        ListenerRegistration registration = db.collection("users")
//                .document(member.getUserId())
//                .addSnapshotListener((snapshot, e) -> {
//                    if (e != null || snapshot == null || !snapshot.exists()) return;
//
//                    Long batteryLong = snapshot.getLong("batteryPercentage");
//                    if (batteryLong != null) {
//                        int newBatteryValue = batteryLong.intValue();
//
//                        // Sirf tab update karein agar value purani value se mukhtalif ho
//                        if (newBatteryValue != member.getBattery()) {
//                            member.setBattery(newBatteryValue);
//
//                            int index = familyMembers.indexOf(member);
//                            if (index != -1) {
//                                // PAYLOAD BHEJEIN: Isse animation/blink nahi hoga
//                                Bundle payload = new Bundle();
//                                payload.putInt("battery", newBatteryValue);
//                                adapter.notifyItemChanged(index, payload);
//                            }
//                        }
//                    }
//                });
//
//        firestoreListeners.put(batKey, registration);
//    }
//
//
//    private void listenUserLocationName(FamilyMember member) {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//        String locKey = "location_" + member.getUserId();
//
//        if (firestoreListeners.containsKey(locKey)) {
//            firestoreListeners.get(locKey).remove();
//        }
//
//        // Corrected the type here to ListenerRegistration
//        ListenerRegistration registration = db.collection("users")
//                .document(member.getUserId())
//                .addSnapshotListener((snapshot, e) -> {
//                    if (e != null || snapshot == null || !snapshot.exists()) return;
//
//                    String locationValue = snapshot.getString("locationName");
//
//                    // Only update if the value actually changed to save resources
//                    if (locationValue != null && !locationValue.equals(member.getLocation())) {
//                        member.setLocation(locationValue);
//
//                        int index = familyMembers.indexOf(member);
//                        if (index != -1) {
//                            Bundle payload = new Bundle();
//                            payload.putString("location", locationValue);
//                            adapter.notifyItemChanged(index, payload);
//                        }
//                    }
//                });
//
//        firestoreListeners.put(locKey, registration);
//    }
//
//    // 🔹 Start Card Animations
////    private void startAnimation() {
////        chatIcon.startAnimation(animateCardIcons);
////        videoIcon.startAnimation(animateCardIcons);
////        groupIcon.startAnimation(animateCardIcons);
////        locationIcon.startAnimation(animateCardIcons);
////    }
//
//    // 🔹 Click Listeners
//    private void setupClickListeners() {
//        familyCircleNotifications.setOnClickListener(v -> {
//            BadgeUtil.clearBadge(familyCircleNotifications);
//            FragmentManager manager = requireActivity().getSupportFragmentManager();
//            manager.beginTransaction()
//                    .replace(R.id.fragment_container, new NotificationsFragment())
//                    .addToBackStack(null).commit();
//        });
//
//        familyMembersViewMap.setOnClickListener(v ->{
//            openUserLocationFragment();
//        });
//
//        addFamilyMemberFab.setOnClickListener(v -> {
//                    FragmentManager manager = requireActivity().getSupportFragmentManager();
//                    manager.beginTransaction()
//                            .replace(R.id.fragment_container, new AddFamilyMembersFragment())
//                            .addToBackStack(null).commit();
//                });
//
//        btnSOS.setOnClickListener(v -> sendEmergencyAlert());
//
//
////        chatIcon.setOnClickListener(v -> showToast("Group Chat clicked"));
////        videoIcon.setOnClickListener(v -> showToast("Group Video clicked"));
////        locationIcon.setOnClickListener(v -> showToast("Location sharing enabled"));
////        groupIcon.setOnClickListener(v -> showToast("Family Group clicked"));
//    }
//
//    private void showToast(String message) {
//        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
//    }
//
//    private void loadFamilyMembers() {
//        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//        db.collection("users")
//                .document(currentUserId)
//                .collection("family_circle")
//                .get()
//                .addOnSuccessListener(querySnapshot -> {
//                    if (!querySnapshot.isEmpty()) {
//                        List<String> memberIds = new ArrayList<>();
//                        Map<String, String> relationMap = new HashMap<>();
//
//                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
//                            FamilyCircleDto circleDto = doc.toObject(FamilyCircleDto.class);
//                            if (circleDto != null) {
//                                memberIds.add(circleDto.getFamilyMemberUserId());
//                                relationMap.put(circleDto.getFamilyMemberUserId(), circleDto.getRelation());
//                            }
//                        }
//
//                        fetchUsersInBatches(memberIds, relationMap);
//                    }
//                })
//                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load family members", Toast.LENGTH_SHORT).show());
//    }
//
//    private void fetchUsersInBatches(List<String> memberIds, Map<String, String> relationMap) {
//        FirebaseFirestore db = FirebaseFirestore.getInstance();
//
//        List<FamilyMember> tempList = new ArrayList<>();
//
//        int batchSize = 10;
//        for (int i = 0; i < memberIds.size(); i += batchSize) {
//            List<String> batch = memberIds.subList(i, Math.min(i + batchSize, memberIds.size()));
//
//            db.collection("users")
//                    .whereIn("userId", batch)
//                    .get()
//                    .addOnSuccessListener(userSnapshots -> {
//                        for (DocumentSnapshot userDoc : userSnapshots.getDocuments()) {
//                            UserDto user = userDoc.toObject(UserDto.class);
//                            if (user != null) {
//                                FamilyMember member = new FamilyMember();
//                                member.setName(user.getFullName());
//                                member.setProfileImageUrl(user.getPhotoUrl());
//                                member.setStatus("offline");// default
//                                member.setBattery(user.getBatteryPercentage());
//                                member.setLocation(user.getLocationName());
//                                member.setRelation(relationMap.get(user.getUserId()));
//                                member.setUserId(user.getUserId());
//                                tempList.add(member);
//                            }
//                        }
//                        // Update adapter once per batch
//                        familyMembers.clear();
//                        familyMembers.addAll(tempList);
//                        adapter.notifyDataSetChanged();
//
//                        for (FamilyMember member : familyMembers) {
//                            listenUserRealtimeUpdates(member);
//                            listenUserBatteryUpdates(member);
//                            listenUserLocationName(member);
//                        }
//
//                    });
//        }
//    }
//
//
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//
//        for (Map.Entry<String, ValueEventListener> entry : userListeners.entrySet()) {
//            FirebaseDatabase.getInstance()
//                    .getReference("status")   // ✅ sahi
//                    .child(entry.getKey())
//                    .removeEventListener(entry.getValue());
//        }
//
//        userListeners.clear();
//
//        // Stop Firestore listeners (NEW)
//        for (ListenerRegistration registration : firestoreListeners.values()) {
//            if (registration != null) {
//                registration.remove();
//            }
//        }
//        firestoreListeners.clear();
//    }
//
//    private void listenUserRealtimeUpdates(FamilyMember member) {
//
//        DatabaseReference statusRef = FirebaseDatabase.getInstance()
//                .getReference("status")
//                .child(member.getUserId());
//
//        ValueEventListener listener = new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//
//                if (!snapshot.exists()) {
//                    member.setStatus("offline");
//                    return;
//                }
//
//                Boolean online = snapshot.child("online").getValue(Boolean.class);
//
//                member.setStatus(
//                        online != null && online ? "online" : "offline"
//                );
//
//                int index = familyMembers.indexOf(member);
//                if (index != -1) {
//                    adapter.notifyItemChanged(index);
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) { }
//        };
//
//        statusRef.addValueEventListener(listener);
//        userListeners.put(member.getUserId(), listener);
//    }
//}





package com.example.safesphere.fragments;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.safesphere.R;
import com.example.safesphere.activity.HomeActivity;
import com.example.safesphere.adapter.FamilyAdapter;
import com.example.safesphere.dto.FamilyCircleDto;
import com.example.safesphere.dto.FamilyMember;
import com.example.safesphere.dto.TrustedContact;
import com.example.safesphere.dto.UserDto;
import com.example.safesphere.utils.BadgeUtil;
import com.example.safesphere.utils.FamilyLiveData;
import com.example.safesphere.utils.NotificationLiveData;
import com.example.safesphere.utils.SharedPrefferanceUtil;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle; // Agar aap onCreate use kar rahe hain
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// Aapki Helper Class ka import
import com.example.safesphere.utils.SOSHelper;

public class HomeFragment extends Fragment {

    private final Map<String, ValueEventListener> statusListeners = new HashMap<>();
    private final Map<String, ListenerRegistration> firestoreListeners = new HashMap<>();

    private List<FamilyMember> familyMembers = new ArrayList<>();
    private List<FamilyMember> displayList = new ArrayList<>();
    private FamilyAdapter adapter;

    private TabLayout statusTabLayout;
    private String currentTab = "All";
    private TextView familyMembersCount;
    private AppCompatButton familyCircleNotifications, btnSettings, familyMembersViewMap;
    private CardView cardEmergencySection;
    private View layoutAddContact, layoutSOS;
    private SharedPreferences sharedPreferences;
    private Button btnSOS;

    private FloatingActionButton addFamilyMemberFab;

    private LinearLayout emptyMemberLayout;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        sharedPreferences = requireContext().getSharedPreferences("SafeSphereData", Context.MODE_PRIVATE);
        initViews(view);
        checkTrustedContactStatus();
        setupClickListeners();
        loadFamilyMembers();

        if(getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).checkGPSSettings();
            ((HomeActivity) getActivity()).showNavigation();
        }
    }


//    private void checkPermissionsAndSndSOS() {
//        String[] permissions = {
//                Manifest.permission.SEND_SMS,
//                Manifest.permission.ACCESS_FINE_LOCATION
//        };
//
//        boolean allGranted = true;
//        for (String s : permissions) {
//            if (ContextCompat.checkSelfPermission(requireContext(), s) != PackageManager.PERMISSION_GRANTED) {
//                allGranted = false;
//                break;
//            }
//        }
//
//        if (allGranted) {
//            // Agar sari permissions hain to SOS process start karo
//            SOSHelper.startSOSProcess(requireContext());
//        } else {
//            // Permissions maango
//            ActivityCompat.requestPermissions(requireActivity(), permissions, 100);
//        }
//    }
//
//    // User jab allow/deny karega to ye method chalega
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == 100) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                SOSHelper.startSOSProcess(requireContext());
//            } else {
//                Toast.makeText(requireContext(), "Permissions are required for SOS!", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }


    private void checkPermissionsAndSndSOS() {
        String[] permissions = {
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        boolean allGranted = true;
        for (String s : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), s) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            SOSHelper.startSOSProcess(requireContext());
        } else {
            requestPermissions(permissions, 100); // Fragment mein requestPermissions direct call karein
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            boolean isAllGranted = true;
            for (int res : grantResults) {
                if (res != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }

            if (isAllGranted) {
                SOSHelper.startSOSProcess(requireContext());
            } else {
                Toast.makeText(requireContext(), "SOS feature ke liye SMS aur Location zaroori hai!", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void initViews(View view) {
        familyMembersCount = view.findViewById(R.id.familyMemberCount);
        familyCircleNotifications = view.findViewById(R.id.familyCircleNotifications);
        btnSettings = view.findViewById(R.id.btnSettings);
        familyMembersViewMap = view.findViewById(R.id.familyMembersViewMap);
        cardEmergencySection = view.findViewById(R.id.cardEmergencySection);
        layoutAddContact = view.findViewById(R.id.layoutAddContact);
        layoutSOS = view.findViewById(R.id.layoutSOS);
        btnSOS = view.findViewById(R.id.btnSOS);
        statusTabLayout = view.findViewById(R.id.statusTabLayout);
        addFamilyMemberFab = view.findViewById(R.id.addFamilyMemberFab);
        RecyclerView recyclerView = view.findViewById(R.id.familyMembersRecyclerView);
        emptyMemberLayout = view.findViewById(R.id.emptyMembersLayout);

        adapter = new FamilyAdapter(requireContext(), displayList, new FamilyAdapter.OnFamilyActionListener() {
            @Override
            public void onMessageClick(FamilyMember member) {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, ChatFragment.newInstance(member))
                        .addToBackStack(null).commit();
            }

            @Override
            public void onLocationClick(FamilyMember member) {
                HashMap<String, Boolean> userIds = new HashMap<>();
                userIds.put(member.getUserId(), true);
                userIds.put(FirebaseAuth.getInstance().getUid(), true);
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, UserLocationFragment.newInstance(userIds))
                        .addToBackStack(null).commit();
            }
        });

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Show notification badge (example count: 3)
        NotificationLiveData notificationLiveData = new NotificationLiveData(getContext());
        notificationLiveData.getNotificationsCountLiveData().observe(getViewLifecycleOwner(), count -> {
            // UI update
            BadgeUtil.attachBadge(requireContext(), familyCircleNotifications, count);
        });

        statusTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getText().toString().split(" ")[0];
                refreshDisplayList();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void loadFamilyMembers() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        // 1. Get member IDs from current user's collection in FIRESTORE
        FirebaseFirestore.getInstance().collection("users").document(uid)
                .collection("family_circle").get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        emptyMemberLayout.setVisibility(View.GONE);
                        List<String> ids = new ArrayList<>();
                        Map<String, String> relations = new HashMap<>();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            FamilyCircleDto dto = doc.toObject(FamilyCircleDto.class);
                            if (dto != null) {
                                ids.add(dto.getFamilyMemberUserId());
                                relations.put(dto.getFamilyMemberUserId(), dto.getRelation());
                            }
                        }
                        fetchUsersFromFirestore(ids, relations);
                    }else {
                        emptyMemberLayout.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void fetchUsersFromFirestore(List<String> ids, Map<String, String> relations) {
        // 2. Fetch full user details from FIRESTORE
        FirebaseFirestore.getInstance().collection("users").whereIn("userId", ids).get()
                .addOnSuccessListener(snapshots -> {
                    familyMembers.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        UserDto user = doc.toObject(UserDto.class);
                        if (user != null) {
                            FamilyMember member = new FamilyMember();
                            member.setUserId(user.getUserId());
                            member.setName(user.getFullName());
                            member.setProfileImageUrl(user.getPhotoUrl());
                            member.setBattery(user.getBatteryPercentage());
                            member.setLocation(user.getLocationName());
                            member.setRelation(relations.get(user.getUserId()));
                            member.setSafe(user.getIsSafe());
                            member.setLatitude(user.getLatitude());
                            member.setLongitude(user.getLongitude());
                            member.setStatus("offline");

                            familyMembers.add(member);
                            listenToRealtimeData(member);
                        }
                    }
                    refreshDisplayList();
                });
    }

    private void listenToRealtimeData(FamilyMember member) {
        // A. Realtime Database (ONLY for Online Presence)
        DatabaseReference statusRef = FirebaseDatabase.getInstance().getReference("status").child(member.getUserId());
        ValueEventListener vListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean online = snapshot.child("online").getValue(Boolean.class);
                member.setStatus(online != null && online ? "online" : "offline");
                refreshDisplayList();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        statusRef.addValueEventListener(vListener);
        statusListeners.put(member.getUserId(), vListener);

        // B. Firestore Snapshots (For EVERYTHING ELSE: Location, Battery, Safe status)
        ListenerRegistration fListener = FirebaseFirestore.getInstance().collection("users").document(member.getUserId())
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot == null || !snapshot.exists()) return;

                    boolean tabRefresh = false;

                    // Update local member object
                    Long bat = snapshot.getLong("batteryPercentage");
                    if (bat != null && bat.intValue() != member.getBattery()) {
                        member.setBattery(bat.intValue());
                        triggerUpdate(member, "battery", bat.intValue());
                    }

                    String loc = snapshot.getString("locationName");
                    if (loc != null && !loc.equals(member.getLocation())) {
                        member.setLocation(loc);
                        triggerUpdate(member, "location", loc);
                    }

                    Boolean safe = snapshot.getBoolean("isSafe");
                    if (safe != null && safe != member.isSafe()) {
                        member.setSafe(safe);
                        tabRefresh = true;
                    }

                    Double lat = snapshot.getDouble("latitude");
                    Double lng = snapshot.getDouble("longitude");
                    if (lat != null && lng != null && (lat != member.getLatitude() || lng != member.getLongitude())) {
                        member.setLatitude(lat);
                        member.setLongitude(lng);
                        tabRefresh = true;
                    }

                    if (tabRefresh) refreshDisplayList();
                });
        firestoreListeners.put(member.getUserId(), fListener);
    }

    private void triggerUpdate(FamilyMember member, String type, Object val) {
        int index = displayList.indexOf(member);
        if (index != -1) {
            Bundle p = new Bundle();
            if (type.equals("battery")) p.putInt("battery", (int) val);
            if (type.equals("location")) p.putString("location", (String) val);
            adapter.notifyItemChanged(index, p);
        }
    }

    private void refreshDisplayList() {
        if (!isAdded()) return;
        displayList.clear();
        int on = 0, aw = 0, un = 0;

        double myLat = Double.parseDouble(sharedPreferences.getString("last_lat", "0.0"));
        double myLng = Double.parseDouble(sharedPreferences.getString("last_lng", "0.0"));
        Location myLoc = new Location(""); myLoc.setLatitude(myLat); myLoc.setLongitude(myLng);

        for (FamilyMember m : familyMembers) {
            if ("online".equals(m.getStatus())) on++;
            if (!m.isSafe()) un++;

            Location mLoc = new Location(""); mLoc.setLatitude(m.getLatitude()); mLoc.setLongitude(m.getLongitude());
            float dist = (myLat == 0 || m.getLatitude() == 0) ? 0 : myLoc.distanceTo(mLoc);
            if (dist > 500) aw++;

            if (currentTab.equals("All")) displayList.add(m);
            else if (currentTab.equals("Online") && "online".equals(m.getStatus())) displayList.add(m);
            else if (currentTab.equals("Away") && dist > 500) displayList.add(m);
            else if (currentTab.equals("Unsafe") && !m.isSafe()) displayList.add(m);
            else if (currentTab.equals("Safe") && m.isSafe()) displayList.add(m);
        }
        updateTabUI(on, aw, un);
        adapter.notifyDataSetChanged();
    }

    private void updateTabUI(int on, int aw, int un) {
        if (statusTabLayout == null) return;
        statusTabLayout.getTabAt(0).setText("All (" + familyMembers.size() + ")");
        statusTabLayout.getTabAt(1).setText("Online (" + on + ")");
        statusTabLayout.getTabAt(2).setText("Away (" + aw + ")");

        TabLayout.Tab sTab = statusTabLayout.getTabAt(3);
        if (un > 0) {
            sTab.setText("Unsafe (" + un + ")");
            sTab.setIcon(R.drawable.ic_unsafe);
            familyMembersCount.setText("Unsafe members detected!");
            familyMembersCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.red));
        } else {
            sTab.setText("All Safe");
            sTab.setIcon(R.drawable.ic_safe);
            familyMembersCount.setText(familyMembers.size() + " members connected");
            familyMembersCount.setTextColor(ContextCompat.getColor(requireContext(), R.color.green));
        }
    }

    private void setupClickListeners() {
                familyCircleNotifications.setOnClickListener(v -> {
            BadgeUtil.clearBadge(familyCircleNotifications);
            FragmentManager manager = requireActivity().getSupportFragmentManager();
            manager.beginTransaction()
                    .replace(R.id.fragment_container, new NotificationsFragment())
                    .addToBackStack(null).commit();
        });

                btnSettings.setOnClickListener(v -> openSettings());

        familyMembersViewMap.setOnClickListener(v ->{
            openUserLocationFragment();
        });

        addFamilyMemberFab.setOnClickListener(v -> {
                    FragmentManager manager = requireActivity().getSupportFragmentManager();
                    manager.beginTransaction()
                            .replace(R.id.fragment_container, new AddFamilyMembersFragment())
                            .addToBackStack(null).commit();
                });

        btnSOS.setOnClickListener(v -> checkPermissionsAndSndSOS());
    }

//    private void sendEmergencyAlert() {
//        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//
//        // 1. Update DB (Safe = false)
//        FirebaseFirestore.getInstance().collection("users")
//                .document(currentUserId)
//                .update("isSafe", false);
//
//        // 2. Logic for sending Location SMS (Offline Support)
//        String phone = sharedPreferences.getString("trusted_phone", "");
//        String name = sharedPreferences.getString("trusted_name", "");
//
//        if (!phone.isEmpty()) {
//            Toast.makeText(getContext(), "Sending SOS Alert to " + name, Toast.LENGTH_LONG).show();
//            // Yahan aap SMS Manager ya Location API use kar ke message bhej sakte hain
//        }
//    }

    private void openUserLocationFragment(){
        // 🔥 Multiple users ka map banana
        HashMap<String, Boolean> userIds = new HashMap<>();

        // all member
        for(FamilyMember member: familyMembers) {
            userIds.put(member.getUserId(), true);
        }

        // optional: current user bhi add karo
        String currentUserId =
                FirebaseAuth.getInstance().getCurrentUser().getUid();
        userIds.put(currentUserId, true);

        UserLocationFragment fragment =
                UserLocationFragment.newInstance(userIds);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

//    private void checkTrustedContactStatus() {
//        if (sharedPreferences.getString("trusted_phone", null) != null) {
//            cardEmergencySection.setVisibility(View.GONE);
//            layoutSOS.setVisibility(View.VISIBLE);
//        } else {
//            cardEmergencySection.setVisibility(View.VISIBLE);
//            layoutSOS.setVisibility(View.GONE);
//            startEmergencyPulse();
//        }
//    }

    private void checkTrustedContactStatus() {
        // 1. SharedPrefferanceUtil se list mangwao
        List<TrustedContact> contacts = SharedPrefferanceUtil.getTrustedContacts(requireContext());

        // 2. Check karo ke list null to nahi ya khali to nahi
        if (contacts != null && !contacts.isEmpty()) {
            // Agar contacts hain: SOS dikhao, Emergency setup chhupao
            cardEmergencySection.setVisibility(View.GONE);
            layoutSOS.setVisibility(View.VISIBLE);
        } else {
            // Agar koi contact nahi hai: Emergency setup dikhao aur pulse start karo
            cardEmergencySection.setVisibility(View.VISIBLE);
            layoutSOS.setVisibility(View.GONE);
            startEmergencyPulse();
        }
    }

    private void startEmergencyPulse() {
        PropertyValuesHolder sx = PropertyValuesHolder.ofFloat(View.SCALE_X, 1.05f);
        PropertyValuesHolder sy = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.05f);
        ObjectAnimator anim = ObjectAnimator.ofPropertyValuesHolder(cardEmergencySection, sx, sy);
        anim.setDuration(800); anim.setRepeatCount(ObjectAnimator.INFINITE);
        anim.setRepeatMode(ObjectAnimator.REVERSE); anim.start();
        cardEmergencySection.setOnClickListener(v -> { anim.cancel(); openAddContact(); });
    }

    private void openAddContact() {
        // Hum null pass kar rahe hain kyunke hum Naya contact add kar rahe hain, update nahi.
        AddContactFragment addFragment = AddContactFragment.newInstance(null);

        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out,
                        android.R.anim.slide_in_left, android.R.anim.slide_out_right) // Smooth Transition
                .replace(R.id.fragment_container, addFragment) // R.id.fragment_container aapki main activity ka container hona chahiye
                .addToBackStack(null) // Back button dabane par wapas settings pe aane ke liye
                .commit();
    }

    // Ek function bana lein navigate karne ke liye
    private void openSettings() {
        SettingsFragment settingsFragment = new SettingsFragment();

        requireActivity().getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, settingsFragment) // R.id.fragment_container wahi dabba hai jahan fragments load hote hain
                .addToBackStack(null) // Takay back dabane par pichli screen aaye
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (ValueEventListener l : statusListeners.values()) FirebaseDatabase.getInstance().getReference("status").removeEventListener(l);
        for (ListenerRegistration r : firestoreListeners.values()) if (r != null) r.remove();
        statusListeners.clear();
        firestoreListeners.clear();
    }
}