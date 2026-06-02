package com.mediremind.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

import com.mediremind.database.DatabaseHelper;
import com.mediremind.models.Alarm;
import com.mediremind.models.AlarmLog;
import com.mediremind.notifications.NotificationHelper;
import com.mediremind.utils.AlarmScheduler;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fires when AlarmManager triggers a medicine alarm.
 * Responsibilities:
 *   1. Wake device
 *   2. Insert alarm log
 *   3. Show notification (+ full-screen intent)
 *   4. Re-schedule next occurrence
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        if (!AlarmScheduler.ACTION_ALARM_TRIGGER.equals(intent.getAction())) return;

        // Acquire wake lock briefly so we can do DB work
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = null;
        if (pm != null) {
            wl = pm.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "MediRemind::AlarmReceiver"
            );
            wl.acquire(10_000L); // max 10s
        }

        try {
            long alarmId            = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1);
            String medicineName     = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_NAME);
            String medicineType     = intent.getStringExtra(AlarmScheduler.EXTRA_MEDICINE_TYPE);
            String strength         = intent.getStringExtra(AlarmScheduler.EXTRA_STRENGTH);
            int    doseQuantity     = intent.getIntExtra(AlarmScheduler.EXTRA_DOSE_QUANTITY, 1);
            String mealRelation     = intent.getStringExtra(AlarmScheduler.EXTRA_MEAL_RELATION);
            String specialInstruction = intent.getStringExtra(AlarmScheduler.EXTRA_SPECIAL_INSTR);

            if (alarmId == -1 || medicineName == null) return;

            DatabaseHelper db = DatabaseHelper.getInstance(context);
            Alarm alarm = db.getAlarmById(alarmId);
            if (alarm == null || !alarm.isEnabled()) return;

            // 1. Insert alarm log
            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(new Date());
            AlarmLog log = new AlarmLog(alarmId, alarm.getMedicineId(), medicineName, now);
            long logId = db.insertAlarmLog(log);

            // 2. Update last triggered
            db.updateAlarmLastTriggered(alarmId, now);

            // 3. Show notification + full-screen alarm
            NotificationHelper.showAlarmNotification(
                    context, alarmId, logId, medicineName,
                    strength, mealRelation, specialInstruction
            );

            // 4. Re-schedule next occurrence
            AlarmScheduler.rescheduleNextOccurrence(context, alarm);

        } finally {
            if (wl != null && wl.isHeld()) wl.release();
        }
    }
}
