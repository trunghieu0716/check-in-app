package com.company.checkinapp.adapters;



import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.company.checkinapp.AttendanceRecord;
import com.company.checkinapp.R;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.GeoPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AttendanceAdapter extends RecyclerView.Adapter<AttendanceAdapter.VH> {

    private final List<AttendanceRecord> data = new ArrayList<>();
    private final SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    public void setData(List<AttendanceRecord> list) {
        data.clear();
        if (list != null) data.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        AttendanceRecord r = data.get(position);
        h.tvType.setText("checkin".equals(r.getType()) ? "Check-in" : "Check-out");

        Timestamp ts = r.getTimestamp();
        h.tvTime.setText(ts != null ? fmt.format(ts.toDate()) : "(đang đồng bộ…)");

        GeoPoint gp = r.getLocation();
        h.tvLocation.setText(gp != null ? gp.getLatitude() + ", " + gp.getLongitude()
                : "Không có vị trí");
    }

    @Override
    public int getItemCount() { return data.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvType, tvTime, tvLocation;
        VH(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvType);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvLocation = itemView.findViewById(R.id.tvLocation);
        }
    }
}
