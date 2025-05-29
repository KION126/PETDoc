package com.petdoc.main;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.petdoc.R;
import com.petdoc.aiCheck.AICheckActivity;
import com.petdoc.genetic.GeneticInfoActivity;
import com.petdoc.genetic.GeneticNoteActivity;
import com.petdoc.login.CurrentPetManager;
import com.petdoc.login.LoginActivity;

public class MainActivity extends AppCompatActivity {

    private FrameLayout btnGeneticNote;
    private FrameLayout btnSmartCheck;
    private TextView nameText;
    private TextView walkLogText;
    private ImageView accountIcon;    // 오른쪽 상단 계정(로그아웃) 아이콘
    private ImageView dogIcon;        // 이름 왼쪽 동그란 프로필 이미지
    private String uid;
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_page);

        // Firebase 로그인 유저 확인
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        uid = user.getUid();
        dbRef = FirebaseDatabase.getInstance().getReference();

        // 뷰 바인딩
        nameText = findViewById(R.id.nameText);
        walkLogText = findViewById(R.id.walkLogText);
        btnGeneticNote = findViewById(R.id.btnGeneticNote);
        btnSmartCheck = findViewById(R.id.btnSmartCheck);
        accountIcon = findViewById(R.id.accountIcon);
        dogIcon = findViewById(R.id.dogIcon);

        // 오른쪽 계정(프로필) 아이콘 클릭 시 로그아웃
        accountIcon.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(MainActivity.this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        // ✅ 강아지 이름/산책일지/프로필 사진 불러오기 (첫 번째 반려견 기준)
        dbRef.child("Users").child(uid).get().addOnSuccessListener(snapshot -> {
            boolean found = false;
            for (DataSnapshot petSnapshot : snapshot.getChildren()) {
                if (petSnapshot.hasChild("기본정보")) {
                    String petKey = petSnapshot.getKey();
                    String name = petSnapshot.child("기본정보").child("이름").getValue(String.class);
                    String profileUrl = petSnapshot.child("기본정보").child("이미지파일경로로").getValue(String.class);

                    if (name != null) {
                        nameText.setText(name);
                        walkLogText.setText(name + "의 산책 일지");
                    }
                    // Glide로 dogIcon에 프로필 세팅 (없으면 기본 아이콘)
                    if (profileUrl != null && !profileUrl.isEmpty()) {
                        Glide.with(this)
                                .load(profileUrl)
                                .placeholder(R.drawable.ic_dog_icon) // 기본 강아지 아이콘
                                .error(R.drawable.ic_dog_icon)
                                .circleCrop()
                                .into(dogIcon);
                    } else {
                        dogIcon.setImageResource(R.drawable.ic_dog_icon);
                    }
                    found = true;
                    break;
                }
            }
            if (!found) {
                nameText.setText("멍멍이 이름");
                walkLogText.setText("멍멍이 이름의 산책 일지");
                dogIcon.setImageResource(R.drawable.ic_dog_icon);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "강아지 정보를 불러오지 못했습니다", Toast.LENGTH_SHORT).show();
            nameText.setText("멍멍이 이름");
            walkLogText.setText("멍멍이 이름의 산책 일지");
            dogIcon.setImageResource(R.drawable.ic_dog_icon);
        });

        // [1] 유전병 진단 노트 버튼
        btnGeneticNote.setOnClickListener(v -> {
            navigateToGeneticScreen();
        });

        // [2] AI 스마트 간편 검진 버튼
        btnSmartCheck.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AICheckActivity.class));
        });
    }

    // [1] 유전병 진단 노트 버튼 이전 유전병 예측 데이터 존재 확인 및 페이지 이동 메서드
    private void navigateToGeneticScreen() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String currentPetId = CurrentPetManager.getInstance().getCurrentPetId();

        if (user == null || currentPetId == null) {
            Log.e("MainActivity", "사용자 또는 반려견 ID 없음");
            return;
        }

        String uid = user.getUid();

        FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child(currentPetId)
                .child("Genetic")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        // 예측 결과 존재 → GeneticInfoActivity로 이동
                        startActivity(new Intent(MainActivity.this, GeneticInfoActivity.class));
                    } else {
                        // 결과 없음 → GeneticNoteActivity로 이동
                        startActivity(new Intent(MainActivity.this, GeneticNoteActivity.class));
                    }
                });
    }

}
