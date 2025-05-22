package com.petdoc.main;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petdoc.R;
import com.petdoc.genetic.GeneticLoadingActivity;
import com.petdoc.genetic.GeneticNoteActivity;

import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;


public class MainActivity extends AppCompatActivity {

    private FrameLayout btnGeneticNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_page);

        btnGeneticNote = findViewById(R.id.btnGeneticNote);

        // Firestore 연동 확인
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (db != null) {
            Log.d("Firestore", "Firestore 연동 성공!");
        } else {
            Log.w("Firestore", "Firestore 연동 실패...");
        }

        btnGeneticNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GeneticNoteActivity.class);
                startActivity(intent);
            }
        });
    }
}
