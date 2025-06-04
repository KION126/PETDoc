package com.petdoc.main;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.petdoc.R;
import com.petdoc.aiCheck.AICheckActivity;
import com.petdoc.genetic.GeneticInfoActivity;
import com.petdoc.genetic.GeneticNoteActivity;
import com.petdoc.login.CurrentPetManager;
import com.petdoc.login.LoginActivity;
import com.petdoc.walklog.CalendarActivity;
import com.petdoc.login.NameInputActivity;
import com.petdoc.login.PetListAdapter;
import com.petdoc.login.model.DogBasicInfo;
import com.petdoc.login.model.Pet;
import com.petdoc.map.MapActivity;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private FrameLayout btnGeneticNote;
    private FrameLayout btnSmartCheck;
    private TextView nameText;
    private TextView walkLogText;
    private ImageView accountIcon;    // 오른쪽 상단 계정(로그아웃) 아이콘
    private ImageView dogIcon;        // 이름 왼쪽 동그란 프로필 이미지
    private String uid;
    private DatabaseReference dbRef;
    private LinearLayout nameWithArrow; // 멍멍이 이름 드롭
    private FrameLayout btnFindHospital;

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
        nameWithArrow = findViewById(R.id.nameWithArrow);
        btnFindHospital = findViewById(R.id.btnFindHospital);

        //멍멍이 이름 클릭시 리스트 드롭
        nameWithArrow.setOnClickListener(v -> {
            showPetSelectorDialog(); // 우리가 만든 메서드 호출
        });

        //병원찾기버튼
        btnFindHospital.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapActivity.class);
            startActivity(intent);
        });

        // 오른쪽 계정(프로필) 아이콘 클릭 시 로그아웃
        accountIcon.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(MainActivity.this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        // 현재 선택된 반려견 ID 가져오기
        String selectedPetKey = CurrentPetManager.getInstance().getCurrentPetId();

        if (selectedPetKey != null && !selectedPetKey.isEmpty()) {
            dbRef.child("Users").child(uid).child(selectedPetKey).child("basicInfo").get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            String name = snapshot.child("name").getValue(String.class);
                            String profileUrl = snapshot.child("imagePath").getValue(String.class);

                            if (name != null) {
                                nameText.setText(name);
                                walkLogText.setText(name + "의 산책 일지");
                            } else {
                                nameText.setText("멍멍이 이름");
                                walkLogText.setText("멍멍이 이름의 산책 일지");
                            }

                            if (profileUrl != null && !profileUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(profileUrl)
                                        .placeholder(R.drawable.ic_dog_icon)
                                        .error(R.drawable.ic_dog_icon)
                                        .circleCrop()
                                        .into(dogIcon);
                            } else {
                                dogIcon.setImageResource(R.drawable.ic_dog_icon);
                            }
                        } else {
                            nameText.setText("멍멍이 이름");
                            walkLogText.setText("멍멍이 이름의 산책 일지");
                            dogIcon.setImageResource(R.drawable.ic_dog_icon);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "강아지 정보를 불러오지 못했습니다", Toast.LENGTH_SHORT).show();
                        nameText.setText("멍멍이 이름");
                        walkLogText.setText("멍멍이 이름의 산책 일지");
                        dogIcon.setImageResource(R.drawable.ic_dog_icon);
                    });
        } else {
            // 선택된 반려견 ID가 없을 경우 기본값 처리
            nameText.setText("멍멍이 이름");
            walkLogText.setText("멍멍이 이름의 산책 일지");
            dogIcon.setImageResource(R.drawable.ic_dog_icon);
        }


        // [1] 유전병 진단 노트 버튼
        btnGeneticNote.setOnClickListener(v -> {
            navigateToGeneticScreen();
        });

        // [2] AI 스마트 간편 검진 버튼
        btnSmartCheck.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AICheckActivity.class));
        });

        // [3] 반려견 산책일지 버튼
        walkLogText.setOnClickListener(v -> {
            String dogId = CurrentPetManager.getInstance().getCurrentPetId();
            String dogName = nameText.getText().toString();

            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            intent.putExtra("dogId", dogId);
            intent.putExtra("dogName", dogName);
            startActivity(intent);
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

    private void showPetSelectorDialog() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> petIdList = new ArrayList<>();
                List<Pet> petList = new ArrayList<>();

                for (DataSnapshot petSnapshot : snapshot.getChildren()) {
                    Log.d("DEBUG", "petId: " + petSnapshot.getKey());
                    Log.d("DEBUG", "전체 petSnapshot: " + petSnapshot.toString());
                    Log.d("DEBUG", "basicInfo: " + petSnapshot.child("basicInfo").toString());
                    Log.d("DEBUG", "name 값: " + petSnapshot.child("basicInfo").child("name").getValue());

                    DogBasicInfo info = petSnapshot.child("basicInfo").getValue(DogBasicInfo.class);

                    if (info != null) {
                        Pet pet = new Pet();
                        pet.basicInfo = info;

                        petIdList.add(petSnapshot.getKey());
                        petList.add(pet);

                        Log.d("DEBUG", "추가된 pet: " + info.name);
                    }
                }

                Log.d("DEBUG", "최종 petList 크기: " + petList.size());
                openPetBottomSheetDialog(petIdList, petList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "반려견 데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    //드롭다운 다이얼로그 열기
    private void openPetBottomSheetDialog(List<String> petIdList, List<Pet> petList) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_pet_selector, null);

        RecyclerView recyclerView = view.findViewById(R.id.petListRecyclerView);
        TextView addNewPet = view.findViewById(R.id.addNewPet);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new PetListAdapter(this, petIdList, petList,
                CurrentPetManager.getInstance().getCurrentPetId(), selectedPetId -> {
            CurrentPetManager.getInstance().setCurrentPetId(selectedPetId);
            dialog.dismiss();
            recreate(); // MainActivity 갱신
        }));

        addNewPet.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, NameInputActivity.class));
        });

        dialog.setContentView(view);
        dialog.show();
    }
}
