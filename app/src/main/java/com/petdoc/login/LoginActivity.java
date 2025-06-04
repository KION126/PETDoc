package com.petdoc.login;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.gms.tasks.Task;
import com.petdoc.R;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import com.petdoc.main.BaseActivity;
import com.petdoc.main.MainActivity;

public class LoginActivity extends BaseActivity {

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private DatabaseReference dbRef;

    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewPager2 viewPager = findViewById(R.id.iconSlider);
        int[] icons = {
                R.drawable.ic_walk_log,
                R.drawable.ic_genetic_note,
                R.drawable.ic_smart_check
        };
        IconSliderAdapter adapter = new IconSliderAdapter(icons);
        viewPager.setAdapter(adapter);

        // 사용자 터치 막기 (스크롤 비활성)
        viewPager.setUserInputEnabled(false);

        // 수직 방향으로 슬라이드 설정
        viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        // 자동 슬라이드 (3초 간격)
        final Handler sliderHandler = new Handler();
        Runnable sliderRunnable = new Runnable() {
            int currentPage = 0;
            @Override
            public void run() {
                int nextPage = (viewPager.getCurrentItem() + 1) % icons.length;

                // 마지막 → 첫 페이지일 때 애니메이션 없이 점프
                boolean animate = nextPage != 0;

                viewPager.setCurrentItem(nextPage, animate);

                sliderHandler.postDelayed(this, 3000);
            }
        };
        sliderHandler.postDelayed(sliderRunnable, 3000);


        // FirebaseAuth, Database 초기화
        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        // 구글 로그인 옵션
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 버튼 리스너
        ImageButton btnLogin = findViewById(R.id.btn_google_login);
        btnLogin.setOnClickListener(v -> signInWithGoogle());

        // 구글 로그인 결과 launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleGoogleSignInResult(result.getData());
                    } else {
                        Toast.makeText(this, "로그인이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 구글 로그인 인텐트 실행
     */
    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    /**
     * 구글 로그인 결과 처리
     */
    private void handleGoogleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken());
            } else {
                Toast.makeText(this, "구글 로그인 실패: 계정 정보 없음", Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            Toast.makeText(this, "구글 로그인 실패", Toast.LENGTH_SHORT).show();
            Log.e("LoginActivity", "Google sign-in failed", e);
        }
    }

    /**
     * Firebase 인증 처리
     */
    private void firebaseAuthWithGoogle(@NonNull String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // **수정: 사용자 이름 유무 체크 → 메인/이름입력 분기**
                            moveToNextByName(firebaseUser);
                        }
                    } else {
                        Toast.makeText(this, "Firebase 인증 실패", Toast.LENGTH_SHORT).show();
                        Log.e("LoginActivity", "Firebase auth failed", task.getException());
                    }
                });
    }

    /**
     * 이름 등록 여부에 따라 MainActivity 또는 NameInputActivity로 이동
     */
    private void moveToNextByName(FirebaseUser firebaseUser) {
        String uid = firebaseUser.getUid();
        DatabaseReference userRef = dbRef.child("Users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean hasName = false;
                for (DataSnapshot pet : snapshot.getChildren()) {
                    if (pet.child("basicInfo").child("name").exists()) {
                        hasName = true;

                        // 첫 번째로 이름이 존재하는 반려동물의 petKey 저장
                        String firstPetKey = pet.getKey();
                        CurrentPetManager.getInstance().setCurrentPetId(firstPetKey);
                        break;
                    }
                }
                if (hasName) {
                    // 이름 있으면 바로 메인
                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                } else {
                    // 이름 없으면 이름입력
                    startActivity(new Intent(LoginActivity.this, NameInputActivity.class));
                }
                finish();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // 실패 시 이름입력 화면으로 fallback
                startActivity(new Intent(LoginActivity.this, NameInputActivity.class));
                finish();
            }
        });
    }

}
