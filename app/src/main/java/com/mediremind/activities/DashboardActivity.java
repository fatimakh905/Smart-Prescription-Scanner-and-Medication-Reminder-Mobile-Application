package com.mediremind.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mediremind.R;
import com.mediremind.adapters.AlarmLogAdapter;
import com.mediremind.adapters.UpcomingAlarmAdapter;
import com.mediremind.database.DatabaseHelper;
import com.mediremind.models.Alarm;
import com.mediremind.models.AlarmLog;
import com.mediremind.utils.AlarmScheduler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardActivity extends AppCompatActivity {

    private RecyclerView rvTodayLog, rvUpcoming;
    private TextView tvDate, tvTodayEmpty, tvUpcomingEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        setupViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void setupViews() {
        rvTodayLog      = findViewById(R.id.rvTodayLog);
        rvUpcoming      = findViewById(R.id.rvUpcoming);
        tvDate          = findViewById(R.id.tvDate);
        tvTodayEmpty    = findViewById(R.id.tvTodayEmpty);
        tvUpcomingEmpty = findViewById(R.id.tvUpcomingEmpty);

        rvTodayLog.setLayoutManager(new LinearLayoutManager(this));
        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));

        String dateStr = new SimpleDateFormat("EEEE, dd MMM", Locale.getDefault()).format(new Date());
        tvDate.setText(dateStr);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void loadData() {
        DatabaseHelper db = DatabaseHelper.getInstance(this);

        // ── Today's Log ────────────────────────────────────────────────
        List<AlarmLog> logs = buildTodaySchedule(db);

        if (logs.isEmpty()) {
            tvTodayEmpty.setVisibility(View.VISIBLE);
            rvTodayLog.setVisibility(View.GONE);
        } else {
            tvTodayEmpty.setVisibility(View.GONE);
            rvTodayLog.setVisibility(View.VISIBLE);

            AlarmLogAdapter logAdapter = new AlarmLogAdapter(logs);
            rvTodayLog.setAdapter(logAdapter);
        }

        // ── Upcoming Alarms ─────────────────────────────────────────────
        List<Alarm> alarms = db.getAllEnabledAlarms();

        if (alarms.isEmpty()) {
            tvUpcomingEmpty.setVisibility(View.VISIBLE);
            rvUpcoming.setVisibility(View.GONE);
        } else {
            tvUpcomingEmpty.setVisibility(View.GONE);
            rvUpcoming.setVisibility(View.VISIBLE);

            UpcomingAlarmAdapter upcomingAdapter = new UpcomingAlarmAdapter(
                    alarms,
                    (alarm, enabled) -> {
                        db.setAlarmEnabled(alarm.getId(), enabled);
                        if (enabled) AlarmScheduler.scheduleAlarm(this, alarm);
                        else AlarmScheduler.cancelAlarm(this, alarm.getId());
                    },
                    (alarm, newHour, newMinute) -> {
                        // Save new time to DB and reschedule
                        db.updateAlarmTime(alarm.getId(), newHour, newMinute);
                        if (alarm.isEnabled()) AlarmScheduler.scheduleAlarm(this, alarm);
                    }
            );
            rvUpcoming.setAdapter(upcomingAdapter);
        }
    }

    /**
     * Builds today's schedule by:
     * 1. Taking actual AlarmLog rows (alarms that already fired today).
     * 2. Adding synthetic pending entries for enabled alarms that have NOT fired yet today.
     */
    private List<AlarmLog> buildTodaySchedule(DatabaseHelper db) {
        List<AlarmLog> firedLogs = db.getTodayLogs();
        List<Alarm> allEnabled  = db.getAllEnabledAlarms();

        // Track which alarm IDs already have a log today
        java.util.Set<Long> firedAlarmIds = new java.util.HashSet<>();
        for (AlarmLog l : firedLogs) firedAlarmIds.add(l.getAlarmId());

        String todayDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        List<AlarmLog> result = new ArrayList<>(firedLogs);

        for (Alarm a : allEnabled) {
            if (!firedAlarmIds.contains(a.getId())) {
                AlarmLog synthetic = new AlarmLog();
                synthetic.setId(-1);
                synthetic.setAlarmId(a.getId());
                synthetic.setMedicineId(a.getMedicineId());
                synthetic.setMedicineName(a.getMedicineName());
                synthetic.setScheduledTime(
                        todayDate + " " + String.format("%02d:%02d:00", a.getAlarmHour(), a.getAlarmMinute()));
                synthetic.setWasTaken(false);
                synthetic.setSkipped(false);
                result.add(synthetic);
            }
        }

        // Sort by scheduled time
        result.sort((a, b) -> {
            String ta = a.getScheduledTime() != null ? a.getScheduledTime() : "";
            String tb = b.getScheduledTime() != null ? b.getScheduledTime() : "";
            return ta.compareTo(tb);
        });

        return result;
    }
}
