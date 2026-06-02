package com.mediremind.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.mediremind.models.Alarm;
import com.mediremind.models.Medicine;
import com.mediremind.receivers.AlarmReceiver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Converts medicine frequency/timing rules into concrete Alarm objects
 * and schedules them via AlarmManager.
 */
public class AlarmScheduler {

    public static final String ACTION_ALARM_TRIGGER = "com.mediremind.ACTION_ALARM_TRIGGER";
    public static final String EXTRA_ALARM_ID       = "alarm_id";
    public static final String EXTRA_MEDICINE_NAME  = "medicine_name";
    public static final String EXTRA_MEDICINE_TYPE  = "medicine_type";
    public static final String EXTRA_STRENGTH       = "strength";
    public static final String EXTRA_DOSE_QUANTITY  = "dose_quantity";
    public static final String EXTRA_MEAL_RELATION  = "meal_relation";
    public static final String EXTRA_SPECIAL_INSTR  = "special_instruction";

    // ─── ALARM TIME RESOLUTION ───────────────────────────────────────────────

    /**
     * Converts a Medicine's frequencyType + timingHint → list of (hour, minute) pairs.
     * Applies meal_relation offset.
     * Returns empty list for SOS (no alarm).
     *
     * Each int[] = { hour, minute }
     */
    public static List<int[]> resolveAlarmTimes(Medicine medicine) {
        List<int[]> times = new ArrayList<>();
        String freq   = medicine.getFrequencyType();
        String timing = medicine.getTimingHint();
        String meal   = medicine.getMealRelation();

        if (freq == null || freq.equals("sos")) return times; // no alarm for SOS

        // empty_stomach overrides everything → fixed 07:30
        if ("empty_stomach".equals(meal)) {
            times.add(new int[]{7, 30});
            return times;
        }

        switch (freq) {
            case "daily":
                times.add(resolveDaily(timing));
                break;

            case "morning_evening":
                times.add(new int[]{8, 0});
                times.add(new int[]{20, 0});
                break;

            case "tds":
                times.add(new int[]{8, 0});
                times.add(new int[]{14, 0});
                times.add(new int[]{20, 0});
                break;

            case "night_only":
                times.add(new int[]{21, 0});
                break;

            case "every_14_days":
                times.add(new int[]{8, 0});
                break;

            default:
                // Fallback: if timingHint available use it, else 8:00
                if (timing != null) times.add(resolveDaily(timing));
                else times.add(new int[]{8, 0});
                break;
        }

        // Apply meal offset
        times = applyMealOffset(times, meal);
        return times;
    }

    private static int[] resolveDaily(String timingHint) {
        if (timingHint == null) return new int[]{8, 0};
        switch (timingHint) {
            case "morning":   return new int[]{8, 0};
            case "afternoon": return new int[]{14, 0};
            case "evening":   return new int[]{20, 0};
            case "night":     return new int[]{21, 0};
            default:          return new int[]{8, 0};
        }
    }

    private static List<int[]> applyMealOffset(List<int[]> times, String meal) {
        if (meal == null) return times;
        List<int[]> adjusted = new ArrayList<>();
        for (int[] time : times) {
            int totalMinutes = time[0] * 60 + time[1];
            if ("before_meal".equals(meal)) {
                totalMinutes -= 30;
            } else if ("after_meal".equals(meal)) {
                totalMinutes += 30;
            }
            // Clamp to valid day range
            if (totalMinutes < 0)    totalMinutes = 0;
            if (totalMinutes > 1439) totalMinutes = 1439;
            adjusted.add(new int[]{totalMinutes / 60, totalMinutes % 60});
        }
        return adjusted;
    }

    /** How many days between repeats. 0 = every_14_days treated specially. */
    public static int resolveRepeatDays(String frequencyType) {
        if ("every_14_days".equals(frequencyType)) return 14;
        return 1; // daily repeat for all others
    }

    // ─── SCHEDULING ──────────────────────────────────────────────────────────

    /** Schedule a single Alarm with AlarmManager. */
    public static void scheduleAlarm(Context context, Alarm alarm) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_ALARM_TRIGGER);
        intent.putExtra(EXTRA_ALARM_ID,       alarm.getId());
        intent.putExtra(EXTRA_MEDICINE_NAME,  alarm.getMedicineName());
        intent.putExtra(EXTRA_MEDICINE_TYPE,  alarm.getMedicineType());
        intent.putExtra(EXTRA_STRENGTH,       alarm.getStrength());
        intent.putExtra(EXTRA_DOSE_QUANTITY,  alarm.getDoseQuantity());
        intent.putExtra(EXTRA_MEAL_RELATION,  alarm.getMealRelation());
        intent.putExtra(EXTRA_SPECIAL_INSTR,  alarm.getSpecialInstruction());

        // Use alarm ID as request code so each alarm is unique
        int requestCode = (int) alarm.getId();
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, alarm.getAlarmHour());
        cal.set(Calendar.MINUTE, alarm.getAlarmMinute());
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // If the time has passed today, schedule for tomorrow (or + repeatDays)
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, alarm.getRepeatEveryDays());
        }

        // AFTER:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) {
                // Exact alarms not permitted — fall back to inexact or prompt user
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                return;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        }
    }

    /** Cancel a scheduled alarm. */
    public static void cancelAlarm(Context context, long alarmId) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(ACTION_ALARM_TRIGGER);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                (int) alarmId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        am.cancel(pi);
    }

    /** Re-schedule alarm for the next interval (called after it fires). */
    public static void rescheduleNextOccurrence(Context context, Alarm alarm) {
        if (!alarm.isEnabled()) return;
        scheduleAlarm(context, alarm);
    }
}
