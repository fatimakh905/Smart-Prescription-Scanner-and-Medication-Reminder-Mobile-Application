package com.mediremind.adapters;

import android.app.TimePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mediremind.R;
import com.mediremind.models.Alarm;

import java.util.List;

public class UpcomingAlarmAdapter extends RecyclerView.Adapter<UpcomingAlarmAdapter.ViewHolder> {

    public interface OnToggleListener {
        void onToggle(Alarm alarm, boolean enabled);
    }

    public interface OnTimeChangedListener {
        void onTimeChanged(Alarm alarm, int newHour, int newMinute);
    }

    private final List<Alarm> alarms;
    private final OnToggleListener toggleListener;
    private final OnTimeChangedListener timeChangedListener;

    public UpcomingAlarmAdapter(List<Alarm> alarms,
                                 OnToggleListener toggleListener,
                                 OnTimeChangedListener timeChangedListener) {
        this.alarms = alarms;
        this.toggleListener = toggleListener;
        this.timeChangedListener = timeChangedListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming_alarm, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Alarm a = alarms.get(position);

        h.tvTime.setText(a.getFormattedTime()); // shows AM/PM

        h.tvName.setText(a.getMedicineName());

        // Subtitle: meal relation + special instruction (no strength, no null)
        StringBuilder sub = new StringBuilder();
        String meal = a.getMealRelationLabel();
        if (!meal.isEmpty()) {
            sub.append(meal);
        }
        String instr = a.getSpecialInstruction();
        if (instr != null && !instr.isEmpty()) {
            if (sub.length() > 0) sub.append(" · ");
            sub.append(instr);
        }
        // Repeat frequency
        if (sub.length() > 0) sub.append(" · ");
        sub.append(a.getRepeatEveryDays() == 14 ? "Every 14 days" : "Daily");

        h.tvSubtitle.setText(sub.toString());

        // Edit alarm time — opens a TimePickerDialog
        h.btnEditTime.setOnClickListener(v -> {
            TimePickerDialog picker = new TimePickerDialog(
                    v.getContext(),
                    (view, hourOfDay, minute) -> {
                        a.setAlarmHour(hourOfDay);
                        a.setAlarmMinute(minute);
                        h.tvTime.setText(a.getFormattedTime());
                        if (timeChangedListener != null) {
                            timeChangedListener.onTimeChanged(a, hourOfDay, minute);
                        }
                    },
                    a.getAlarmHour(),
                    a.getAlarmMinute(),
                    false // false = 12-hour mode with AM/PM
            );
            picker.show();
        });

        // Toggle enable/disable
        h.switchEnabled.setOnCheckedChangeListener(null);
        h.switchEnabled.setChecked(a.isEnabled());
        h.switchEnabled.setOnCheckedChangeListener((btn, checked) -> {
            a.setEnabled(checked);
            toggleListener.onToggle(a, checked);
        });
    }

    @Override
    public int getItemCount() { return alarms.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTime, tvName, tvSubtitle;
        ImageButton btnEditTime;
        SwitchCompat switchEnabled;

        ViewHolder(View v) {
            super(v);
            tvTime        = v.findViewById(R.id.tvTime);
            tvName        = v.findViewById(R.id.tvName);
            tvSubtitle    = v.findViewById(R.id.tvSubtitle);
            btnEditTime   = v.findViewById(R.id.btnEditTime);
            switchEnabled = v.findViewById(R.id.switchEnabled);
        }
    }
}
