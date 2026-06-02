package com.mediremind.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mediremind.R;
import com.mediremind.adapters.MedicineVerificationAdapter;
import com.mediremind.models.Medicine;
import com.mediremind.models.Prescription;

import java.util.List;

public class MedicineVerificationActivity extends AppCompatActivity {

    private RecyclerView rvMedicines;
    private MedicineVerificationAdapter adapter;
    private TextView tvFollowUp, tvWarningCount;
    private Prescription prescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medicine_verification);

        // AFTER:
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            prescription = getIntent().getSerializableExtra("prescription", Prescription.class);
        } else {
            prescription = (Prescription) getIntent().getSerializableExtra("prescription");
        }
        if (prescription == null) {
            finish();
            return;
        }

        setupViews();
        loadMedicines();
    }

    private void setupViews() {
        rvMedicines    = findViewById(R.id.rvMedicines);
        tvFollowUp     = findViewById(R.id.tvFollowUp);
        tvWarningCount = findViewById(R.id.tvWarningCount);

        rvMedicines.setLayoutManager(new LinearLayoutManager(this));

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Confirm all & Continue
        findViewById(R.id.btnConfirm).setOnClickListener(v -> confirmAndContinue());


    }

    private void loadMedicines() {
        List<Medicine> medicines = prescription.getMedicines();

        // Count how many need attention
        long warningCount = 0;
        for (Medicine m : medicines) {
            if (m.isNeedsUserInput() || m.getConfidence() < 0.5) warningCount++;
        }

        if (warningCount > 0) {
            tvWarningCount.setVisibility(View.VISIBLE);
            tvWarningCount.setText("⚠️ " + warningCount + " Medicine(s) need your review");
        } else {
            tvWarningCount.setVisibility(View.GONE);
        }

        adapter = new MedicineVerificationAdapter(this, medicines);
        rvMedicines.setAdapter(adapter);
    }

    private void confirmAndContinue() {
        List<Medicine> medicines = adapter.getMedicines();

        // Validate — ensure all needsUserInput medicines have been addressed
        for (Medicine m : medicines) {
            if (m.isNeedsUserInput()) {
                if (m.getFrequencyType() == null || m.getFrequencyType().isEmpty()) {
                    Toast.makeText(this,
                            "Please set frequency for: " + m.getMedicineName(),
                            Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        // Update medicines in prescription
        prescription.setMedicines(medicines);

        // Navigate to alarm generation
        Intent intent = new Intent(this, AlarmGenerationActivity.class);
        intent.putExtra("prescription", prescription);
        startActivity(intent);
    }
}
