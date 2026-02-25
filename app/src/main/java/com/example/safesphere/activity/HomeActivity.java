
package com.example.safesphere.activity;

import static android.widget.Toast.makeText;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.safesphere.R;
import com.example.safesphere.auth.AuthActivity;
import com.example.safesphere.fragments.ChatListFragment;
import com.example.safesphere.fragments.HomeFragment;
import com.example.safesphere.fragments.PermissionGuideFragment;
import com.example.safesphere.fragments.ProfileFragment;

import com.example.safesphere.location.RealtimeLocationService;
import com.example.safesphere.services.BatteryForegroundService;
import com.example.safesphere.utils.SyncHelper;
import com.example.safesphere.utils.ThemeUtils;
import com.example.safesphere.utils.UserStatusHelper;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;


public class HomeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private NavigationView navigationView;
    private BottomSheetDialog bottomSheetDialog;
    private SyncHelper syncHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initViews();
        // 🔹 Start Firestore → Realtime DB sync
        syncHelper = new SyncHelper();
        syncHelper.startSync();
        navigationView.setItemIconTintList(null);
        applyThemeIconTin(navigationView);
        clickListeners();
        UserStatusHelper.detectUserStatus();

        // Battery update service
        initBatteryForgroundService(savedInstanceState);

        changeAppStatusBarColor();

        // 🔐 user login check (tumhara existing code)
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        // check if user permit for messaging service and location
        checkPermission();

        // check gps location button is enabled
        checkGPSSettings();

        startLocationTracking();

    }

    @Override
    protected void onResume(){
        super.onResume();
        // check gps location button is enabled
        checkGPSSettings();
    }

    @Override
    protected void onPause(){
        super.onPause();
        // check gps location button is enabled
        checkGPSSettings();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        // check gps location button is enabled
        checkGPSSettings();
    }

    @Override
    protected void onRestart(){
        super.onRestart();
        // check gps location button is enabled
        checkGPSSettings();
    }




    private void checkPermission(){

        // 🔀 FIRST TIME PERMISSION CHECK
        SharedPreferences sp = getSharedPreferences("app", MODE_PRIVATE);
        boolean granted = sp.getBoolean("permissions_done", false);

        if (!granted) {
            loadFragment(new PermissionGuideFragment());
        } else {
            loadFragment(new HomeFragment());
        }

    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void disableBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Intent i = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            }
        }
    }

    public void startLocationTracking() {
        // Check if permission is actually granted before starting
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {

            Intent intent = new Intent(this, RealtimeLocationService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, intent);
            } else {
                startService(intent);
            }
        }
    }


    public void checkGPSSettings() {
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        client.checkLocationSettings(builder.build())
                .addOnFailureListener(this, e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            // Ye line user ko direct GPS ON karne ka dialog dikhayegi
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            resolvable.startResolutionForResult(HomeActivity.this, 123);
                        } catch (Exception sendEx) { /* Ignore error */ }
                    }
                });
    }

    public void initBatteryForgroundService(Bundle savedInstanceState){
        Intent intent = new Intent(this, BatteryForegroundService.class);
        if (!BatteryForegroundService.isRunning) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
        }

        if (savedInstanceState == null) {
            // default fragment
            loadFragments(new HomeFragment());
            bottomNavigationView.setSelectedItemId(R.id.bottom_nav_home);
        }
    }

    public void initViews() {
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        bottomSheetDialog.setContentView(R.layout.bottomsheet_theme_menu);
        navigationView = bottomSheetDialog.findViewById(R.id.bottomSheetNavigationView);

    }


    public void clickListeners(){
        //nav item click listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.bottom_nav_home) {
                loadFragments(new HomeFragment());
            } else if (itemId == R.id.bottom_nav_chats) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ChatListFragment())
                        .addToBackStack(null)
                        .commit();
            } else if(itemId == R.id.bottom_nav_themes){
                bottomSheetDialog.show();
            }else if(itemId == R.id.bottom_nav_profile){
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new ProfileFragment())
                        .addToBackStack(null)
                        .commit();
           }
            return true;
        });


        //theme menu item click listener
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.btnCyan) {
                ThemeUtils.changeTheme(this, "cyan");
            } else if (id == R.id.btnNavyBlue) {
                ThemeUtils.changeTheme(this, "navy_blue");
            } else if (id == R.id.btnPrimary) {
                ThemeUtils.changeTheme(this, "SafeSphere");
            } else if (id == R.id.btnYellow) {
                ThemeUtils.changeTheme(this, "yellow");
            } else if (id == R.id.btnDark) {
                ThemeUtils.changeTheme(this, "SafeSphere_dark");
            } else if (id == R.id.btnDarkPink) {
                ThemeUtils.changeTheme(this, "dark_pink");
            } else if (id == R.id.btnOrange) {
                ThemeUtils.changeTheme(this, "dark_orange");
            } else if (id == R.id.btnTeal) {
                ThemeUtils.changeTheme(this, "teal");
            }
            return true;
        });
    }
    public void loadFragments(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
    }


    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    public void applyThemeIconTin(NavigationView navigationView) {
        setItemIconTint(navigationView,R.id.btnCyan,R.color.cyan);
        setItemIconTint(navigationView,R.id.btnNavyBlue,R.color.navy_blue);
        setItemIconTint(navigationView,R.id.btnPrimary,R.color.colorPrimary);
        setItemIconTint(navigationView,R.id.btnYellow,R.color.yellow);
        setItemIconTint(navigationView,R.id.btnDark,R.color.black);
        setItemIconTint(navigationView,R.id.btnDarkPink,R.color.dark_pink);
        setItemIconTint(navigationView,R.id.btnOrange,R.color.dark_orange);
        setItemIconTint(navigationView,R.id.btnTeal,R.color.teal);
    }

    public void setItemIconTint(NavigationView navigationView,int itemId, int color) {
        MenuItem item = navigationView.getMenu().findItem(itemId);
        item.getIcon().mutate().setTint(ContextCompat.getColor(this,color));
    }

    public void showNavigation(){
        bottomNavigationView.setVisibility(View.VISIBLE);
    }
    public void hideNavigation(){
        bottomNavigationView.setVisibility(View.GONE);
    }


   private void changeAppStatusBarColor(){
        // Status bar color change karne ke liye check (API 21+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // 1. Theme se colorPrimary ki value nikalna
            TypedValue typedValue = new TypedValue();
            getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
            int color = typedValue.data;

            // 2. Status bar par color apply karna
            window.setStatusBarColor(color);
        }
    }
}
