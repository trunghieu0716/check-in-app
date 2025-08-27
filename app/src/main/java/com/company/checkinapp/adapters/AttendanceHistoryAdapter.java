package com.company.checkinapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.company.checkinapp.R;
import com.company.checkinapp.models.AttendanceSession;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AttendanceHistoryAdapter extends RecyclerView.Adapter<AttendanceHistoryAdapter.ViewHolder> {

    private List<AttendanceSession> attendanceList;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public AttendanceHistoryAdapter(List<AttendanceSession> attendanceList) {
        this.attendanceList = attendanceList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceSession session = attendanceList.get(position);

        // Hiển thị ngày
        if (session.getCheckinTime() != null) {
            holder.dateText.setText(dateFormat.format(session.getCheckinTime().toDate()));
        }

        // Hiển thị thời gian check-in
        if (session.getCheckinTime() != null) {
            holder.checkinTimeText.setText("Vào: " + timeFormat.format(session.getCheckinTime().toDate()));
        } else {
            holder.checkinTimeText.setText("Vào: --:--");
        }

        // Hiển thị thời gian check-out
        if (session.getCheckoutTime() != null) {
            holder.checkoutTimeText.setText("Ra: " + timeFormat.format(session.getCheckoutTime().toDate()));
        } else {
            holder.checkoutTimeText.setText("Ra: Chưa check-out");
        }

        // Hiển thị thời gian làm việc
        if (session.getWorkDurationText() != null && !session.getWorkDurationText().isEmpty()) {
            holder.workDurationText.setText("Thời gian: " + session.getWorkDurationText());
        } else {
            holder.workDurationText.setText("Thời gian: Đang làm việc...");
        }

        // Hiển thị trạng thái
        if ("active".equals(session.getStatus())) {
            holder.statusText.setText("🟢 Đang làm việc");
            holder.statusText.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
        } else {
            holder.statusText.setText("🔴 Đã hoàn thành");
            holder.statusText.setTextColor(holder.itemView.getContext().getResources().getColor(android.R.color.holo_red_dark));
        }

        // Hiển thị vị trí check-in
        if (session.getCheckinLocation() != null) {
            holder.locationText.setText(String.format("Vị trí: %.6f, %.6f",
                    session.getCheckinLocation().getLatitude(),
                    session.getCheckinLocation().getLongitude()));
        } else {
            holder.locationText.setText("Vị trí: Không xác định");
        }
    }

    @Override
    public int getItemCount() {
        return attendanceList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView dateText, checkinTimeText, checkoutTimeText, workDurationText, statusText, locationText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            checkinTimeText = itemView.findViewById(R.id.checkinTimeText);
            checkoutTimeText = itemView.findViewById(R.id.checkoutTimeText);
            workDurationText = itemView.findViewById(R.id.workDurationText);
            statusText = itemView.findViewById(R.id.statusText);
            locationText = itemView.findViewById(R.id.locationText);
        }
    }
}