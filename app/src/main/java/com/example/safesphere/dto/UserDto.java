package com.example.safesphere.dto;

import com.google.firebase.Timestamp;
import com.google.firebase.database.PropertyName;

import java.util.List;

public class UserDto {
    private String fullName;   // 🔹 Capital "F" hata diya
    private String email;
    private String photoUrl;
    private String userId;
    private String gender;
    private String locationName;
    private String status;
    private String deviceToken;
    private Double latitude;
    private Double longitude;
    private Timestamp lastSeen;
    private int batteryPercentage;
    private boolean isSharingLocation;

    private List<TrustedContact> trustedList;

    @PropertyName("isSafe")
    private boolean isSafe;

    // 🔹 Firestore ke liye zaroori empty constructor
    public UserDto() {}

    public UserDto(String userId, String fullName, String email, String photoUrl, String gender,
                   String locationName, String status, String deviceToken,
                   Double latitude, Double longitude, Timestamp lastSeen,
                   int batteryPercentage, boolean isSharingLocation, boolean isSafe, List<TrustedContact> trustedList) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.photoUrl = photoUrl;
        this.gender = gender;
        this.locationName = locationName;
        this.status = status;
        this.deviceToken = deviceToken;
        this.latitude = latitude;
        this.longitude = longitude;
        this.lastSeen = lastSeen;
        this.batteryPercentage = batteryPercentage;
        this.isSharingLocation = isSharingLocation;
        this.isSafe = isSafe;
        this.trustedList = trustedList;
    }

    // ✅ Getters & Setters
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDeviceToken() { return deviceToken; }
    public void setDeviceToken(String deviceToken) { this.deviceToken = deviceToken; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Timestamp getLastSeen() { return lastSeen; }
    public void setLastSeen(Timestamp lastSeen) { this.lastSeen = lastSeen; }

    public int getBatteryPercentage() { return batteryPercentage; }
    public void setBatteryPercentage(int batteryPercentage) { this.batteryPercentage = batteryPercentage; }

    public boolean isSharingLocation() { return isSharingLocation; }
    public void setSharingLocation(boolean sharingLocation) { isSharingLocation = sharingLocation; }

    @PropertyName("isSafe")
    public boolean getIsSafe() {return isSafe;}

    @PropertyName("isSafe")
    public void setIsSafe(boolean safe) {
        isSafe = safe;
    }

    public List<TrustedContact> getTrustedList() {
        return trustedList;
    }

    public void setTrustedList(List<TrustedContact> trustedList) {
        this.trustedList = trustedList;
    }

    @Override
    public String toString() {
        return "UserDto{" +
                "fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", photoUrl='" + photoUrl + '\'' +
                ", userId='" + userId + '\'' +
                ", gender='" + gender + '\'' +
                ", locationName='" + locationName + '\'' +
                ", status='" + status + '\'' +
                ", deviceToken='" + deviceToken + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", lastSeen=" + lastSeen +
                ", batteryPercentage=" + batteryPercentage +
                ", isSharingLocation=" + isSharingLocation +
                ", isSafe=" + isSafe +
                '}';
    }
}
