package com.mediremind.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mediremind.database.DatabaseHelper;
import com.mediremind.notifications.NotificationHelper;

import java.util.Calendar;

/**
 * Handles notification action buttons:
 *  - Mark Taken
 *  - Snooze (10 minutes)
 *  - Mark Skipped
 *  - Dismiss
 */
public class AlarmDismissReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        long alarmId = intent.getLongExtra("alarm_id", -1);
        long logId   = intent.getLongExtra("log_id",   -1);
        String action = intent.getAction();

        if (action == null || alarmId == -1) return;

        DatabaseHelper db = DatabaseHelper.getInstance(context);

        switch (action) {
            case NotificationHelper.ACTION_MARK_TAKEN:
                if (logId != -1) db.markLogTaken(logId);
                NotificationHelper.cancelNotification(context, alarmId);
                break;

            case NotificationHelper.ACTION_SNOOZE_ALARM:
                // Snooze 10 minutes
                NotificationHelper.cancelNotification(context, alarmId);
                scheduleSnooze(context, alarmId, logId, intent);
                break;

            case NotificationHelper.ACTION_MARK_SKIPPED:
                if (logId != -1) db.markLogSkipped(logId);
                NotificationHelper.cancelNotification(context, alarmId);
                break;

            case "com.mediremind.ACTION_DISMISS_ALARM":
                NotificationHelper.cancelNotification(context, alarmId);
                break;
        }
    }

    private void scheduleSnooze(Context context, long alarmId, long logId, Intent originalIntent) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        // Re-use original intent extras for the snooze trigger
        Intent snoozeIntent = new Intent(context, AlarmReceiver.class);
        snoozeIntent.setAction(com.mediremind.utils.AlarmScheduler.ACTION_ALARM_TRIGGER);

        // Copy all extras
        if (originalIntent.getExtras() != null) {
            snoozeIntent.putExtras(originalIntent.getExtras());
        }

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                (int) (alarmId + 900), // unique request code for snooze
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000L); // 10 minutes

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pi);
                return;
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, snoozeTime, pi);
        }
    }
}
