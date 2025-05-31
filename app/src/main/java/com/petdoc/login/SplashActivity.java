package com.petdoc.login;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
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
        EdgeToEdge.enable(this);
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
                    if (snapshot.getChildrenCount() > 0) {
                        // 반려견 노드 중 첫 번째 key를 기본 반려견 ID로 사용
                        String firstPetId = null;
                        for (DataSnapshot petSnapshot : snapshot.getChildren()) {
                            firstPetId = petSnapshot.getKey();
                            break;
                        }

                        if (firstPetId != null) {
                            // CurrentPetManager에 기본 반려견 ID 저장
                            CurrentPetManager.getInstance().setCurrentPetId(firstPetId);
                        }

                        // 메인 화면으로 이동
                        startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    } else {
                        // 반려견 노드 없으면 이름 입력 화면
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
