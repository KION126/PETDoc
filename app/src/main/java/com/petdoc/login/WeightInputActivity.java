package com.petdoc.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.petdoc.R;
import com.petdoc.main.MainActivity;

public class WeightInputActivity extends AppCompatActivity {

    private EditText edtWeight;

    private ImageButton btnNext, btnPrev;

    private TextView tvPetName, tvPetNameTitle;
    private DatabaseReference dbRef;
    private String uid, petKey, petName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pet_weight);

        edtWeight = findViewById(R.id.edtWeight);
        btnNext = findViewById(R.id.btnNext);
      

        btnPrev = findViewById(R.id.btnPrev);

        tvPetName = findViewById(R.id.tvPetName);
        tvPetNameTitle = findViewById(R.id.tvPetNameTitle);


        // Firebase 초기화
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        uid = user.getUid();
        dbRef = FirebaseDatabase.getInstance().getReference();

        // petKey, petName 전달받기
        petKey = getIntent().getStringExtra("petKey");
        petName = getIntent().getStringExtra("petName");
        if (petKey == null) {
            Toast.makeText(this, "반려견 정보 없음", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 상단 이름 표시
        if (petName != null && !petName.isEmpty()) {
            tvPetName.setText(petName);
            tvPetNameTitle.setText(petName + "의 성별을 선택해 주세요");
        } else {
            tvPetName.setText("멍멍이 이름");
            tvPetNameTitle.setText("멍멍이의 성별을 선택해 주세요");
        }

        // 초기 상태
        btnNext.setEnabled(false);
        btnNext.setImageResource(R.drawable.ic_arrow_forward);

        //나중에 등록하기 버튼 클릭시
        findViewById(R.id.imgRegisterLater).setOnClickListener(v -> {
            Intent intent = new Intent(WeightInputActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // 입력 감지 → 버튼 상태 갱신
        edtWeight.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean valid = s.toString().trim().length() > 0;
                btnNext.setEnabled(valid);
                btnNext.setImageResource(valid
                        ? R.drawable.ic_arrow_forward2
                        : R.drawable.ic_arrow_forward);
            }
        });

        // 다음 버튼 클릭
        btnNext.setOnClickListener(v -> {
            String weightText = edtWeight.getText().toString().trim();
            if (weightText.isEmpty()) return;

            double weight;
            try {
                weight = Double.parseDouble(weightText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "숫자를 정확히 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            dbRef.child("Users")
                    .child(uid)
                    .child(petKey)
                    .child("basicInfo")
                    .child("weight")
                    .setValue(weight)
                    .addOnSuccessListener(unused -> {
                        Intent intent = new Intent(WeightInputActivity.this, PhotoInputActivity.class);
                        intent.putExtra("petKey", petKey); // 다음 화면에 반려견 정보 전달
                        intent.putExtra("petName", petName); // 다음 화면에도 이름 전달
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // 이전 버튼 클릭 → 이전 화면(GenderInputActivity)로 이동, 정보 전달
        ImageButton btnPrev = findViewById(R.id.btnPrev);
        btnPrev.setOnClickListener(v -> {
            Intent intent = new Intent(WeightInputActivity.this, GenderInputActivity.class);
            intent.putExtra("petKey", petKey);
            intent.putExtra("petName", petName);
            startActivity(intent);
            finish();
        });
    }
}
