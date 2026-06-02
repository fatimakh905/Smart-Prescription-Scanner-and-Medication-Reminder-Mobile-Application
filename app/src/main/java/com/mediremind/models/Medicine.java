package com.mediremind.models;

import java.io.Serializable;

public class Medicine implements Serializable {

    private long id;
    private long prescriptionId;
    private String medicineName;
    private String medicineType;    // tablet, capsule, syrup, injection, sachet, rotacap
    private String strength;
    private int doseQuantity;
    private String frequencyType;   // daily, tds, morning_evening, night_only, every_14_days, sos
    private String timingHint;      // morning, evening, night, afternoon, morning_evening, morning_afternoon_evening
    private String mealRelation;    // before_meal, after_meal, empty_stomach
    private String specialInstruction;
    private String durationDays;
    private boolean statDose;
    private boolean needsUserInput;
    private double confidence;
    private boolean isActive;

    // Empty constructor for DB read
    public Medicine() {}

    // Full constructor
    public Medicine(long prescriptionId, String medicineName, String medicineType,
                    String strength, int doseQuantity, String frequencyType,
                    String timingHint, String mealRelation, String specialInstruction,
                    String durationDays, boolean statDose, boolean needsUserInput,
                    double confidence) {
        this.prescriptionId = prescriptionId;
        this.medicineName = medicineName;
        this.medicineType = medicineType;
        this.strength = strength;
        this.doseQuantity = doseQuantity;
        this.frequencyType = frequencyType;
        this.timingHint = timingHint;
        this.mealRelation = mealRelation;
        this.specialInstruction = specialInstruction;
        this.durationDays = durationDays;
        this.statDose = statDose;
        this.needsUserInput = needsUserInput;
        this.confidence = confidence;
        this.isActive = true;
    }

    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(long prescriptionId) { this.prescriptionId = prescriptionId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getMedicineType() { return medicineType; }
    public void setMedicineType(String medicineType) { this.medicineType = medicineType; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public int getDoseQuantity() { return doseQuantity; }
    public void setDoseQuantity(int doseQuantity) { this.doseQuantity = doseQuantity; }

    public String getFrequencyType() { return frequencyType; }
    public void setFrequencyType(String frequencyType) { this.frequencyType = frequencyType; }

    public String getTimingHint() { return timingHint; }
    public void setTimingHint(String timingHint) { this.timingHint = timingHint; }

    public String getMealRelation() { return mealRelation; }
    public void setMealRelation(String mealRelation) { this.mealRelation = mealRelation; }

    public String getSpecialInstruction() { return specialInstruction; }
    public void setSpecialInstruction(String specialInstruction) { this.specialInstruction = specialInstruction; }

    public String getDurationDays() { return durationDays; }
    public void setDurationDays(String durationDays) { this.durationDays = durationDays; }

    public boolean isStatDose() { return statDose; }
    public void setStatDose(boolean statDose) { this.statDose = statDose; }

    public boolean isNeedsUserInput() { return needsUserInput; }
    public void setNeedsUserInput(boolean needsUserInput) { this.needsUserInput = needsUserInput; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    /** Human-readable frequency label */
    public String getFrequencyLabel() {
        if (frequencyType == null) return "Unknown";
        switch (frequencyType) {
            case "daily":            return "Once Daily";
            case "morning_evening":  return "Twice Daily";
            case "tds":              return "3x Daily";
            case "night_only":       return "Night Only";
            case "every_14_days":    return "Every 14 Days";
            case "sos":              return "As Needed (SOS)";
            default:                 return frequencyType;
        }
    }

    /** Icon emoji for medicine type */
    public String getTypeEmoji() {
        if (medicineType == null) return "💊";
        switch (medicineType) {
            case "tablet":    return "💊";
            case "capsule":   return "💊";
            case "syrup":     return "🧴";
            case "injection": return "💉";
            case "sachet":    return "📦";
            case "rotacap":   return "🌬️";
            default:          return "💊";
        }
    }
}
