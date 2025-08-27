package com.company.checkinapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.company.checkinapp.activities.HistoryActivity;
import com.company.checkinapp.activities.LoginActivity;
import com.company.checkinapp.models.AttendanceSession;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.Timestamp;
import java.util.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private Button checkinBtn, checkoutBtn, historyBtn;
    private Button logoutBtn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;
    private static final String TAG = "MainActivity";
    
    // SharedPreferences để lưu trạng thái
    private SharedPreferences preferences;
    private static final String PREF_NAME = "attendance_state";
    private static final String KEY_IS_CHECKED_IN = "is_checked_in";
    private static final String KEY_CHECKIN_TIME = "checkin_time";
    private static final String KEY_SESSION_ID = "session_id";

    private final ActivityResultLauncher<String[]> requestLocationPermissions =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean fine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                Boolean coarse = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
                if (!(Boolean.TRUE.equals(fine) || Boolean.TRUE.equals(coarse))) {
                    Toast.makeText(this, "Bạn cần cấp quyền vị trí để chấm công", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        checkinBtn = findViewById(R.id.checkinBtn);
        checkoutBtn = findViewById(R.id.checkoutBtn);
        historyBtn = findViewById(R.id.historyBtn);
        logoutBtn = findViewById(R.id.logoutBtn);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        checkinBtn.setOnClickListener(v -> markAttendance("checkin"));
        checkoutBtn.setOnClickListener(v -> markAttendance("checkout"));
        historyBtn.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));

        logoutBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        clearAttendanceState();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        askLocationPermissionIfNeeded();
        updateUIBasedOnState();
    }

    private void updateUIBasedOnState() {
        boolean isCheckedIn = preferences.getBoolean(KEY_IS_CHECKED_IN, false);
        long checkinTime = preferences.getLong(KEY_CHECKIN_TIME, 0);
        
        if (isCheckedIn && checkinTime > 0) {
            checkinBtn.setEnabled(false);
            checkoutBtn.setEnabled(true);
            
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
            String checkinTimeStr = sdf.format(new Date(checkinTime));
            statusText.setText("Đã check-in lúc: " + checkinTimeStr + "\nChờ check-out...");
            
            checkinBtn.setText("Đã Check-in");
            checkoutBtn.setText("Check-out");
        } else {
            checkinBtn.setEnabled(true);
            checkoutBtn.setEnabled(false);
            
            statusText.setText("Chưa chấm công");
            checkinBtn.setText("Check-in");
            checkoutBtn.setText("Chưa thể Check-out");
        }
    }

    private void askLocationPermissionIfNeeded() {
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if (!fine && !coarse) {
            requestLocationPermissions.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void markAttendance(String type) {
        boolean isCheckedIn = preferences.getBoolean(KEY_IS_CHECKED_IN, false);
        
        if (type.equals("checkin") && isCheckedIn) {
            Toast.makeText(this, "Bạn đã check-in rồi! Hãy check-out trước.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (type.equals("checkout") && !isCheckedIn) {
            Toast.makeText(this, "Bạn chưa check-in! Hãy check-in trước.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!fine && !coarse) {
            askLocationPermissionIfNeeded();
            return;
        }

        Toast.makeText(this, "Đang lấy vị trí...", Toast.LENGTH_SHORT).show();

        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                Log.i(TAG, "Last location obtained: lat=" + location.getLatitude() + " lon=" + location.getLongitude());
                processAttendanceWithLocation(type, new GeoPoint(location.getLatitude(), location.getLongitude()));
            } else {
                Log.w(TAG, "Last location is null, requesting fresh location");
                requestFreshLocation(type);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get last location", e);
            requestFreshLocation(type);
        });
    }

    private void requestFreshLocation(String type) {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(15000)
                .build();

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(locationSettingsResponse -> {
            proceedWithLocationRequest(type, locationRequest);
        });

        task.addOnFailureListener(e -> {
            Log.w(TAG, "Location settings not satisfied", e);
            proceedWithLocationRequest(type, locationRequest);
        });
    }

    private void proceedWithLocationRequest(String type, LocationRequest locationRequest) {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (locationResult != null && !locationResult.getLocations().isEmpty()) {
                    android.location.Location location = locationResult.getLastLocation();
                    Log.i(TAG, "Fresh location obtained: lat=" + location.getLatitude() + " lon=" + location.getLongitude());

                    locationClient.removeLocationUpdates(locationCallback);
                    processAttendanceWithLocation(type, new GeoPoint(location.getLatitude(), location.getLongitude()));
                }
            }
        };

        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (fine || coarse) {
            locationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());

            new android.os.Handler().postDelayed(() -> {
                if (locationCallback != null) {
                    locationClient.removeLocationUpdates(locationCallback);
                    Toast.makeText(this, "Không thể lấy vị trí, sẽ lưu không có location", Toast.LENGTH_LONG).show();
                    processAttendanceWithLocation(type, null);
                }
            }, 20000);
        } else {
            Toast.makeText(this, "Cần quyền truy cập vị trí", Toast.LENGTH_SHORT).show();
        }
    }

    private void processAttendanceWithLocation(String type, GeoPoint geoPoint) {
        String uid = mAuth.getCurrentUser().getUid();
        
        if (type.equals("checkin")) {
            createNewAttendanceSession(uid, geoPoint);
        } else {
            updateAttendanceSession(uid, geoPoint);
        }
    }

    private void createNewAttendanceSession(String uid, GeoPoint geoPoint) {
        // Tạo date string cho ngày hôm nay
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("userId", uid);
        sessionData.put("checkinTime", FieldValue.serverTimestamp());
        sessionData.put("checkinLocation", geoPoint);
        sessionData.put("date", todayDate);
        sessionData.put("status", "active");

        db.collection("attendanceSessions")
                .add(sessionData)
                .addOnSuccessListener(doc -> {
                    Log.d(TAG, "Attendance session created: " + doc.getId());
                    
                    String locationText = geoPoint != null
                            ? "với vị trí (lat: " + String.format("%.6f", geoPoint.getLatitude()) +
                            ", lon: " + String.format("%.6f", geoPoint.getLongitude()) + ")"
                            : "(không có vị trí)";

                    statusText.setText("Check-in thành công " + locationText);
                    updateLocalState("checkin", doc.getId());
                    showSuccessDialog("Chấm công vào", geoPoint, "checkin");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create attendance session", e);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateAttendanceSession(String uid, GeoPoint geoPoint) {
        String sessionId = preferences.getString(KEY_SESSION_ID, "");
        if (sessionId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy phiên check-in", Toast.LENGTH_SHORT).show();
            return;
        }

        long checkinTime = preferences.getLong(KEY_CHECKIN_TIME, 0);
        long workDuration = System.currentTimeMillis() - checkinTime;
        long hours = TimeUnit.MILLISECONDS.toHours(workDuration);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(workDuration) % 60;

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("checkoutTime", FieldValue.serverTimestamp());
        updateData.put("checkoutLocation", geoPoint);
        updateData.put("workDurationMs", workDuration);
        updateData.put("workDurationText", hours + " giờ " + minutes + " phút");
        updateData.put("status", "completed");

        db.collection("attendanceSessions").document(sessionId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Attendance session updated: " + sessionId);
                    
                    String locationText = geoPoint != null
                            ? "với vị trí (lat: " + String.format("%.6f", geoPoint.getLatitude()) +
                            ", lon: " + String.format("%.6f", geoPoint.getLongitude()) + ")"
                            : "(không có vị trí)";

                    statusText.setText("Check-out thành công " + locationText);
                    updateLocalState("checkout", "");
                    showSuccessDialog("Chấm công ra", geoPoint, "checkout");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update attendance session", e);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void updateLocalState(String type, String sessionId) {
        SharedPreferences.Editor editor = preferences.edit();
        
        if (type.equals("checkin")) {
            editor.putBoolean(KEY_IS_CHECKED_IN, true);
            editor.putLong(KEY_CHECKIN_TIME, System.currentTimeMillis());
            editor.putString(KEY_SESSION_ID, sessionId);
        } else if (type.equals("checkout")) {
            editor.putBoolean(KEY_IS_CHECKED_IN, false);
            editor.remove(KEY_CHECKIN_TIME);
            editor.remove(KEY_SESSION_ID);
        }
        
        editor.apply();
        updateUIBasedOnState();
    }

    private void clearAttendanceState() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    private void showSuccessDialog(String actionText, GeoPoint geoPoint, String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✅ Thành công!");

        String message = actionText + " thành công!\n\n";
        message += "Thời gian: " + java.text.DateFormat.getDateTimeInstance().format(new Date()) + "\n";

        if (geoPoint != null) {
            message += "Vị trí: " + String.format("%.6f", geoPoint.getLatitude()) +
                    ", " + String.format("%.6f", geoPoint.getLongitude()) + "\n";
        } else {
            message += "Vị trí: Không xác định được\n";
        }

        if (type.equals("checkout")) {
            long checkinTime = preferences.getLong(KEY_CHECKIN_TIME, 0);
            if (checkinTime > 0) {
                long workDuration = System.currentTimeMillis() - checkinTime;
                long hours = TimeUnit.MILLISECONDS.toHours(workDuration);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(workDuration) % 60;
                message += "\nThời gian làm việc: " + hours + " giờ " + minutes + " phút";
            }
        }

        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.setIcon(android.R.drawable.ic_dialog_info);

        AlertDialog dialog = builder.create();
        dialog.show();

        new android.os.Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }, 5000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUIBasedOnState();
    }
}