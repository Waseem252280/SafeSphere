package com.example.safesphere.fragments;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.safesphere.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.Map;

public class UserLocationFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore firestore;
    private HashMap<String, Boolean> userIds;

    private final Map<String, Marker> userMarkers = new HashMap<>();
    private final Map<String, ListenerRegistration> firestoreListeners = new HashMap<>();
    private final Map<String, ValueEventListener> statusListeners = new HashMap<>();
    private final Map<String, DatabaseReference> statusRefs = new HashMap<>();
    private final Map<String, UserDto> lastKnownData = new HashMap<>();

    private View markerView;
    private TextView txtName, txtBattery, txtCity;
    private ImageView imgProfile, ivStatusDot, ivBattery;

    private android.widget.EditText searchEditText;

    // Variables add karein
    private View searchCard;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabSearch;
    private ImageView ivCloseSearch;

    public static UserLocationFragment newInstance(HashMap<String, Boolean> userIds) {
        UserLocationFragment fragment = new UserLocationFragment();
        Bundle b = new Bundle();
        b.putSerializable("userIds", userIds);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            userIds = (HashMap<String, Boolean>) getArguments().getSerializable("userIds");
        }
    }

//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View v = inflater.inflate(R.layout.fragment_user_location, container, false);
//        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
//        if (mapFragment != null) mapFragment.getMapAsync(this);
//        initMarkerView();
//
//        // Search Logic
//        searchEditText = v.findViewById(R.id.searchEditText);
//        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                filterMarkers(s.toString());
//            }
//
//            @Override
//            public void afterTextChanged(android.text.Editable s) {}
//        });
//        return v;
//    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_user_location, container, false);

        // Bind Views
        searchCard = v.findViewById(R.id.searchCard);
        fabSearch = v.findViewById(R.id.fabSearch);
        ivCloseSearch = v.findViewById(R.id.ivCloseSearch);
        searchEditText = v.findViewById(R.id.searchEditText);

        // 1. Search Icon Click -> Show Bar, Hide Icon
        fabSearch.setOnClickListener(view -> {
            searchCard.setVisibility(View.VISIBLE);
            fabSearch.setVisibility(View.GONE);
            searchEditText.requestFocus();
            // Keyboard open karne ke liye (Optional)
        });

        // 2. Close Icon Click -> Hide Bar, Show Icon, Clear Text
        ivCloseSearch.setOnClickListener(view -> {
            // 1. Clear text and reset markers
            searchEditText.setText("");

            // 2. Hide Search Bar and Show FAB
            searchCard.setVisibility(View.GONE);
            fabSearch.setVisibility(View.VISIBLE);

            // 3. 🔥 Keyboard Hide karne ka logic
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
            }

            // Taaki focus edit text se hat jaye
            searchEditText.clearFocus();
        });

        // Existing Map & TextWatcher Logic
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
        initMarkerView();

        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMarkers(s.toString());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        return v;
    }

