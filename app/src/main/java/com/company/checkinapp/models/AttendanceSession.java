package com.company.checkinapp.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

public class AttendanceSession {
    private String id;
    private String userId;
    private Timestamp checkinTime;
    private Timestamp checkoutTime;
    private GeoPoint checkinLocation;
    private GeoPoint checkoutLocation;
    private long workDurationMs;
    private String workDurationText;
    private String status; // "active", "completed"
    private String date; // YYYY-MM-DD format for easy querying

    // Constructors
    public AttendanceSession() {}

    public AttendanceSession(String userId, Timestamp checkinTime, GeoPoint checkinLocation, String date) {
        this.userId = userId;
        this.checkinTime = checkinTime;
        this.checkinLocation = checkinLocation;
        this.date = date;
        this.status = "active";
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Timestamp getCheckinTime() { return checkinTime; }
    public void setCheckinTime(Timestamp checkinTime) { this.checkinTime = checkinTime; }

    public Timestamp getCheckoutTime() { return checkoutTime; }
    public void setCheckoutTime(Timestamp checkoutTime) { this.checkoutTime = checkoutTime; }

    public GeoPoint getCheckinLocation() { return checkinLocation; }
    public void setCheckinLocation(GeoPoint checkinLocation) { this.checkinLocation = checkinLocation; }

    public GeoPoint getCheckoutLocation() { return checkoutLocation; }
    public void setCheckoutLocation(GeoPoint checkoutLocation) { this.checkoutLocation = checkoutLocation; }

    public long getWorkDurationMs() { return workDurationMs; }
    public void setWorkDurationMs(long workDurationMs) { this.workDurationMs = workDurationMs; }

    public String getWorkDurationText() { return workDurationText; }
    public void setWorkDurationText(String workDurationText) { this.workDurationText = workDurationText; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}