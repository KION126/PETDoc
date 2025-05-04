package com.petdoc.main;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petdoc.R;

import android.util.Log;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Firestore 연동 확인
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (db != null) {
            Log.d("Firestore", "Firestore 연동 성공!");
        } else {
            Log.w("Firestore", "Firestore 연동 실패...");
        }
    }
}