//    private void filterMarkers(String query) {
//        String lowerCaseQuery = query.toLowerCase().trim();
//
//        // Agar search empty hai to sab markers dikhao aur camera reset karo
//        if (lowerCaseQuery.isEmpty()) {
//            for (Marker marker : userMarkers.values()) {
//                marker.setVisible(true);
//            }
//            updateCameraFocus(); // Sab ko wapas screen mein fit karo
//            return;
//        }
//
//        boolean foundFirstMatch = false;
//
//        for (Map.Entry<String, Marker> entry : userMarkers.entrySet()) {
//            String uid = entry.getKey();
//            Marker marker = entry.getValue();
//            UserDto userData = lastKnownData.get(uid);
//
//            if (userData != null && userData.fullName != null) {
//                boolean isMatch = userData.fullName.toLowerCase().contains(lowerCaseQuery);
//
//                // Marker visibility update karein (Sirf match hone wala dikhega)
//                marker.setVisible(isMatch);
//
//                // Sirf pehle match hone wale user par camera le jayein
//                if (isMatch && !foundFirstMatch) {
//                    if (userData.latitude != null && userData.longitude != null) {
//                        LatLng userLocation = new LatLng(userData.latitude, userData.longitude);
//                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17f));
//                        marker.showInfoWindow();
//                        foundFirstMatch = true;
//                    }
//                }
//            }
//        }
//    }
private void filterMarkers(String query) {
    String lowerCaseQuery = query.toLowerCase().trim();

    if (lowerCaseQuery.isEmpty()) {
        for (Marker marker : userMarkers.values()) {
            marker.setVisible(true);
        }
        updateCameraFocus();
        return;
    }

    boolean foundAnyMatch = false;
    boolean foundFirstMatch = false;

    for (Map.Entry<String, Marker> entry : userMarkers.entrySet()) {
        String uid = entry.getKey();
        Marker marker = entry.getValue();
        UserDto userData = lastKnownData.get(uid);

        if (userData != null && userData.fullName != null) {
            boolean isMatch = userData.fullName.toLowerCase().contains(lowerCaseQuery);
            marker.setVisible(isMatch);

            if (isMatch) {
                foundAnyMatch = true;
                if (!foundFirstMatch) {
                    if (userData.latitude != null && userData.longitude != null) {
                        LatLng userLocation = new LatLng(userData.latitude, userData.longitude);
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17f));
                        marker.showInfoWindow();
                        foundFirstMatch = true;
                    }
                }
            }
        }
    }

    if (!foundAnyMatch) {
        // Correct Syntax: Toast.LENGTH_SHORT
        android.widget.Toast.makeText(requireContext(),
                "No member found: " + query,
                android.widget.Toast.LENGTH_SHORT).show();
    }
}
    private void initMarkerView() {
        markerView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_marker_layout, null);
        txtName = markerView.findViewById(R.id.txtName);
        txtBattery = markerView.findViewById(R.id.txtBattery);
        txtCity = markerView.findViewById(R.id.txtCity);
        imgProfile = markerView.findViewById(R.id.imgProfile);
        ivStatusDot = markerView.findViewById(R.id.ivStatusDot);
        ivBattery = markerView.findViewById(R.id.ivBattery);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (userIds != null) {
            startTrackingUsers();
        }
    }

    private void startTrackingUsers() {
        for (String uid : userIds.keySet()) {
            setupUserFirestoreListener(uid);
            setupUserStatusListener(uid);
        }
    }

    private void setupUserFirestoreListener(String uid) {
        ListenerRegistration reg = firestore.collection("users").document(uid)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    UserDto user = snapshot.toObject(UserDto.class);
                    if (user != null) {
                        user.userId = uid;
                        // Pehle se mojood status ko maintain karein agar hai
                        if (lastKnownData.containsKey(uid)) {
                            user.isOnline = lastKnownData.get(uid).isOnline;
                        }
                        handleUserUpdate(user);
                    }
                });
        firestoreListeners.put(uid, reg);
    }

    private void setupUserStatusListener(String uid) {
        // Correct path as per your requirement: status -> userId -> online
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("status").child(uid).child("online");
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean online = snapshot.getValue(Boolean.class);
                boolean isOnline = (online != null && online);

                UserDto currentData = lastKnownData.get(uid);

                // FIXED: Agar data null hai tab bhi online status save karein
                if (currentData == null) {
                    currentData = new UserDto();
                    currentData.userId = uid;
                    currentData.isOnline = isOnline;
                    lastKnownData.put(uid, currentData);
                } else if (currentData.isOnline != isOnline) {
                    currentData.isOnline = isOnline;
                    // FIXED: UI tabhi refresh karein jab marker map par mojood ho
                    if (userMarkers.containsKey(uid)) {
                        refreshMarkerIcon(currentData);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        ref.addValueEventListener(listener);
        statusRefs.put(uid, ref);
        statusListeners.put(uid, listener);
    }

    private void handleUserUpdate(UserDto newUser) {

        // 🔥 FIX: Null check for Latitude and Longitude to prevent crash
        if (newUser.latitude == null || newUser.longitude == null) {
            // Agar location null hai to marker update nahi karenge
            return;
        }

        UserDto oldUser = lastKnownData.get(newUser.userId);

        LatLng newPos = new LatLng(newUser.latitude, newUser.longitude);
        Marker marker = userMarkers.get(newUser.userId);

        if (marker == null) {
            lastKnownData.put(newUser.userId, newUser);
            createNewMarker(newUser, newPos);
        } else {
            marker.setPosition(newPos);
            if (shouldRefreshIcon(oldUser, newUser)) {
                lastKnownData.put(newUser.userId, newUser);
                refreshMarkerIcon(newUser);
            }
        }
        updateCameraFocus();
    }

    private boolean shouldRefreshIcon(UserDto old, UserDto newUser) {
        if (old == null) return true;
        // FIXED: added isOnline check here too
        return !old.fullName.equals(newUser.fullName) ||
                !old.locationName.equals(newUser.locationName) ||
                !old.batteryPercentage.equals(newUser.batteryPercentage) ||
                !old.photoUrl.equals(newUser.photoUrl) ||
                old.isOnline != newUser.isOnline;
    }

    private void createNewMarker(UserDto user, LatLng pos) {
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(pos)
                .anchor(0.5f, 1.0f));
        userMarkers.put(user.userId, marker);
        refreshMarkerIcon(user);
    }

    private void refreshMarkerIcon(UserDto user) {
        Picasso.get().load(user.photoUrl)
                .placeholder(R.drawable.profile)
                .error(R.drawable.profile)
                .into(imgProfile, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        updateBitmapAndSetIcon(user);
                    }
                    @Override
                    public void onError(Exception e) {
                        updateBitmapAndSetIcon(user);
                    }
                });
    }

    private void updateBitmapAndSetIcon(UserDto user) {
        txtName.setText(user.fullName);
        txtCity.setText(user.locationName != null ? user.locationName : "Unknown");
        txtBattery.setText(user.batteryPercentage + "%");

        // UI UPDATE: Online Dot
        ivStatusDot.setVisibility(user.isOnline ? View.VISIBLE : View.GONE);

        // UI UPDATE: Battery Tint
        if (user.batteryPercentage <= 15) {
            ivBattery.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red));
        } else {
            ivBattery.setColorFilter(ContextCompat.getColor(requireContext(), R.color.green));
        }

        markerView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        markerView.layout(0, 0, markerView.getMeasuredWidth(), markerView.getMeasuredHeight());

        Bitmap bitmap = Bitmap.createBitmap(markerView.getMeasuredWidth(), markerView.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        markerView.draw(canvas);

        Marker marker = userMarkers.get(user.userId);
        if (marker != null) {
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
        }
    }

    private void updateCameraFocus() {
        if (userMarkers.isEmpty()) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (Marker m : userMarkers.values()) {
            builder.include(m.getPosition());
        }

        if (userMarkers.size() == 1) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    userMarkers.values().iterator().next().getPosition(), 16f));
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 200));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (ListenerRegistration lr : firestoreListeners.values()) if (lr != null) lr.remove();
        for (String uid : statusRefs.keySet()) {
            if (statusRefs.get(uid) != null && statusListeners.get(uid) != null) {
                statusRefs.get(uid).removeEventListener(statusListeners.get(uid));
            }
        }
    }

    public static class UserDto {
        public String userId;
        public String fullName = "";
        public String locationName = "";
        public Double latitude = 0.0;
        public Double longitude = 0.0;
        public String photoUrl = "";
        public Long batteryPercentage = 0L;
        public boolean isOnline = false;
        public UserDto() {}
    }
}