package com.mediremind.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mediremind.models.Alarm;
import com.mediremind.models.AlarmLog;
import com.mediremind.models.Medicine;
import com.mediremind.models.Prescription;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "mediremind.db";
    private static final int DB_VERSION = 1;

    // Table names
    public static final String TABLE_PRESCRIPTIONS = "prescriptions";
    public static final String TABLE_MEDICINES      = "medicines";
    public static final String TABLE_ALARMS         = "alarms";
    public static final String TABLE_ALARM_LOGS     = "alarm_logs";

    // prescriptions columns
    private static final String COL_ID           = "id";
    private static final String COL_PATIENT_NAME = "patient_name";
    private static final String COL_DOCTOR_NAME  = "doctor_name";
    private static final String COL_DATE         = "date";
    private static final String COL_IMAGE_PATH   = "image_path";
    private static final String COL_FOLLOW_UP    = "follow_up";
    private static final String COL_CREATED_AT   = "created_at";

    // medicines columns
    private static final String COL_PRESCRIPTION_ID   = "prescription_id";
    private static final String COL_MEDICINE_NAME     = "medicine_name";
    private static final String COL_MEDICINE_TYPE     = "medicine_type";
    private static final String COL_STRENGTH          = "strength";
    private static final String COL_DOSE_QUANTITY     = "dose_quantity";
    private static final String COL_FREQUENCY_TYPE    = "frequency_type";
    private static final String COL_TIMING_HINT       = "timing_hint";
    private static final String COL_MEAL_RELATION     = "meal_relation";
    private static final String COL_SPECIAL_INSTR     = "special_instruction";
    private static final String COL_DURATION_DAYS     = "duration_days";
    private static final String COL_STAT_DOSE         = "stat_dose";
    private static final String COL_NEEDS_USER_INPUT  = "needs_user_input";
    private static final String COL_CONFIDENCE        = "confidence";
    private static final String COL_IS_ACTIVE         = "is_active";

    // alarms columns
    private static final String COL_MEDICINE_ID       = "medicine_id";
    private static final String COL_ALARM_HOUR        = "alarm_hour";
    private static final String COL_ALARM_MINUTE      = "alarm_minute";
    private static final String COL_REPEAT_EVERY_DAYS = "repeat_every_days";
    private static final String COL_IS_ENABLED        = "is_enabled";
    private static final String COL_LAST_TRIGGERED    = "last_triggered";

    // alarm_logs columns
    private static final String COL_ALARM_ID          = "alarm_id";
    private static final String COL_SCHEDULED_TIME    = "scheduled_time";
    private static final String COL_TAKEN_AT          = "taken_at";
    private static final String COL_WAS_TAKEN         = "was_taken";
    private static final String COL_SKIPPED           = "skipped";

    // Singleton
    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // prescriptions
        db.execSQL("CREATE TABLE " + TABLE_PRESCRIPTIONS + " (" +
                COL_ID           + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PATIENT_NAME + " TEXT, " +
                COL_DOCTOR_NAME  + " TEXT, " +
                COL_DATE         + " TEXT, " +
                COL_IMAGE_PATH   + " TEXT, " +
                COL_FOLLOW_UP    + " TEXT, " +
                COL_CREATED_AT   + " TEXT" +
                ")");

        // medicines
        db.execSQL("CREATE TABLE " + TABLE_MEDICINES + " (" +
                COL_ID              + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PRESCRIPTION_ID + " INTEGER, " +
                COL_MEDICINE_NAME   + " TEXT, " +
                COL_MEDICINE_TYPE   + " TEXT, " +
                COL_STRENGTH        + " TEXT, " +
                COL_DOSE_QUANTITY   + " INTEGER, " +
                COL_FREQUENCY_TYPE  + " TEXT, " +
                COL_TIMING_HINT     + " TEXT, " +
                COL_MEAL_RELATION   + " TEXT, " +
                COL_SPECIAL_INSTR   + " TEXT, " +
                COL_DURATION_DAYS   + " TEXT, " +
                COL_STAT_DOSE       + " INTEGER DEFAULT 0, " +
                COL_NEEDS_USER_INPUT+ " INTEGER DEFAULT 0, " +
                COL_CONFIDENCE      + " REAL DEFAULT 1.0, " +
                COL_IS_ACTIVE       + " INTEGER DEFAULT 1, " +
                "FOREIGN KEY(" + COL_PRESCRIPTION_ID + ") REFERENCES " + TABLE_PRESCRIPTIONS + "(id)" +
                ")");

        // alarms
        db.execSQL("CREATE TABLE " + TABLE_ALARMS + " (" +
                COL_ID              + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_MEDICINE_ID     + " INTEGER, " +
                COL_MEDICINE_NAME   + " TEXT, " +
                COL_MEDICINE_TYPE   + " TEXT, " +
                COL_STRENGTH        + " TEXT, " +
                COL_DOSE_QUANTITY   + " INTEGER, " +
                COL_SPECIAL_INSTR   + " TEXT, " +
                COL_MEAL_RELATION   + " TEXT, " +
                COL_ALARM_HOUR      + " INTEGER, " +
                COL_ALARM_MINUTE    + " INTEGER, " +
                COL_REPEAT_EVERY_DAYS + " INTEGER DEFAULT 1, " +
                COL_IS_ENABLED      + " INTEGER DEFAULT 1, " +
                COL_LAST_TRIGGERED  + " TEXT, " +
                "FOREIGN KEY(" + COL_MEDICINE_ID + ") REFERENCES " + TABLE_MEDICINES + "(id)" +
                ")");

        // alarm_logs
        db.execSQL("CREATE TABLE " + TABLE_ALARM_LOGS + " (" +
                COL_ID             + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ALARM_ID       + " INTEGER, " +
                COL_MEDICINE_ID    + " INTEGER, " +
                COL_MEDICINE_NAME  + " TEXT, " +
                COL_SCHEDULED_TIME + " TEXT, " +
                COL_TAKEN_AT       + " TEXT, " +
                COL_WAS_TAKEN      + " INTEGER DEFAULT 0, " +
                COL_SKIPPED        + " INTEGER DEFAULT 0" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALARM_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ALARMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDICINES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRESCRIPTIONS);
        onCreate(db);
    }

    // ─────────────────────────────────────────────────────────────────
    //  PRESCRIPTIONS
    // ─────────────────────────────────────────────────────────────────

    public long insertPrescription(Prescription p) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PATIENT_NAME, p.getPatientName());
        cv.put(COL_DOCTOR_NAME, p.getDoctorName());
        cv.put(COL_DATE, p.getDate());
        cv.put(COL_IMAGE_PATH, p.getImagePath());
        cv.put(COL_FOLLOW_UP, p.getFollowUp());
        cv.put(COL_CREATED_AT, nowTimestamp());
        return db.insert(TABLE_PRESCRIPTIONS, null, cv);
    }

    public List<Prescription> getAllPrescriptions() {
        List<Prescription> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_PRESCRIPTIONS, null, null, null, null, null,
                COL_CREATED_AT + " DESC");
        while (c.moveToNext()) {
            list.add(cursorToPrescription(c));
        }
        c.close();
        return list;
    }

    private Prescription cursorToPrescription(Cursor c) {
        Prescription p = new Prescription();
        p.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
        p.setPatientName(c.getString(c.getColumnIndexOrThrow(COL_PATIENT_NAME)));
        p.setDoctorName(c.getString(c.getColumnIndexOrThrow(COL_DOCTOR_NAME)));
        p.setDate(c.getString(c.getColumnIndexOrThrow(COL_DATE)));
        p.setImagePath(c.getString(c.getColumnIndexOrThrow(COL_IMAGE_PATH)));
        p.setFollowUp(c.getString(c.getColumnIndexOrThrow(COL_FOLLOW_UP)));
        p.setCreatedAt(c.getString(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        return p;
    }

    // ─────────────────────────────────────────────────────────────────
    //  MEDICINES
    // ─────────────────────────────────────────────────────────────────

    public long insertMedicine(Medicine m) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_PRESCRIPTION_ID,  m.getPrescriptionId());
        cv.put(COL_MEDICINE_NAME,    m.getMedicineName());
        cv.put(COL_MEDICINE_TYPE,    m.getMedicineType());
        cv.put(COL_STRENGTH,         m.getStrength());
        cv.put(COL_DOSE_QUANTITY,    m.getDoseQuantity());
        cv.put(COL_FREQUENCY_TYPE,   m.getFrequencyType());
        cv.put(COL_TIMING_HINT,      m.getTimingHint());
        cv.put(COL_MEAL_RELATION,    m.getMealRelation());
        cv.put(COL_SPECIAL_INSTR,    m.getSpecialInstruction());
        cv.put(COL_DURATION_DAYS,    m.getDurationDays());
        cv.put(COL_STAT_DOSE,        m.isStatDose() ? 1 : 0);
        cv.put(COL_NEEDS_USER_INPUT, m.isNeedsUserInput() ? 1 : 0);
        cv.put(COL_CONFIDENCE,       m.getConfidence());
        cv.put(COL_IS_ACTIVE,        1);
        return db.insert(TABLE_MEDICINES, null, cv);
    }

    public void updateMedicine(Medicine m) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_MEDICINE_NAME,    m.getMedicineName());
        cv.put(COL_MEDICINE_TYPE,    m.getMedicineType());
        cv.put(COL_STRENGTH,         m.getStrength());
        cv.put(COL_DOSE_QUANTITY,    m.getDoseQuantity());
        cv.put(COL_FREQUENCY_TYPE,   m.getFrequencyType());
        cv.put(COL_TIMING_HINT,      m.getTimingHint());
        cv.put(COL_MEAL_RELATION,    m.getMealRelation());
        cv.put(COL_SPECIAL_INSTR,    m.getSpecialInstruction());
        cv.put(COL_DURATION_DAYS,    m.getDurationDays());
        cv.put(COL_NEEDS_USER_INPUT, m.isNeedsUserInput() ? 1 : 0);
        db.update(TABLE_MEDICINES, cv, COL_ID + "=?",
                new String[]{String.valueOf(m.getId())});
    }

    public List<Medicine> getMedicinesForPrescription(long prescriptionId) {
        List<Medicine> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_MEDICINES, null,
                COL_PRESCRIPTION_ID + "=?",
                new String[]{String.valueOf(prescriptionId)},
                null, null, null);
        while (c.moveToNext()) list.add(cursorToMedicine(c));
        c.close();
        return list;
    }

    public List<Medicine> getAllActiveMedicines() {
        List<Medicine> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_MEDICINES, null,
                COL_IS_ACTIVE + "=1", null, null, null, null);
        while (c.moveToNext()) list.add(cursorToMedicine(c));
        c.close();
        return list;
    }

    private Medicine cursorToMedicine(Cursor c) {
        Medicine m = new Medicine();
        m.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
        m.setPrescriptionId(c.getLong(c.getColumnIndexOrThrow(COL_PRESCRIPTION_ID)));
        m.setMedicineName(c.getString(c.getColumnIndexOrThrow(COL_MEDICINE_NAME)));
        m.setMedicineType(c.getString(c.getColumnIndexOrThrow(COL_MEDICINE_TYPE)));
        m.setStrength(c.getString(c.getColumnIndexOrThrow(COL_STRENGTH)));
        m.setDoseQuantity(c.getInt(c.getColumnIndexOrThrow(COL_DOSE_QUANTITY)));
        m.setFrequencyType(c.getString(c.getColumnIndexOrThrow(COL_FREQUENCY_TYPE)));
        m.setTimingHint(c.getString(c.getColumnIndexOrThrow(COL_TIMING_HINT)));
        m.setMealRelation(c.getString(c.getColumnIndexOrThrow(COL_MEAL_RELATION)));
        m.setSpecialInstruction(c.getString(c.getColumnIndexOrThrow(COL_SPECIAL_INSTR)));
        m.setDurationDays(c.getString(c.getColumnIndexOrThrow(COL_DURATION_DAYS)));
        m.setStatDose(c.getInt(c.getColumnIndexOrThrow(COL_STAT_DOSE)) == 1);
        m.setNeedsUserInput(c.getInt(c.getColumnIndexOrThrow(COL_NEEDS_USER_INPUT)) == 1);
        m.setConfidence(c.getDouble(c.getColumnIndexOrThrow(COL_CONFIDENCE)));
        m.setActive(c.getInt(c.getColumnIndexOrThrow(COL_IS_ACTIVE)) == 1);
        return m;
    }

    // ─────────────────────────────────────────────────────────────────
    //  ALARMS
    // ─────────────────────────────────────────────────────────────────

    public long insertAlarm(Alarm a) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_MEDICINE_ID,      a.getMedicineId());
        cv.put(COL_MEDICINE_NAME,    a.getMedicineName());
        cv.put(COL_MEDICINE_TYPE,    a.getMedicineType());
        cv.put(COL_STRENGTH,         a.getStrength());
        cv.put(COL_DOSE_QUANTITY,    a.getDoseQuantity());
        cv.put(COL_SPECIAL_INSTR,    a.getSpecialInstruction());
        cv.put(COL_MEAL_RELATION,    a.getMealRelation());
        cv.put(COL_ALARM_HOUR,       a.getAlarmHour());
        cv.put(COL_ALARM_MINUTE,     a.getAlarmMinute());
        cv.put(COL_REPEAT_EVERY_DAYS,a.getRepeatEveryDays());
        cv.put(COL_IS_ENABLED,       a.isEnabled() ? 1 : 0);
        return db.insert(TABLE_ALARMS, null, cv);
    }

    public List<Alarm> getAllEnabledAlarms() {
        List<Alarm> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_ALARMS, null,
                COL_IS_ENABLED + "=1", null, null, null,
                COL_ALARM_HOUR + " ASC, " + COL_ALARM_MINUTE + " ASC");
        while (c.moveToNext()) list.add(cursorToAlarm(c));
        c.close();
        return list;
    }

    public Alarm getAlarmById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_ALARMS, null,
                COL_ID + "=?", new String[]{String.valueOf(id)}, null, null, null);
        Alarm a = null;
        if (c.moveToFirst()) a = cursorToAlarm(c);
        c.close();
        return a;
    }

    public void setAlarmEnabled(long alarmId, boolean enabled) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_IS_ENABLED, enabled ? 1 : 0);
        db.update(TABLE_ALARMS, cv, COL_ID + "=?",
                new String[]{String.valueOf(alarmId)});
    }

    public void updateAlarmTime(long alarmId, int hour, int minute) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ALARM_HOUR, hour);
        cv.put(COL_ALARM_MINUTE, minute);
        db.update(TABLE_ALARMS, cv, COL_ID + "=?",
                new String[]{String.valueOf(alarmId)});
    }

    public void updateAlarmLastTriggered(long alarmId, String timestamp) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_LAST_TRIGGERED, timestamp);
        db.update(TABLE_ALARMS, cv, COL_ID + "=?",
                new String[]{String.valueOf(alarmId)});
    }

    public void deleteAlarmsForMedicine(long medicineId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_ALARMS, COL_MEDICINE_ID + "=?",
                new String[]{String.valueOf(medicineId)});
    }

    private Alarm cursorToAlarm(Cursor c) {
        Alarm a = new Alarm();
        a.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
        a.setMedicineId(c.getLong(c.getColumnIndexOrThrow(COL_MEDICINE_ID)));
        a.setMedicineName(c.getString(c.getColumnIndexOrThrow(COL_MEDICINE_NAME)));
        a.setMedicineType(c.getString(c.getColumnIndexOrThrow(COL_MEDICINE_TYPE)));
        a.setStrength(c.getString(c.getColumnIndexOrThrow(COL_STRENGTH)));
        a.setDoseQuantity(c.getInt(c.getColumnIndexOrThrow(COL_DOSE_QUANTITY)));
        a.setSpecialInstruction(c.getString(c.getColumnIndexOrThrow(COL_SPECIAL_INSTR)));
        a.setMealRelation(c.getString(c.getColumnIndexOrThrow(COL_MEAL_RELATION)));
        a.setAlarmHour(c.getInt(c.getColumnIndexOrThrow(COL_ALARM_HOUR)));
        a.setAlarmMinute(c.getInt(c.getColumnIndexOrThrow(COL_ALARM_MINUTE)));
        a.setRepeatEveryDays(c.getInt(c.getColumnIndexOrThrow(COL_REPEAT_EVERY_DAYS)));
        a.setEnabled(c.getInt(c.getColumnIndexOrThrow(COL_IS_ENABLED)) == 1);
        a.setLastTriggered(c.getString(c.getColumnIndexOrThrow(COL_LAST_TRIGGERED)));
        return a;
    }

    // ─────────────────────────────────────────────────────────────────
    //  ALARM LOGS
    // ─────────────────────────────────────────────────────────────────

    public long insertAlarmLog(AlarmLog log) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_ALARM_ID,       log.getAlarmId());
        cv.put(COL_MEDICINE_ID,    log.getMedicineId());
        cv.put(COL_MEDICINE_NAME,  log.getMedicineName());
        cv.put(COL_SCHEDULED_TIME, log.getScheduledTime());
        cv.put(COL_WAS_TAKEN,      0);
        cv.put(COL_SKIPPED,        0);
        return db.insert(TABLE_ALARM_LOGS, null, cv);
    }

    public void markLogTaken(long logId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_WAS_TAKEN, 1);
        cv.put(COL_TAKEN_AT, nowTimestamp());
        db.update(TABLE_ALARM_LOGS, cv, COL_ID + "=?",
                new String[]{String.valueOf(logId)});
    }

    public void markLogSkipped(long logId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_SKIPPED, 1);
        db.update(TABLE_ALARM_LOGS, cv, COL_ID + "=?",
                new String[]{String.valueOf(logId)});
    }

    public List<AlarmLog> getTodayLogs() {
        List<AlarmLog> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date());
        Cursor c = db.query(TABLE_ALARM_LOGS, null,
                COL_SCHEDULED_TIME + " LIKE ?",
                new String[]{today + "%"},
                null, null, COL_SCHEDULED_TIME + " ASC");
        while (c.moveToNext()) list.add(cursorToLog(c));
        c.close();
        return list;
    }

    public AlarmLog getLatestPendingLog(long alarmId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_ALARM_LOGS, null,
                COL_ALARM_ID + "=? AND " + COL_WAS_TAKEN + "=0 AND " + COL_SKIPPED + "=0",
                new String[]{String.valueOf(alarmId)},
                null, null, COL_ID + " DESC", "1");
        AlarmLog log = null;
        if (c.moveToFirst()) log = cursorToLog(c);
        c.close();
        return log;
    }

    private AlarmLog cursorToLog(Cursor c) {
        AlarmLog log = new AlarmLog();
        log.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
        log.setAlarmId(c.getLong(c.getColumnIndexOrThrow(COL_ALARM_ID)));
        log.setMedicineId(c.getLong(c.getColumnIndexOrThrow(COL_MEDICINE_ID)));
        log.setMedicineName(c.getString(c.getColumnIndexOrThrow(COL_MEDICINE_NAME)));
        log.setScheduledTime(c.getString(c.getColumnIndexOrThrow(COL_SCHEDULED_TIME)));
        log.setTakenAt(c.getString(c.getColumnIndexOrThrow(COL_TAKEN_AT)));
        log.setWasTaken(c.getInt(c.getColumnIndexOrThrow(COL_WAS_TAKEN)) == 1);
        log.setSkipped(c.getInt(c.getColumnIndexOrThrow(COL_SKIPPED)) == 1);
        return log;
    }

    // ─────────────────────────────────────────────────────────────────
    //  HELPER
    // ─────────────────────────────────────────────────────────────────

    private String nowTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
    }
}
