package com.company.checkinapp.activities;

import android.app.Activity;



import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.company.checkinapp.AttendanceRecord;
import com.company.checkinapp.R;
import com.company.checkinapp.adapters.AttendanceAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends Activity {

    private RecyclerView recycler;
    private AttendanceAdapter adapter;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recycler = findViewById(R.id.recycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AttendanceAdapter();
        recycler.setAdapter(adapter);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db = FirebaseFirestore.getInstance();

        loadData();
    }

    private void loadData() {
        db.collection("attendance")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snap -> {
                    List<AttendanceRecord> list = new ArrayList<>();
                    snap.forEach(doc -> list.add(doc.toObject(AttendanceRecord.class)));
                    adapter.setData(list);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi tải lịch sử: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}

