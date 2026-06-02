package com.mediremind.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Prescription implements Serializable {

    private long id;
    private String patientName;
    private String doctorName;
    private String date;
    private String imagePath;
    private String followUp;
    private String createdAt;
    private List<Medicine> medicines;

    public Prescription() {
        medicines = new ArrayList<>();
    }

    public Prescription(String patientName, String doctorName, String date,
                        String imagePath, String followUp) {
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.date = date;
        this.imagePath = imagePath;
        this.followUp = followUp;
        this.medicines = new ArrayList<>();
    }

    // Getters & Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getFollowUp() { return followUp; }
    public void setFollowUp(String followUp) { this.followUp = followUp; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public List<Medicine> getMedicines() { return medicines; }
    public void setMedicines(List<Medicine> medicines) { this.medicines = medicines; }

    public void addMedicine(Medicine medicine) {
        medicines.add(medicine);
    }
}
