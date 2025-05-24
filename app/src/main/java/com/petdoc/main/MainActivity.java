package com.petdoc.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;
import com.petdoc.aiCheck.AICheckActivity;
import com.petdoc.genetic.GeneticNoteActivity;

import android.view.View;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    private FrameLayout btnGeneticNote;
    private FrameLayout btnSmartCheck; // AI 스마트 간편 검진 버튼

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_page);

        // [1] 유전병 진단 노트 버튼 클릭 시 GeneticNoteActivity로 이동
        btnGeneticNote = findViewById(R.id.btnGeneticNote);
        btnGeneticNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GeneticNoteActivity.class);
                startActivity(intent);
            }
        });

        // [2] AI 스마트 간편 검진 버튼 클릭 시 AICheckActivity로 이동
        btnSmartCheck = findViewById(R.id.btnSmartCheck);
        btnSmartCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AICheckActivity.class);
                startActivity(intent);
            }
        });

    }
}