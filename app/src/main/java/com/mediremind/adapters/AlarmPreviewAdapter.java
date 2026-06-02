package com.mediremind.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mediremind.R;
import com.mediremind.models.Alarm;

import java.util.List;

public class AlarmPreviewAdapter extends RecyclerView.Adapter<AlarmPreviewAdapter.ViewHolder> {

    private final List<Alarm> alarms;

    public AlarmPreviewAdapter(List<Alarm> alarms) {
        this.alarms = alarms;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm_preview, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Alarm a = alarms.get(position);

        h.tvTime.setText(a.getFormattedTime());
        h.tvMedicineName.setText(a.getMedicineName());

        String detail = "";
        if (a.getStrength() != null && !a.getStrength().isEmpty()) {
            detail += a.getStrength();
        }
        if (a.getMealRelationLabel() != null && !a.getMealRelationLabel().isEmpty()) {
            detail += (detail.isEmpty() ? "" : " · ") + a.getMealRelationLabel();
        }
        if (a.getRepeatEveryDays() == 14) {
            detail += " · Every 14 days";
        } else {
            detail += " · Daily";
        }
        h.tvDetail.setText(detail);
    }

    @Override
    public int getItemCount() { return alarms.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvMedicineName, tvDetail;

        ViewHolder(View v) {
            super(v);
            tvTime         = v.findViewById(R.id.tvTime);
            tvMedicineName = v.findViewById(R.id.tvMedicineName);
            tvDetail       = v.findViewById(R.id.tvDetail);
        }
    }
}
