package com.mediremind.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mediremind.R;
import com.mediremind.adapters.UpcomingAlarmAdapter;
import com.mediremind.database.DatabaseHelper;
import com.mediremind.models.Alarm;
import com.mediremind.models.Medicine;
import com.mediremind.models.Prescription;
import com.mediremind.utils.AlarmScheduler;

import java.util.ArrayList;
import java.util.List;

public class AlarmGenerationActivity extends AppCompatActivity {

    private Prescription prescription;
    private List<Alarm> generatedAlarms = new ArrayList<>();
    private RecyclerView rvAlarms;
    private TextView tvAlarmSummary;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_generation);

        prescription = (Prescription) getIntent().getSerializableExtra("prescription");
        if (prescription == null) {
            finish();
            return;
        }

        setupViews();
        generateAlarmPreviews();
    }

    private void setupViews() {
        rvAlarms       = findViewById(R.id.rvAlarms);
        tvAlarmSummary = findViewById(R.id.tvAlarmSummary);

        rvAlarms.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSaveAlarms).setOnClickListener(v -> saveAndSchedule());
    }

    /**
     * Generates Alarm preview objects from medicines without saving to DB yet.
     * Shows them in UpcomingAlarmAdapter so the user can edit each alarm's time
     * before hitting Save.
     */
    private void generateAlarmPreviews() {
        generatedAlarms.clear();

        List<Medicine> medicines = prescription.getMedicines();

        for (Medicine medicine : medicines) {
            // Skip SOS medicines
            if ("sos".equals(medicine.getFrequencyType())) continue;
            // Skip medicines with no frequency
            if (medicine.getFrequencyType() == null) continue;

            List<int[]> times = AlarmScheduler.resolveAlarmTimes(medicine);
            int repeatDays    = AlarmScheduler.resolveRepeatDays(medicine.getFrequencyType());

            for (int[] time : times) {
                Alarm alarm = new Alarm(
                        0, // medicineId not yet known
                        medicine.getMedicineName(),
                        medicine.getMedicineType(),
                        medicine.getStrength(),
                        medicine.getDoseQuantity(),
                        medicine.getSpecialInstruction(),
                        medicine.getMealRelation(),
                        time[0],
                        time[1],
                        repeatDays
                );
                generatedAlarms.add(alarm);
            }
        }

        // Display summary
        tvAlarmSummary.setText(generatedAlarms.size() + " alarm(s) will be created for " +
                medicines.size() + " medicine(s)");

        // Show preview with editable times — user can tap the edit (pencil) icon
        // on any alarm card to change its time before saving.
        UpcomingAlarmAdapter adapter = new UpcomingAlarmAdapter(
                generatedAlarms,
                (alarm, enabled) -> alarm.setEnabled(enabled),   // toggle — update in-memory
                (alarm, newHour, newMinute) -> {                  // time edit — already applied by adapter
                    alarm.setAlarmHour(newHour);
                    alarm.setAlarmMinute(newMinute);
                }
        );
        rvAlarms.setAdapter(adapter);
    }

    /**
     * Save prescription, medicines, and alarms to DB. Schedule each alarm.
     * Uses the (possibly user-edited) times stored in generatedAlarms.
     */
    private void saveAndSchedule() {
        DatabaseHelper db = DatabaseHelper.getInstance(this);

        // 1. Save prescription
        long prescriptionId = db.insertPrescription(prescription);
        prescription.setId(prescriptionId);

        // 2. Save medicines & map generated alarms back to their medicine IDs
        List<Alarm> alarmsToSchedule = new ArrayList<>();
        int previewIndex = 0;

        for (Medicine medicine : prescription.getMedicines()) {
            medicine.setPrescriptionId(prescriptionId);
            long medicineId = db.insertMedicine(medicine);
            medicine.setId(medicineId);

            // Skip SOS and null-frequency
            if ("sos".equals(medicine.getFrequencyType())) continue;
            if (medicine.getFrequencyType() == null) continue;

            int repeatDays = AlarmScheduler.resolveRepeatDays(medicine.getFrequencyType());
            int alarmCount = AlarmScheduler.resolveAlarmTimes(medicine).size();

            for (int i = 0; i < alarmCount && previewIndex < generatedAlarms.size(); i++, previewIndex++) {
                Alarm preview = generatedAlarms.get(previewIndex);

                // Build a fresh Alarm with the (possibly user-edited) hour/minute
                Alarm alarm = new Alarm(
                        medicineId,
                        medicine.getMedicineName(),
                        medicine.getMedicineType(),
                        medicine.getStrength(),
                        medicine.getDoseQuantity(),
                        medicine.getSpecialInstruction(),
                        medicine.getMealRelation(),
                        preview.getAlarmHour(),   // may have been edited by user
                        preview.getAlarmMinute(),  // may have been edited by user
                        repeatDays
                );
                long alarmId = db.insertAlarm(alarm);
                alarm.setId(alarmId);
                alarmsToSchedule.add(alarm);
            }
        }

        // 3. Schedule all alarms with AlarmManager
        for (Alarm alarm : alarmsToSchedule) {
            AlarmScheduler.scheduleAlarm(this, alarm);
        }

        Toast.makeText(this,
                " " + alarmsToSchedule.size() + " alarm(s) scheduled!",
                Toast.LENGTH_LONG).show();

        // Navigate to Dashboard
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
