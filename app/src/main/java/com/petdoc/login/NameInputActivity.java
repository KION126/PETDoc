package com.petdoc.login;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class NameInputActivity extends AppCompatActivity {

    private EditText edtPetName;
    private ImageView hintText;
    private ImageButton btnNext;

    private DatabaseReference dbRef;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_name);

        edtPetName = findViewById(R.id.edtPetName);
        hintText = findViewById(R.id.hintText);
        btnNext = findViewById(R.id.btnNext);

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
        btnNext.setImageResource(R.drawable.ic_arrow_forward);

        hintText.setOnClickListener(v -> {
            hintText.setVisibility(ImageView.GONE);
            edtPetName.setVisibility(EditText.VISIBLE);
            edtPetName.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(edtPetName, InputMethodManager.SHOW_IMPLICIT);
        });

        edtPetName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                boolean hasText = s.toString().trim().length() > 0;
                btnNext.setEnabled(hasText);
                btnNext.setImageResource(hasText
                        ? R.drawable.ic_arrow_forward2
                        : R.drawable.ic_arrow_forward);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (!btnNext.isEnabled()) return;

            String petName = edtPetName.getText().toString().trim();
            DatabaseReference userRef = dbRef.child("Users").child(uid);

            // 현재 반려견 수를 가져와서 자동 키 생성
            userRef.get().addOnSuccessListener(snapshot -> {
                int petCount = (int) snapshot.getChildrenCount();
                String newPetKey = "반려견" + (petCount + 1);

                userRef.child(newPetKey)
                        .child("기본정보")
                        .child("이름")
                        .setValue(petName)
                        .addOnSuccessListener(unused -> {
                            // 다음 액티비티로 이동하면서 petKey도 같이 전달
                            Intent intent = new Intent(NameInputActivity.this, GenderInputActivity.class);
                            intent.putExtra("petKey", newPetKey);     // 예: "반려견1"
                            intent.putExtra("petName", petName);     // 실제 입력한 이름
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );
            });
        });
    }
}
