package com.petdoc.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
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
import com.petdoc.login.utils.PetInfoUtils;
import com.petdoc.main.BaseActivity;
import com.petdoc.main.MainActivity;

public class WeightInputActivity extends BaseActivity {

    private EditText edtWeight;

    private Button btnNext, btnPrev;

    private TextView tvPetNameTitle;
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

        petName = getIntent().getStringExtra("petName");

        // 상단 이름 표시
        if (petName != null && !petName.isEmpty()) {
            tvPetNameTitle.setText(petName + "의 몸무게를\n입력해 주세요");
        } else {
            tvPetNameTitle.setText("멍멍이의 몸무게를\n입력해 주세요");
        }

        // 초기 상태
        btnNext.setEnabled(false);

        findViewById(R.id.imgRegisterLater).setOnClickListener(v -> {
            dbRef.child("Users").child(uid).get().addOnSuccessListener(snapshot -> {
                int petCount = (int) snapshot.getChildrenCount();
                String newPetKey = "Dog" + (petCount + 1);

                PetInfoUtils.updateCompletedDogInfo(getIntent().getExtras(), newPetKey);

                Intent intent = new Intent(WeightInputActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        });


        // 입력 감지 → 버튼 상태 갱신
        edtWeight.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean valid = s.toString().trim().length() > 0;
                btnNext.setEnabled(valid);
            }
        });

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

            // Intent로 값 전달만 (저장은 하지 않음)
            Intent intent = new Intent(WeightInputActivity.this, PhotoInputActivity.class);
            intent.putExtras(getIntent()); // 이전 값 유지
            intent.putExtra("petWeight", weight);
            startActivity(intent);
            finish();
        });


        // 이전 버튼 클릭 → 이전 화면(GenderInputActivity)로 이동, 정보 전달
        btnPrev.setOnClickListener(v -> {
            Intent intent = new Intent(WeightInputActivity.this, GenderInputActivity.class);
            intent.putExtras(getIntent());
            startActivity(intent);
            finish();
        });

    }
}
