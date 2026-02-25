package com.example.safesphere.utils;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.telephony.SmsManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.safesphere.dto.TrustedContact;
import com.example.safesphere.dto.UserDto;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SOSHelper {

    @SuppressLint("MissingPermission")
    public static void startSOSProcess(Context context) {
        // 1. WhatsApp-style Custom Dialog Show Karein
        AlertDialog progressDialog = createSOSDialog(context);
        progressDialog.show();

        // 2. Current Location Fetch Karein
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        // Location mil gayi, ab SMS aur Database ka kaam karein
                        processSOS(context, location, progressDialog);
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(context, "Location is not found. please check GPS is enabled!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(context, "Location Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private static void processSOS(Context context, Location location, AlertDialog progressDialog) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        // 1. Get Address/Location Name
        String locationName = getLocationName(context, lat, lon);

        // 2. Firestore Update (isSafe = false)
        UserDto user = SharedPrefferanceUtil.isUserLoggedIn(context);
        if (user != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUserId())
                    .update("isSafe", false);
        }

        // 3. Prepare English SOS Message
        List<TrustedContact> contacts = SharedPrefferanceUtil.getTrustedContacts(context);
        String senderName = (user != null) ? user.getFullName() : "A user";

        // Standard Google Maps Link
        String mapsUrl = "https://www.google.com/maps?q=" + lat + "," + lon;

        String smsBody = "SafeSphere Alert!\n" +
                senderName + " is in an EMERGENCY and needs your help immediately!\n" +
                "Location: " + locationName + "\n" +
                "Track Live: " + mapsUrl;

        try {
            // 4. Modern SmsManager for Dual SIM and Android 6.0+
            SmsManager smsManager;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                smsManager = context.getSystemService(SmsManager.class);
            } else {
                smsManager = SmsManager.getDefault();
            }

            if (contacts == null || contacts.isEmpty()) {
                Toast.makeText(context, "No trusted contacts found!", Toast.LENGTH_SHORT).show();
            } else {
                for (TrustedContact contact : contacts) {
                    if (contact.getPhone() != null && !contact.getPhone().isEmpty()) {

                        // Divide long message into parts to avoid delivery failure
                        ArrayList<String> parts = smsManager.divideMessage(smsBody);

                        // Send as Multipart to ensure the full link is delivered
                        smsManager.sendMultipartTextMessage(contact.getPhone(), null, parts, null, null);
                    }
                }
                Toast.makeText(context, "SOS Alerts Sent Successfully!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "SMS Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        } finally {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }

    private static String getLocationName(Context context, double lat, double lon) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getAddressLine(0); // Pura address (Street, City, etc.)
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknown Location";
    }

    private static AlertDialog createSOSDialog(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(60, 60, 60, 60);

        // Background Color: ?attr/colorOnPrimary
        TypedValue bgValue = new TypedValue();
        context.getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnPrimary, bgValue, true);
        layout.setBackgroundColor(bgValue.data);

        // ProgressBar (left side)
        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(true);
        TypedValue primaryColor = new TypedValue();
        context.getTheme().resolveAttribute(androidx.appcompat.R.attr.colorPrimary, primaryColor, true);
        progressBar.getIndeterminateDrawable().setColorFilter(primaryColor.data, PorterDuff.Mode.SRC_IN);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(100, 100);
        progressBar.setLayoutParams(lp);

        // TextView (right side)
        TextView textView = new TextView(context);
        textView.setText("Sending alert message...");
        textView.setPadding(40, 0, 0, 0);
        textView.setGravity(Gravity.CENTER_VERTICAL);
        TypedValue textColor = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.textColor, textColor, true);
        textView.setTextColor(textColor.data);

        layout.addView(progressBar);
        layout.addView(textView);

        return new AlertDialog.Builder(context).setCancelable(false).setView(layout).create();
    }
}