package com.example.safesphere.dto;

import java.io.Serializable;

public class TrustedContact implements Serializable {
    private String name;
    private String phone; // Variable name 'phone' rakha hai 'number' ki jagah sync ke liye

    public TrustedContact() {}

    public TrustedContact(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}