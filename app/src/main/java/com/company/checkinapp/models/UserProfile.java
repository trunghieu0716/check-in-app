package com.company.checkinapp.models;

import com.google.firebase.Timestamp;

public class UserProfile {
    private String userId;
    private String email;
    private String displayName;
    private String department;
    private String position;
    private Timestamp createdAt;
    private Timestamp lastLoginAt;
    private boolean isActive;

    // Constructors
    public UserProfile() {}

    public UserProfile(String userId, String email, String displayName) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.isActive = true;
        this.createdAt = Timestamp.now();
        this.lastLoginAt = Timestamp.now();
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Timestamp lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}