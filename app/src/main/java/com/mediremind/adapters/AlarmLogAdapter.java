package com.mediremind.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.mediremind.R;
import com.mediremind.models.AlarmLog;

import java.util.List;

public class AlarmLogAdapter extends RecyclerView.Adapter<AlarmLogAdapter.ViewHolder> {

    private final List<AlarmLog> logs;

    public AlarmLogAdapter(List<AlarmLog> logs) {
        this.logs = logs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm_log, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        AlarmLog log = logs.get(position);

        h.tvMedicineName.setText(log.getMedicineName());
        h.tvTime.setText(formatTimeAmPm(log.getScheduledTime()));
    }

    /**
     * Converts "yyyy-MM-dd HH:mm:ss" stored timestamp → "08:30 AM" display string.
     */
    private String formatTimeAmPm(String scheduledTime) {
        if (scheduledTime == null) return "--:--";
        try {
            String[] parts = scheduledTime.split(" ");
            if (parts.length >= 2) {
                String[] hms = parts[1].split(":");
                if (hms.length >= 2) {
                    int hour = Integer.parseInt(hms[0]);
                    int min  = Integer.parseInt(hms[1]);
                    String amPm = hour >= 12 ? "PM" : "AM";
                    if (hour == 0) hour = 12;
                    else if (hour > 12) hour -= 12;
                    return String.format("%02d:%02d %s", hour, min, amPm);
                }
            }
        } catch (Exception ignored) { }
        return scheduledTime;
    }

    @Override
    public int getItemCount() { return logs.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardRoot;
        TextView tvMedicineName, tvTime;

        ViewHolder(View v) {
            super(v);
            cardRoot       = v.findViewById(R.id.cardRoot);
            tvMedicineName = v.findViewById(R.id.tvMedicineName);
            tvTime         = v.findViewById(R.id.tvTime);
        }
    }
}
