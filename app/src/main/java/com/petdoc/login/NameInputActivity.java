package com.petdoc.login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.petdoc.main.BaseActivity;

public class NameInputActivity extends BaseActivity {

    private EditText edtPetName;
    private Button btnNext, btnPrev;

    private DatabaseReference dbRef;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pet_name);

        edtPetName = findViewById(R.id.edtPetName);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);

        // 인텐트로부터 값 복원
        String petKeyFromBack = getIntent().getStringExtra("petKey");
        String petNameFromBack = getIntent().getStringExtra("petName");

        if (petNameFromBack != null) {
            edtPetName.setVisibility(EditText.VISIBLE);
            edtPetName.setText(petNameFromBack);
            btnNext.setEnabled(true);
        }

        // Firebase 초기화
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            uid = user.getUid();
        } else {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbRef = FirebaseDatabase.getInstance().getReference();

        btnNext.setEnabled(false);

        edtPetName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                btnNext.setEnabled(hasText);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (!btnNext.isEnabled()) return;

            String petName = edtPetName.getText().toString().trim();

            // 이름만 intent로 전달하고, 저장은 하지 않음
            Intent intent = new Intent(NameInputActivity.this, GenderInputActivity.class);
            intent.putExtras(getIntent());                 // 혹시 이전 intent 값이 있으면 함께 넘김
            intent.putExtra("petName", petName);           // 현재 이름 값
            startActivity(intent);
            finish();
        });


        btnPrev.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();  // 로그아웃 처리
            Intent intent = new Intent(NameInputActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

    }
}
