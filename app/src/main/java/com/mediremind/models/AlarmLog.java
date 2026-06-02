package com.mediremind.models;

public class AlarmLog {

    private long id;
    private long alarmId;
    private long medicineId;
    private String medicineName;
    private String scheduledTime;
    private String takenAt;
    private boolean wasTaken;
    private boolean skipped;

    public AlarmLog() {}

    public AlarmLog(long alarmId, long medicineId, String medicineName, String scheduledTime) {
        this.alarmId = alarmId;
        this.medicineId = medicineId;
        this.medicineName = medicineName;
        this.scheduledTime = scheduledTime;
        this.wasTaken = false;
        this.skipped = false;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getAlarmId() { return alarmId; }
    public void setAlarmId(long alarmId) { this.alarmId = alarmId; }

    public long getMedicineId() { return medicineId; }
    public void setMedicineId(long medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(String scheduledTime) { this.scheduledTime = scheduledTime; }

    public String getTakenAt() { return takenAt; }
    public void setTakenAt(String takenAt) { this.takenAt = takenAt; }

    public boolean isWasTaken() { return wasTaken; }
    public void setWasTaken(boolean wasTaken) { this.wasTaken = wasTaken; }

    public boolean isSkipped() { return skipped; }
    public void setSkipped(boolean skipped) { this.skipped = skipped; }

    public String getStatusLabel() {
        if (wasTaken) return "Taken";
        if (skipped)  return "Skipped";
        return "Pending";
    }
}
