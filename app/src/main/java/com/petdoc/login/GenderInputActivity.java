package com.petdoc.login;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import com.petdoc.main.MainActivity;

public class GenderInputActivity extends AppCompatActivity {
    private ImageButton btnMale, btnFemale, btnNeutered, btnNext, btnPrev;
    private ImageView imgMale, imgFemale, labelMale, labelFemale, labelNeutered;
    private TextView tvPetName, tvPetNameTitle;

    // 데이터 변수
    private String selectedGender = null; // "수컷" or "암컷"
    private boolean isNeutered = false;

    private DatabaseReference dbRef;
    private String uid, petKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pet_gender);

        // 파이어베이스 인증 체크
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        uid = user.getUid();
        dbRef = FirebaseDatabase.getInstance().getReference();


        // 뷰 연결
        btnMale = findViewById(R.id.btnMale);
        btnFemale = findViewById(R.id.btnFemale);
        btnNeutered = findViewById(R.id.btnNeutered);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);

        imgMale = findViewById(R.id.imgSearch);
        imgFemale = findViewById(R.id.imgLocation);

        labelMale = findViewById(R.id.labelMaleSelected);
        labelFemale = findViewById(R.id.labelFemaleUnselected);
        labelNeutered = findViewById(R.id.labelNeutered);

        tvPetName = findViewById(R.id.tvPetName);
        tvPetNameTitle = findViewById(R.id.tvPetNameTitle);

        String petName = getIntent().getStringExtra("petName");

        // 상단 이름 표시
        if (petName != null && !petName.isEmpty()) {
            tvPetName.setText(petName);
            tvPetNameTitle.setText(petName + "의 성별을 선택해 주세요");
        } else {
            tvPetName.setText("멍멍이 이름");
            tvPetNameTitle.setText("멍멍이의 성별을 선택해 주세요");
        }

        btnNext.setEnabled(false);
        btnNext.setImageResource(R.drawable.ic_arrow_forward);

        findViewById(R.id.imgRegisterLater).setOnClickListener(v -> {
            dbRef.child("Users").child(uid).get().addOnSuccessListener(snapshot -> {
                int petCount = (int) snapshot.getChildrenCount();
                String newPetKey = "Dog" + (petCount + 1);

                PetInfoUtils.updateCompletedDogInfo(getIntent().getExtras(), newPetKey);

                Intent intent = new Intent(GenderInputActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        });


        btnMale.setOnClickListener(v -> {
            selectedGender = "Male";
            imgMale.setImageResource(R.drawable.gender_male_on);
            labelMale.setImageResource(R.drawable.ic_male_on_label);
            imgFemale.setImageResource(R.drawable.gender_female_off);
            labelFemale.setImageResource(R.drawable.ic_female_off_label);
            activateNextButton();
        });

        btnFemale.setOnClickListener(v -> {
            selectedGender = "Female";
            imgMale.setImageResource(R.drawable.gender_male_off);
            labelMale.setImageResource(R.drawable.ic_male_off_label);
            imgFemale.setImageResource(R.drawable.gender_female_on);
            labelFemale.setImageResource(R.drawable.ic_female_on_label);
            activateNextButton();
        });

        // 중성화 여부 토글
        btnNeutered.setOnClickListener(v -> {
            isNeutered = !isNeutered;
            if (isNeutered) {
                btnNeutered.setImageResource(R.drawable.ic_btn_eclipse_selected);
                labelNeutered.setImageResource(R.drawable.ic_neutering_on_label);
            } else {
                btnNeutered.setImageResource(R.drawable.ic_btn_eclipse);
                labelNeutered.setImageResource(R.drawable.ic_neutering_off_label);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (selectedGender == null) return;

            Intent intent = new Intent(GenderInputActivity.this, WeightInputActivity.class);
            intent.putExtras(getIntent()); // 이전 값들 유지
            intent.putExtra("petGender", selectedGender);
            intent.putExtra("petNeutered", isNeutered);
            startActivity(intent);
            finish();
        });



        // [이전] 버튼 - 이름 입력 화면으로 (petKey, petName 전달)
        btnPrev.setOnClickListener(v -> {
            Intent intent = new Intent(GenderInputActivity.this, NameInputActivity.class);
            intent.putExtras(getIntent());
            startActivity(intent);
            finish();
        });
    }

    // 다음 버튼 활성화
    private void activateNextButton() {
        btnNext.setEnabled(true);
        btnNext.setImageResource(R.drawable.ic_arrow_forward2);
    }
}
