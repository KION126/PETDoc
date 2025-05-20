package com.petdoc.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.petdoc.R;

public class WeightInputActivity extends AppCompatActivity {

    private EditText edtWeight;
    private ImageButton btnNext;
    private DatabaseReference dbRef;
    private String uid, petKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_weight);

        edtWeight = findViewById(R.id.edtWeight);
        btnNext = findViewById(R.id.btnNext);

        // Firebase 초기화
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        uid = user.getUid();
        dbRef = FirebaseDatabase.getInstance().getReference();

        // petKey 전달받기
        petKey = getIntent().getStringExtra("petKey");
        if (petKey == null) {
            Toast.makeText(this, "반려견 정보 없음", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 초기 상태
        btnNext.setEnabled(false);
        btnNext.setImageResource(R.drawable.ic_arrow_forward);

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
                    .child("기본정보")
                    .child("체중")
                    .setValue(weight)
                    .addOnSuccessListener(unused -> {
                        Intent intent = new Intent(WeightInputActivity.this, PhotoInputActivity.class);
                        intent.putExtra("petKey", petKey); // 다음 화면에 반려견 정보 전달
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }
}
