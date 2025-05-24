package com.petdoc.login;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import com.petdoc.main.MainActivity;
import com.petdoc.R;

public class SplashActivity extends AppCompatActivity {
    private static final int SPLASH_DELAY = 1000; // 1.5초 (취향에 따라 조정)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> checkLoginState(), SPLASH_DELAY);
    }

    private void checkLoginState() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // 로그인 X → 로그인 화면
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            finish();
        } else {
            String uid = user.getUid();
            // 이름이 있으면 바로 메인, 없으면 이름 입력 화면
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean hasName = false;
                    for (DataSnapshot pet : snapshot.getChildren()) {
                        if (pet.child("기본정보").child("이름").exists()) {
                            hasName = true;
                            break;
                        }
                    }
                    if (hasName) {
                        // 기존 반려견 이름 있으면 메인
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    } else {
                        // 이름 입력해야 함
                        startActivity(new Intent(SplashActivity.this, NameInputActivity.class));
                    }
                    finish();
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // 에러 → 로그인 화면으로 fallback
                    startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                    finish();
                }
            });
        }
    }
}
