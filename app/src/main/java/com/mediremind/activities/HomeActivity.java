package com.mediremind.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mediremind.R;
import com.mediremind.adapters.AlarmLogAdapter;
import com.mediremind.database.DatabaseHelper;
import com.mediremind.models.Alarm;
import com.mediremind.models.AlarmLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 101;

    private RecyclerView rvTodaySchedule;
    private AlarmLogAdapter logAdapter;
    private TextView tvDate, tvEmptyState, tvMedicineCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setupViews();
        requestNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodayData();
    }

    private void setupViews() {
        tvDate          = findViewById(R.id.tvDate);
        tvEmptyState    = findViewById(R.id.tvEmptyState);
        tvMedicineCount = findViewById(R.id.tvMedicineCount);
        rvTodaySchedule = findViewById(R.id.rvTodaySchedule);

        rvTodaySchedule.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnScanPrescription).setOnClickListener(v ->
                startActivity(new Intent(this, PrescriptionProcessingActivity.class)));

        findViewById(R.id.btnDashboard).setOnClickListener(v ->
                startActivity(new Intent(this, DashboardActivity.class)));

        String dateStr = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
                .format(new Date());
        tvDate.setText(dateStr);
    }

    private void loadTodayData() {
        DatabaseHelper db = DatabaseHelper.getInstance(this);

        int count = db.getAllActiveMedicines().size();
        tvMedicineCount.setText(count + " active medicine" + (count != 1 ? "s" : ""));

        // Build merged list of fired logs + pending alarms (same logic as Dashboard)
        List<AlarmLog> logs = buildTodaySchedule(db);

        if (logs.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvTodaySchedule.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvTodaySchedule.setVisibility(View.VISIBLE);

            logAdapter = new AlarmLogAdapter(logs);
            rvTodaySchedule.setAdapter(logAdapter);
        }
    }

    /**
     * Merges real fired AlarmLog rows with synthetic pending entries for
     * alarms that haven't fired yet today, so the list is never empty.
     */
    private List<AlarmLog> buildTodaySchedule(DatabaseHelper db) {
        List<AlarmLog> firedLogs = db.getTodayLogs();
        List<Alarm> allEnabled   = db.getAllEnabledAlarms();

        Set<Long> firedAlarmIds = new HashSet<>();
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

        result.sort((a, b) -> {
            String ta = a.getScheduledTime() != null ? a.getScheduledTime() : "";
            String tb = b.getScheduledTime() != null ? b.getScheduledTime() : "";
            return ta.compareTo(tb);
        });

        return result;
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Notification permission is optional — proceed regardless
    }
}
