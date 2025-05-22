package com.petdoc.genetic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;

public class GeneticNoteActivity extends AppCompatActivity {

    private Button btnNext;
    private Button btnBack;
    private ImageView btnAlbem;
    private ImageView btnCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_genetic);

        // 뷰 연결
        btnNext = findViewById(R.id.btn_next);
        btnBack = findViewById(R.id.btn_back);
        btnAlbem = findViewById(R.id.btn_album);
        btnCamera = findViewById(R.id.btn_carmera);

        // 다음 페이지 (로딩 페이지)
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GeneticNoteActivity.this, GeneticLoadingActivity.class);
                startActivity(intent);
            }
        });

        // 이전 페이지
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                finish();
            }
        });

        // 다음 버튼 초기 설정
        btnNext.setEnabled(true);
        btnNext.setText("우리 아이의 과거로!");
    }
}
