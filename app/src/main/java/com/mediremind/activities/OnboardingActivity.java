package com.mediremind.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.mediremind.R;

public class OnboardingActivity extends AppCompatActivity {

    private int currentScreen = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.onboarding_screen_1);

        Button btn = findViewById(R.id.btnContinue);

        btn.setOnClickListener(v -> {

            if (currentScreen == 1) {
                currentScreen = 2;
                setContentView(R.layout.onboarding_screen_2);

                Button btn2 = findViewById(R.id.btnContinue);

                btn2.setOnClickListener(v2 -> {
                    startActivity(new Intent(
                            OnboardingActivity.this,
                            HomeActivity.class
                    ));
                    finish();
                });

            }
        });
    }
}