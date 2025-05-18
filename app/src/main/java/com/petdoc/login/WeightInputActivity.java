package com.petdoc.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;

public class WeightInputActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_weight);

        ImageButton btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> {
            startActivity(new Intent(this, PhotoInputActivity.class));
            finish();
        });
    }
}
