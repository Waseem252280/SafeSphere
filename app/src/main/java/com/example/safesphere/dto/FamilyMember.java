package com.example.safesphere.dto;

import com.google.firebase.database.PropertyName;

public class FamilyMember {
    private String userId;
    private String name;
    private String profileImageUrl;
    private String status;
    private int battery;
    private String location;
    private String relation;

    @PropertyName("isSafe")
    private boolean isSafe;

    private double latitude;
    private double longitude;

    public FamilyMember() { }

    public FamilyMember(String userId, String name, String profileImageUrl, String status,
                        int battery, String location, String relation, boolean isSafe,
                        double latitude, double longitude) {
        this.userId = userId;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.status = status;
        this.battery = battery;
        this.location = location;
        this.relation = relation;
        this.isSafe = isSafe;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getBattery() { return battery; }
    public void setBattery(int battery) { this.battery = battery; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }

    @PropertyName("isSafe")
    public boolean isSafe() {return isSafe;}

    @PropertyName("isSafe")
    public void setSafe(boolean safe) {
        isSafe = safe;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}
