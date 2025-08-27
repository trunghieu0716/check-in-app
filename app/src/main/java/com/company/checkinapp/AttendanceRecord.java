package com.company.checkinapp;


import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

public class AttendanceRecord {
    private String userId;
    private String type;          // "checkin" | "checkout"
    private Timestamp timestamp;  // server time
    private GeoPoint location;    // lat/lng

    public AttendanceRecord() { } // Firestore cần constructor rỗng

    public AttendanceRecord(String userId, String type, Timestamp timestamp, GeoPoint location) {
        this.userId = userId;
        this.type = type;
        this.timestamp = timestamp;
        this.location = location;
    }

    public String getUserId() { return userId; }
    public String getType() { return type; }
    public Timestamp getTimestamp() { return timestamp; }
    public GeoPoint getLocation() { return location; }

    public void setUserId(String userId) { this.userId = userId; }
    public void setType(String type) { this.type = type; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public void setLocation(GeoPoint location) { this.location = location; }
}

