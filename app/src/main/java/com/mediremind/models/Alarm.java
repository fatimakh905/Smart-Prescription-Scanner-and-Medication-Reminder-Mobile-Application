package com.mediremind.models;

import java.io.Serializable;

public class Alarm implements Serializable {

    private long id;
    private long medicineId;
    private String medicineName;
    private String medicineType;
    private String strength;
    private int doseQuantity;
    private String specialInstruction;
    private String mealRelation;
    private int alarmHour;         // 24h format stored in DB
    private int alarmMinute;
    private int repeatEveryDays;
    private boolean isEnabled;
    private String lastTriggered;

    public Alarm() {}

    public Alarm(long medicineId, String medicineName, String medicineType,
                 String strength, int doseQuantity, String specialInstruction,
                 String mealRelation, int alarmHour, int alarmMinute, int repeatEveryDays) {
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.medicineType = medicineType;
        this.strength = strength;
        this.doseQuantity = doseQuantity;
        this.specialInstruction = specialInstruction;
        this.mealRelation = mealRelation;
        this.alarmHour = alarmHour;
        this.alarmMinute = alarmMinute;
        this.repeatEveryDays = repeatEveryDays;
        this.isEnabled = true;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getMedicineId() { return medicineId; }
    public void setMedicineId(long medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getMedicineType() { return medicineType; }
    public void setMedicineType(String medicineType) { this.medicineType = medicineType; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public int getDoseQuantity() { return doseQuantity; }
    public void setDoseQuantity(int doseQuantity) { this.doseQuantity = doseQuantity; }

    public String getSpecialInstruction() { return specialInstruction; }
    public void setSpecialInstruction(String specialInstruction) { this.specialInstruction = specialInstruction; }

    public String getMealRelation() { return mealRelation; }
    public void setMealRelation(String mealRelation) { this.mealRelation = mealRelation; }

    public int getAlarmHour() { return alarmHour; }
    public void setAlarmHour(int alarmHour) { this.alarmHour = alarmHour; }

    public int getAlarmMinute() { return alarmMinute; }
    public void setAlarmMinute(int alarmMinute) { this.alarmMinute = alarmMinute; }

    public int getRepeatEveryDays() { return repeatEveryDays; }
    public void setRepeatEveryDays(int repeatEveryDays) { this.repeatEveryDays = repeatEveryDays; }

    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { isEnabled = enabled; }

    public String getLastTriggered() { return lastTriggered; }
    public void setLastTriggered(String lastTriggered) { this.lastTriggered = lastTriggered; }

    /**
     * Returns time in 12-hour AM/PM format, e.g. "08:00 AM", "09:30 PM"
     */
    public String getFormattedTime() {
        int h = alarmHour;
        String amPm = h >= 12 ? "PM" : "AM";
        if (h == 0) h = 12;
        else if (h > 12) h -= 12;
        return String.format("%02d:%02d %s", h, alarmMinute, amPm);
    }

    /**
     * Returns time in 24-hour format for DB/scheduling use, e.g. "08:00"
     */
    public String get24HTime() {
        return String.format("%02d:%02d", alarmHour, alarmMinute);
    }

    /** Meal relation label */
    public String getMealRelationLabel() {
        if (mealRelation == null) return "";
        switch (mealRelation) {
            case "before_meal":   return "Before meal";
            case "after_meal":    return "After meal";
            case "empty_stomach": return "Empty stomach";
            default:              return "";
        }
    }
}
