package com.mediremind.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.mediremind.R;
import com.mediremind.activities.AlarmRingActivity;
import com.mediremind.receivers.AlarmDismissReceiver;

public class NotificationHelper {

    public static final String CHANNEL_ID_ALARMS   = "mediremind_alarms";
    public static final String CHANNEL_ID_REMINDERS = "mediremind_reminders";

    public static final String ACTION_MARK_TAKEN   = "com.mediremind.ACTION_MARK_TAKEN";
    public static final String ACTION_SNOOZE_ALARM = "com.mediremind.ACTION_SNOOZE_ALARM";
    public static final String ACTION_MARK_SKIPPED = "com.mediremind.ACTION_MARK_SKIPPED";

    private static final int NOTIFICATION_BASE_ID = 1000;

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // High-priority alarm channel with sound
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        AudioAttributes audioAttr = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        NotificationChannel alarmChannel = new NotificationChannel(
                CHANNEL_ID_ALARMS,
                "Medicine Alarms",
                NotificationManager.IMPORTANCE_HIGH
        );
        alarmChannel.setDescription("Critical medicine alarm notifications");
        alarmChannel.enableLights(true);
        alarmChannel.setLightColor(Color.parseColor("#0EA5C9"));
        alarmChannel.enableVibration(true);
        alarmChannel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 500});
        alarmChannel.setSound(alarmSound, audioAttr);
        alarmChannel.setShowBadge(true);
        alarmChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(alarmChannel);

        // Reminder channel (softer)
        NotificationChannel reminderChannel = new NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        reminderChannel.setDescription("General medicine reminders");
        nm.createNotificationChannel(reminderChannel);
    }

    /**
     * Show full-screen alarm notification that launches AlarmRingActivity.
     */
    public static void showAlarmNotification(Context context, long alarmId,
                                              long logId, String medicineName,
                                              String strength, String mealRelation,
                                              String specialInstruction) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Full-screen intent → AlarmRingActivity
        Intent fullScreenIntent = new Intent(context, AlarmRingActivity.class);
        fullScreenIntent.putExtra("alarm_id",  alarmId);
        fullScreenIntent.putExtra("log_id",    logId);
        fullScreenIntent.putExtra("medicine_name",  medicineName);
        fullScreenIntent.putExtra("strength",        strength);
        fullScreenIntent.putExtra("meal_relation",   mealRelation);
        fullScreenIntent.putExtra("special_instruction", specialInstruction);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent fullScreenPI = PendingIntent.getActivity(
                context, (int) alarmId, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // "Mark Taken" action
        Intent takenIntent = new Intent(context, AlarmDismissReceiver.class);
        takenIntent.setAction(ACTION_MARK_TAKEN);
        takenIntent.putExtra("alarm_id", alarmId);
        takenIntent.putExtra("log_id",   logId);
        PendingIntent takenPI = PendingIntent.getBroadcast(
                context, (int) (alarmId + 100), takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // "Snooze" action (10 min)
        Intent snoozeIntent = new Intent(context, AlarmDismissReceiver.class);
        snoozeIntent.setAction(ACTION_SNOOZE_ALARM);
        snoozeIntent.putExtra("alarm_id", alarmId);
        snoozeIntent.putExtra("log_id",   logId);
        PendingIntent snoozePI = PendingIntent.getBroadcast(
                context, (int) (alarmId + 200), snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        String title = "💊 Medicine Reminder";
        StringBuilder body = new StringBuilder(medicineName);
        if (strength != null && !strength.isEmpty()) body.append(" ").append(strength);
        if (mealRelation != null) {
            switch (mealRelation) {
                case "before_meal":    body.append(" — Take before meal"); break;
                case "after_meal":     body.append(" — Take after meal"); break;
                case "empty_stomach":  body.append(" — Take on empty stomach"); break;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_ALARMS)
                .setSmallIcon(R.drawable.ic_pill)
                .setContentTitle(title)
                .setContentText(body.toString())
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(buildDetailedText(medicineName, strength, mealRelation, specialInstruction)))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSound(soundUri)
                .setVibrate(new long[]{0, 500, 200, 500, 200, 500})
                .setLights(Color.parseColor("#0EA5C9"), 1000, 500)
                .setFullScreenIntent(fullScreenPI, true)
                .setContentIntent(fullScreenPI)
                .addAction(R.drawable.ic_check, "Mark Taken", takenPI)
                .addAction(R.drawable.ic_snooze, "Snooze 10 min", snoozePI);

        int notifId = NOTIFICATION_BASE_ID + (int) alarmId;
        nm.notify(notifId, builder.build());
    }

    public static void cancelNotification(Context context, long alarmId) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_BASE_ID + (int) alarmId);
    }

    private static String buildDetailedText(String name, String strength,
                                             String mealRelation, String specialInstruction) {
        StringBuilder sb = new StringBuilder(name);
        if (strength != null && !strength.isEmpty()) sb.append(" ").append(strength);
        sb.append("\n");
        if (mealRelation != null) {
            switch (mealRelation) {
                case "before_meal":    sb.append("⏰ Take 30 min before meal\n"); break;
                case "after_meal":     sb.append("⏰ Take 30 min after meal\n"); break;
                case "empty_stomach":  sb.append("☕ Take on empty stomach\n"); break;
            }
        }
        if (specialInstruction != null && !specialInstruction.isEmpty()) {
            sb.append("ℹ️ ").append(specialInstruction);
        }
        return sb.toString().trim();
    }
}
