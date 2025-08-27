package com.company.checkinapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import com.company.checkinapp.activities.HistoryActivity;
import com.company.checkinapp.activities.LoginActivity;
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
import java.util.Date;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private Button checkinBtn, checkoutBtn, historyBtn;
    private Button logoutBtn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;
    private static final String TAG = "MainActivity";

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

        checkinBtn.setOnClickListener(v -> markAttendance("checkin"));
        checkoutBtn.setOnClickListener(v -> markAttendance("checkout"));
        historyBtn.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));

        logoutBtn.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận đăng xuất")
                    .setMessage("Bạn có chắc chắn muốn đăng xuất?")
                    .setPositiveButton("Đăng xuất", (dialog, which) -> {
                        FirebaseAuth.getInstance().signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        askLocationPermissionIfNeeded();
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
        boolean fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!fine && !coarse) {
            askLocationPermissionIfNeeded();
            return;
        }

        // Hiển thị loading
        Toast.makeText(this, "Đang lấy vị trí...", Toast.LENGTH_SHORT).show();

        // Thử lấy last known location trước
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

        // Kiểm tra location settings trước
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = settingsClient.checkLocationSettings(builder.build());

        task.addOnSuccessListener(locationSettingsResponse -> {
            // Location settings are satisfied, proceed with location request
            proceedWithLocationRequest(type, locationRequest);
        });

        task.addOnFailureListener(e -> {
            Log.w(TAG, "Location settings not satisfied", e);
            // Still try to get location even if settings are not ideal
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

                    // Dừng location updates
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

            // Timeout sau 20 giây nếu không lấy được location
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

        // Thay vì query phức tạp, chỉ lưu trực tiếp nếu gặp lỗi index
        // Sau này khi đã tạo index thì có thể uncomment phần check duplicate
        saveAttendanceRecord(type, geoPoint, uid);

        /*
        // Code này sẽ hoạt động sau khi tạo composite index
        db.collection("attendance")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    boolean skip = false;
                    if (!qs.isEmpty()) {
                        DocumentSnapshot last = qs.getDocuments().get(0);
                        String lastType = last.getString("type");
                        com.google.firebase.Timestamp ts = last.getTimestamp("timestamp");
                        if (ts != null && lastType != null) {
                            Date lastDate = ts.toDate();
                            Date now = new Date();
                            Calendar c1 = Calendar.getInstance(); c1.setTime(lastDate);
                            Calendar c2 = Calendar.getInstance(); c2.setTime(now);
                            boolean sameDay = c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                                    && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH)
                                    && c1.get(Calendar.DAY_OF_MONTH) == c2.get(Calendar.DAY_OF_MONTH);
                            if (sameDay && lastType.equals(type)) {
                                Toast.makeText(this, "Bạn đã " + type + " hôm nay rồi", Toast.LENGTH_SHORT).show();
                                skip = true;
                            }
                        }
                    }

                    if (!skip) {
                        saveAttendanceRecord(type, geoPoint, uid);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to query last attendance", e);

                    // Nếu lỗi là do thiếu index, vẫn cho phép lưu attendance
                    if (e.getMessage() != null && e.getMessage().contains("FAILED_PRECONDITION")) {
                        Log.w(TAG, "Index not found, proceeding without duplicate check");
                        Toast.makeText(this, "Cảnh báo: Không thể kiểm tra trùng lặp do thiếu index", Toast.LENGTH_SHORT).show();
                    }
                    saveAttendanceRecord(type, geoPoint, uid);
                });
        */
    }

    private void saveAttendanceRecord(String type, GeoPoint geoPoint, String uid) {
        Map<String, Object> record = new HashMap<>();
        record.put("userId", uid);
        record.put("type", type);
        record.put("timestamp", FieldValue.serverTimestamp());
        if (geoPoint != null) {
            record.put("location", geoPoint);
        } else {
            record.put("location", null);
            Log.w(TAG, "Location is null, saving record without location");
        }

        db.collection("attendance")
                .add(record)
                .addOnSuccessListener(doc -> {
                    String actionText = type.equals("checkin") ? "Chấm công vào" : "Chấm công ra";
                    String locationText = geoPoint != null
                            ? "với vị trí (lat: " + String.format("%.6f", geoPoint.getLatitude()) +
                            ", lon: " + String.format("%.6f", geoPoint.getLongitude()) + ")"
                            : "(không có vị trí)";

                    String message = actionText + " thành công " + locationText;
                    statusText.setText(message);

                    // Hiển thị dialog thành công
                    showSuccessDialog(actionText, geoPoint);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save attendance", e);
                    String errorMsg = "Lỗi: " + e.getMessage();
                    Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                });
    }

    private void showSuccessDialog(String actionText, GeoPoint geoPoint) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("✅ Thành công!");

        String message = actionText + " thành công!\n\n";
        message += "Thời gian: " + java.text.DateFormat.getDateTimeInstance().format(new Date()) + "\n";

        if (geoPoint != null) {
            message += "Vị trí: " + String.format("%.6f", geoPoint.getLatitude()) +
                    ", " + String.format("%.6f", geoPoint.getLongitude());
        } else {
            message += "Vị trí: Không xác định được";
        }

        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.setIcon(android.R.drawable.ic_dialog_info);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Tự động đóng sau 3 giây
        new android.os.Handler().postDelayed(() -> {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }, 3000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
    }
}