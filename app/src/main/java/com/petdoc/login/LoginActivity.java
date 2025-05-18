package com.petdoc.login;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.petdoc.R;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ImageButton btnLogin = findViewById(R.id.btn_google_login);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 로그인 성공 시 pet 정보 입력으로 이동
                Intent intent = new Intent(LoginActivity.this, NameInputActivity.class);
                startActivity(intent);
                finish(); // 로그인 화면 종료
            }
        });
    }
}
