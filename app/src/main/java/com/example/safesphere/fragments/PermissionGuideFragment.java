package com.example.safesphere.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.safesphere.R;
import com.example.safesphere.activity.HomeActivity;

import java.util.ArrayList;
import java.util.List;

public class PermissionGuideFragment extends Fragment {

    private static final int REQ_CODE = 101;

    public PermissionGuideFragment() {
        // Required empty constructor
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_permission_guide, container, false);

        v.findViewById(R.id.btnGrant).setOnClickListener(view -> requestPermissions());

        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).hideNavigation();
        }

        return v;
    }

    private void requestPermissions() {
        if (checkAllPermissionsGranted()) {
            goToHome();
            return;
        }

        // Agar basic permissions (SMS, Fine Location) nahi hain
        List<String> perms = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.SEND_SMS);
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED)
            perms.add(Manifest.permission.RECEIVE_SMS);

        if (!perms.isEmpty()) {
            requestPermissions(perms.toArray(new String[0]), REQ_CODE);
        } else {
            // Basic permissions mil chuki hain, ab background mangein
            requestBackgroundLocation();
        }
    }

    private void showSettingsDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setTitle("Permissions Required")
                .setMessage("You have denied some permissions. Please enable them manually in App Settings to use SafeSphere.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestBackgroundLocation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // 💡 User ko samjhane ke liye Dialog
                new androidx.appcompat.app.AlertDialog.Builder(getContext())
                        .setTitle("Background Location Needed")
                        .setMessage("SafeSphere needs 'Allow all the time' location access to protect you even when the app is closed. \n\nIn the next screen, please select 'Allow all the time'.")
                        .setPositiveButton("Configure in Settings", (dialog, which) -> {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 102);
                        })
                        .setNegativeButton("Cancel", (dialog, i) -> showErrorToast("Background safety disabled."))
                        .setCancelable(false)
                        .show();
            } else {
                goToHome();
            }
        } else {
            goToHome();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_CODE) {
            // Check karein ke kya list khali to nahi (User ne dialog cancel kiya ho)
            if (grantResults.length > 0) {

                // Basic Permissions Check
                boolean fineLocationGranted = ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                boolean sendSmsGranted = ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
                boolean receiveSmsGranted = ContextCompat.checkSelfPermission(requireContext(),
                        Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;

                if (fineLocationGranted && sendSmsGranted && receiveSmsGranted) {
                    // Agar sab basic permissions mil gayi, to background mangen
                    requestBackgroundLocation();
                } else {
                    // Agar koi bhi basic permission reh gayi
                    showErrorToast("SMS and Location permissions are mandatory.");
                }
            }
        } else if (requestCode == 102) {
            // Background Location ka result
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                goToHome();
            } else {
                showErrorToast("Please select 'Allow all the time' in Settings.");
            }
        }
    }
    private void showErrorToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    private void goToHome() {

        // 1️⃣ mark permissions done
        SharedPreferences sp =
                requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
        sp.edit().putBoolean("permissions_done", true).apply();

        // 2️⃣ battery optimization dialog
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).disableBatteryOptimization();
            ((HomeActivity) getActivity()).startLocationTracking();
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).showNavigation();
            }

        }

        // 3️⃣ go to HomeFragment
//        requireActivity().getSupportFragmentManager()
//                .beginTransaction()
//                .replace(R.id.fragment_container, new HomeFragment())
//                .commit();
        requireContext().startActivity(new Intent(requireContext(),HomeActivity.class));
    }


    @Override
    public void onResume() {
        super.onResume();

        // Check karein ke permissions done hain ya nahi
        SharedPreferences sp = requireActivity().getSharedPreferences("app", Context.MODE_PRIVATE);
        boolean alreadyDone = sp.getBoolean("permissions_done", false);

        // Agar user Settings se "Allow all the time" karke wapas aaya hai
        if (!alreadyDone && checkAllPermissionsGranted()) {
            goToHome();
        }
    }

    private boolean checkAllPermissionsGranted() {
        boolean fineLoc = ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        boolean smsSend = ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;

        // Background location check (Android 10+)
        boolean backgroundLoc = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundLoc = ContextCompat.checkSelfPermission(getContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }

        return fineLoc && smsSend && backgroundLoc;
    }

}
