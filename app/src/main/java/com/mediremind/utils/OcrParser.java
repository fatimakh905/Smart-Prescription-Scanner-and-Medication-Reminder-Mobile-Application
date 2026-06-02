package com.mediremind.utils;

import com.mediremind.models.Medicine;
import com.mediremind.models.Prescription;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Parses the structured JSON output from the OCR + LLM pipeline
 * into Prescription and Medicine domain objects.
 */
public class OcrParser {

    /**
     * Parse the full JSON string (as produced by PaddleOCR + Qwen pipeline).
     * Returns a Prescription with its medicines list populated.
     */
    public static Prescription parse(String json) throws JSONException {
        JSONObject root = new JSONObject(json);

        Prescription prescription = new Prescription();
        prescription.setDate(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(new Date()));
        prescription.setPatientName("Patient");   // filled by user later
        prescription.setDoctorName("Doctor");

        // follow_up
        String followUp = root.optString("follow_up", null);
        prescription.setFollowUp(followUp);

        // medicines array
        JSONArray medicines = root.optJSONArray("medicines");
        if (medicines == null) return prescription;

        for (int i = 0; i < medicines.length(); i++) {
            JSONObject m = medicines.getJSONObject(i);
            Medicine medicine = parseMedicine(m);
            prescription.addMedicine(medicine);
        }

        return prescription;
    }

    private static Medicine parseMedicine(JSONObject m) throws JSONException {
        Medicine med = new Medicine();

        med.setMedicineName(m.optString("medicine_name", "Unknown"));
        med.setMedicineType(m.optString("medicine_type", null));
        med.setStrength(m.optString("strength", ""));

        // dose_quantity can be null in JSON
        if (!m.isNull("dose_quantity")) {
            med.setDoseQuantity(m.optInt("dose_quantity", 1));
        } else {
            med.setDoseQuantity(1);
        }

        med.setFrequencyType(nullableString(m, "frequency_type"));
        med.setTimingHint(nullableString(m, "timing_hint"));
        med.setMealRelation(nullableString(m, "meal_relation"));
        med.setSpecialInstruction(nullableString(m, "special_instruction"));

        // duration_days can be null
        if (!m.isNull("duration_days")) {
            String rawDur = String.valueOf(m.optInt("duration_days", 0));
            med.setDurationDays("0".equals(rawDur) ? null : rawDur);
        } else {
            med.setDurationDays(null);
        }

        med.setStatDose(m.optBoolean("stat_dose", false));
        med.setNeedsUserInput(m.optBoolean("needs_user_input", false));
        med.setConfidence(m.optDouble("confidence", 1.0));

        // Auto-set needs_user_input based on confidence and missing fields
        if (med.getConfidence() < 0.5 ||
                med.getFrequencyType() == null ||
                (med.getTimingHint() == null && !"sos".equals(med.getFrequencyType()))) {
            med.setNeedsUserInput(true);
        }

        return med;
    }

    private static String nullableString(JSONObject obj, String key) throws JSONException {
        if (obj.isNull(key)) return null;
        String val = obj.optString(key, null);
        if (val != null && val.equals("null")) return null;
        return val;
    }
}
