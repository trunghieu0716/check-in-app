package com.company.checkinapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.company.checkinapp.R;
import com.company.checkinapp.adapters.AttendanceHistoryAdapter;
import com.company.checkinapp.models.AttendanceSession;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AttendanceHistoryAdapter adapter;
    private List<AttendanceSession> attendanceList;
    private FirebaseFirestore db;
    private static final String TAG = "HistoryActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Setup toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lịch sử chấm công");
        }

        recyclerView = findViewById(R.id.recyclerViewHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        attendanceList = new ArrayList<>();
        adapter = new AttendanceHistoryAdapter(attendanceList);
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        loadAttendanceHistory();
    }

    private void loadAttendanceHistory() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("attendanceSessions")
                .whereEqualTo("userId", userId)
                .orderBy("checkinTime", Query.Direction.DESCENDING)
                .limit(50) // Giới hạn 50 records gần nhất
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attendanceList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        AttendanceSession session = document.toObject(AttendanceSession.class);
                        session.setId(document.getId());
                        attendanceList.add(session);
                    }
                    adapter.notifyDataSetChanged();

                    if (attendanceList.isEmpty()) {
                        Toast.makeText(this, "Chưa có dữ liệu chấm công", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load attendance history", e);
                    Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}