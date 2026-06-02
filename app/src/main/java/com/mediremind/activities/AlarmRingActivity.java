package com.mediremind.activities;

import android.app.KeyguardManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.mediremind.R;
import com.mediremind.database.DatabaseHelper;
import com.mediremind.notifications.NotificationHelper;

public class AlarmRingActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private long alarmId, logId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Block back button so alarm cannot be dismissed accidentally
        getOnBackPressedDispatcher().addCallback(this,
                new androidx.activity.OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() { /* intentionally blocked */ }
                });

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Show on lock screen and turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (km != null) km.requestDismissKeyguard(this, null);
        } else {
            //noinspection deprecation — pre-API-27 fallback flags
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
        }

        setContentView(R.layout.activity_alarm_ring);

        alarmId           = getIntent().getLongExtra("alarm_id", -1);
        logId             = getIntent().getLongExtra("log_id", -1);
        String medicineName      = getIntent().getStringExtra("medicine_name");
        String strength          = getIntent().getStringExtra("strength");
        String mealRelation      = getIntent().getStringExtra("meal_relation");
        String specialInstruction = getIntent().getStringExtra("special_instruction");

        TextView tvMedicineName = findViewById(R.id.tvMedicineName);
        TextView tvStrength     = findViewById(R.id.tvStrength);
        TextView tvMealRelation = findViewById(R.id.tvMealRelation);
        TextView tvInstruction  = findViewById(R.id.tvInstruction);

        if (medicineName != null) tvMedicineName.setText(medicineName);
        if (strength != null && !strength.isEmpty()) tvStrength.setText(strength);

        if (mealRelation != null) {
            switch (mealRelation) {
                case "before_meal":
                    tvMealRelation.setText("⏰ Take 30 min before meal");
                    break;
                case "after_meal":
                    tvMealRelation.setText("⏰ Take 30 min after meal");
                    break;
                case "empty_stomach":
                    tvMealRelation.setText("☕ Take on empty stomach");
                    break;
                default:
                    tvMealRelation.setText("");
                    break;
            }
        }

        if (specialInstruction != null && !specialInstruction.isEmpty()) {
            tvInstruction.setText("ℹ️ " + specialInstruction);
        }

        startAlarmSound();

        findViewById(R.id.btnTaken).setOnClickListener(v -> markTaken());
        findViewById(R.id.btnSnooze).setOnClickListener(v -> snooze());
        findViewById(R.id.btnSkip).setOnClickListener(v -> skip());

        // Auto-snooze after 5 minutes if no user action
        new Handler(Looper.getMainLooper()).postDelayed(this::autoSnooze, 5 * 60 * 1000L);
    }

    private void startAlarmSound() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmUri);
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            // Fallback: vibration-only mode if sound setup fails
        }

        long[] pattern = {0, 500, 200, 500, 200, 500, 1000};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+: use VibratorManager
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vm != null) {
                vibrator = vm.getDefaultVibrator();
            }
        } else {
            //noinspection deprecation — getSystemService(VIBRATOR_SERVICE) is the correct path pre-S
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                //noinspection deprecation — vibrate(long[], int) required pre-O
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void stopAlarmSound() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) { }
            mediaPlayer = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        NotificationHelper.cancelNotification(this, alarmId);
    }

    private void markTaken() {
        stopAlarmSound();
        if (logId != -1) {
            DatabaseHelper.getInstance(this).markLogTaken(logId);
        }
        finish();
    }

    private void snooze() {
        stopAlarmSound();
        com.mediremind.models.Alarm alarm =
                DatabaseHelper.getInstance(this).getAlarmById(alarmId);
        if (alarm != null) {
            android.app.AlarmManager am =
                    (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null) {
                android.content.Intent intent =
                        new android.content.Intent(this, com.mediremind.receivers.AlarmReceiver.class);
                intent.setAction(com.mediremind.utils.AlarmScheduler.ACTION_ALARM_TRIGGER);
                intent.putExtra(com.mediremind.utils.AlarmScheduler.EXTRA_ALARM_ID, alarmId);
                intent.putExtra(com.mediremind.utils.AlarmScheduler.EXTRA_MEDICINE_NAME,
                        alarm.getMedicineName());
                intent.putExtra(com.mediremind.utils.AlarmScheduler.EXTRA_MEDICINE_TYPE,
                        alarm.getMedicineType());
                intent.putExtra(com.mediremind.utils.AlarmScheduler.EXTRA_STRENGTH,
                        alarm.getStrength());
                intent.putExtra(com.mediremind.utils.AlarmScheduler.EXTRA_DOSE_QUANTITY,
                        alarm.getDoseQuantity());
                intent.putExtra(com.mediremind.utils.AlarmScheduler.EXTRA_MEAL_RELATION,
                        alarm.getMealRelation());
                intent.putExtra(com.mediremind.utils.AlarmScheduler.EXTRA_SPECIAL_INSTR,
                        alarm.getSpecialInstruction());

                android.app.PendingIntent pi = android.app.PendingIntent.getBroadcast(
                        this,
                        (int) (alarmId + 9999),
                        intent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                | android.app.PendingIntent.FLAG_IMMUTABLE
                );

                long snoozeTime = System.currentTimeMillis() + 10 * 60 * 1000L;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (am.canScheduleExactAlarms()) {
                        am.setExactAndAllowWhileIdle(
                                android.app.AlarmManager.RTC_WAKEUP, snoozeTime, pi);
                    } else {
                        am.setAndAllowWhileIdle(
                                android.app.AlarmManager.RTC_WAKEUP, snoozeTime, pi);
                    }
                } else {
                    am.setExactAndAllowWhileIdle(
                            android.app.AlarmManager.RTC_WAKEUP, snoozeTime, pi);
                }
            }
        }
        finish();
    }

    private void skip() {
        stopAlarmSound();
        if (logId != -1) {
            DatabaseHelper.getInstance(this).markLogSkipped(logId);
        }
        finish();
    }

    private void autoSnooze() {
        if (!isFinishing()) snooze();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarmSound();
    }
}
