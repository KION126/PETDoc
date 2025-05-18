package com.petdoc.login;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 3초 후 LoginActivity로 전환
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish(); // SplashActivity 종료
        }, 3000);
    }
}
