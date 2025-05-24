package com.petdoc.aiCheck;

import android.os.Bundle;
import android.content.Intent;
import android.widget.FrameLayout;
import com.petdoc.aiCheck.eye.EyeCamActivity;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;
import com.petdoc.aiCheck.skin.SkinCamActivity;

/**
 * AI 스마트 간편 검진 메인 페이지
 */
public class AICheckActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai_check);

        // 👁 안구 AI 카메라 버튼 → EyeCamActivity
        FrameLayout eyeButton = findViewById(R.id.eye_button);
        eyeButton.setOnClickListener(v -> {
            Intent intent = new Intent(AICheckActivity.this, EyeCamActivity.class);
            startActivity(intent);
        });

        // 🧴 피부 AI 카메라 버튼 → SkinCamActivity
        FrameLayout skinButton = findViewById(R.id.skin_button);
        skinButton.setOnClickListener(v -> {
            Intent intent = new Intent(AICheckActivity.this, SkinCamActivity.class);
            startActivity(intent);
        });
    }
}