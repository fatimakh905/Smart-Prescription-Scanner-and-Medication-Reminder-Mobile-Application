package com.mediremind.activities;

import android.Manifest;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.mediremind.R;
import com.mediremind.models.Prescription;
import com.mediremind.utils.ImagePickerHelper;
import com.mediremind.utils.OcrParser;
import com.mediremind.utils.OcrPipelineClient;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import androidx.annotation.NonNull;

public class PrescriptionProcessingActivity extends AppCompatActivity {

    private EditText etPatientName, etDoctorName;
    private ImageView ivPreview;
    private TextView tvImageHint, tvFollowUp, tvStatus;
    private Button btnSelectImage, btnProcess;
    private LinearLayout layoutImageSection;

    private File selectedImageFile;
    private ImagePickerHelper pickerHelper;
    private androidx.appcompat.app.AlertDialog progressDialog;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    private static final int REQUEST_CAMERA_PERMISSION = 301;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prescription_processing);

        // In onCreate(), after super.onCreate():
        cameraLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        selectedImageFile = pickerHelper.getCapturedFile();
                        displaySelectedImage();
                    }
                });

        galleryLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            try {
                                selectedImageFile = ImagePickerHelper.resolveGalleryUri(this, uri);
                                displaySelectedImage();
                            } catch (IOException e) {
                                Toast.makeText(this, "Could not load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
        pickerHelper = new ImagePickerHelper();
        bindViews();
    }

    private void bindViews() {
        etPatientName    = findViewById(R.id.etPatientName);
        etDoctorName     = findViewById(R.id.etDoctorName);
        ivPreview        = findViewById(R.id.ivPreview);
        tvImageHint      = findViewById(R.id.tvImageHint);
        tvFollowUp       = findViewById(R.id.tvFollowUp);
        tvStatus         = findViewById(R.id.tvStatus);
        btnSelectImage   = findViewById(R.id.btnSelectImage);
        btnProcess       = findViewById(R.id.btnProcess);
        layoutImageSection = findViewById(R.id.layoutImageSection);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnSelectImage.setOnClickListener(v -> showImageSourceDialog());

        btnProcess.setOnClickListener(v -> {
            if (validateInput()) runPipeline();
        });
    }

    // ── Image source dialog ────────────────────────────────────────────────────

    private void showImageSourceDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Select Prescription Image")
                .setItems(new String[]{"Take Photo", "Choose from Gallery"}, (dialog, which) -> {
                    if (which == 0) launchCamera();
                    else launchGallery();
                })
                .show();
    }


    private void launchCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        try {
            Intent intent = pickerHelper.getCameraIntent(this);
            cameraLauncher.launch(intent);
        } catch (IOException e) {
            Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            Toast.makeText(this, "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchGallery() {
        galleryLauncher.launch(pickerHelper.getGalleryIntent());
    }

    private void displaySelectedImage() {
        if (selectedImageFile != null && selectedImageFile.exists()) {
            ivPreview.setVisibility(View.VISIBLE);
            tvImageHint.setVisibility(View.GONE);
            Glide.with(this).load(selectedImageFile).into(ivPreview);
            btnProcess.setEnabled(true);
        }
    }


    // ── Validation ─────────────────────────────────────────────────────────────

    private boolean validateInput() {
        String name = etPatientName.getText().toString().trim();
        if (name.isEmpty()) {
            etPatientName.setError("Patient name is required");
            etPatientName.requestFocus();
            return false;
        }
        if (selectedImageFile == null || !selectedImageFile.exists()) {
            Toast.makeText(this,
                    "Please select a prescription image first", Toast.LENGTH_SHORT).show();
            return false;
        }
        long sizeMB = selectedImageFile.length() / (1024 * 1024);
        if (sizeMB > 10) {
            Toast.makeText(this,
                    "Image too large (" + sizeMB + " MB). Please use an image under 10 MB.",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    // ── Real OCR pipeline ──────────────────────────────────────────────────────

    private void runPipeline() {
        showProgress("Extracting Medicines may take a while...");
        btnProcess.setEnabled(false);

        String patientName = etPatientName.getText().toString().trim();
        String doctorName  = etDoctorName.getText().toString().trim();
        if (doctorName.isEmpty()) doctorName = "Unknown Doctor";

        final String finalDoctorName = doctorName;

        new OcrPipelineClient().runPipeline(selectedImageFile,
                new OcrPipelineClient.PipelineCallback() {

                    @Override
                    public void onProgress(String message) {
                        updateProgress(message);
                    }

                    @Override
                    public void onSuccess(String structuredJson) {
                        dismissProgress();
                        btnProcess.setEnabled(true);
                        handleSuccess(structuredJson, patientName, finalDoctorName);
                    }

                    @Override
                    public void onError(String error) {
                        dismissProgress();
                        btnProcess.setEnabled(true);
                        showError(error);
                    }
                });
    }

    private void handleSuccess(String json, String patientName, String doctorName) {
        try {
            Prescription prescription = OcrParser.parse(json);
            prescription.setPatientName(patientName);
            prescription.setDoctorName(doctorName);
            prescription.setImagePath(selectedImageFile.getAbsolutePath());

            if (prescription.getMedicines().isEmpty()) {
                Toast.makeText(this,
                        "No medicines were found in the prescription.\n" +
                        "Please ensure the image is clear and contains medicine instructions.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // Show follow-up if found
            if (prescription.getFollowUp() != null && !prescription.getFollowUp().equals("null")) {
                tvFollowUp.setVisibility(View.VISIBLE);
                tvFollowUp.setText("📅 Follow-up noted: " + prescription.getFollowUp());
            }

            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText("✅ Found " + prescription.getMedicines().size()
                    + " medicine(s). Proceeding to verification…");

            // Go to verification screen
            Intent intent = new Intent(this, MedicineVerificationActivity.class);
            intent.putExtra("prescription", prescription);
            startActivity(intent);

        } catch (JSONException e) {
            showError("AI returned invalid data. Please try again.\n\nDetail: " + e.getMessage());
        }
    }

    private void showError(String error) {
        tvStatus.setVisibility(View.VISIBLE);
        tvStatus.setText("❌ " + error);
        new AlertDialog.Builder(this)
                .setTitle("Pipeline Error")
                .setMessage(error)
                .setPositiveButton("Retry", (d, w) -> runPipeline())
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Progress dialog ────────────────────────────────────────────────────────

    private void showProgress(String message) {
        if (progressDialog == null || !progressDialog.isShowing()) {
            android.widget.TextView tv = new android.widget.TextView(this);
            tv.setPadding(60, 40, 60, 20);
            tv.setText(message);
            progressDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setView(tv)
                    .setCancelable(false)
                    .create();
            progressDialog.show();
        } else {
            updateProgress(message);
        }
    }

    private void updateProgress(String message) {
        // update tvStatus only; dialog has no dynamic message
        runOnUiThread(() -> {
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText(message);
        });
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
