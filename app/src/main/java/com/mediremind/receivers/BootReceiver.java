package com.mediremind.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mediremind.database.DatabaseHelper;
import com.mediremind.models.Alarm;
import com.mediremind.utils.AlarmScheduler;

import java.util.List;

/**
 * Reschedules all active alarms after device reboot.
 * AlarmManager alarms are wiped on reboot — this restores them.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
                !"android.intent.action.LOCKED_BOOT_COMPLETED".equals(action)) return;

        // Use goAsync() to avoid ANR on DB access post-boot
        PendingResult pendingResult = goAsync();
        new Thread(() -> {
            try {
                DatabaseHelper db = DatabaseHelper.getInstance(context);
                List<Alarm> alarms = db.getAllEnabledAlarms();
                for (Alarm alarm : alarms) {
                    AlarmScheduler.scheduleAlarm(context, alarm);
                }
            } finally {
                pendingResult.finish();
            }
        }).start();
    }
}
