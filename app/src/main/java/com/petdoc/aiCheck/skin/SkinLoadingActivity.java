package com.petdoc.aiCheck.skin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.petdoc.R;
import com.petdoc.login.CurrentPetManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SkinLoadingActivity extends AppCompatActivity {

    private Handler handler;

    private static final String[] LABELS = {
            "papules_plaques",
            "dandruff_scaling_epidermal_collarette",
            "lichenification_hyperpigmentation",
            "pustules_acne",
            "erosion_ulceration",
            "nodules_mass"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_skin_loading);

        handler = new Handler(Looper.getMainLooper());

        // Get image URI
        String uriStr = getIntent().getStringExtra("image_uri");

        // Simulate prediction
        simulatePrediction(uriStr);
    }

    private void simulatePrediction(String imageUri) {
        handler.postDelayed(() -> {
            Map<String, Integer> resultMap = new HashMap<>();

            resultMap.put("papules_plaques", 37);
            resultMap.put("dandruff_scaling_epidermal_collarette", 57);
            resultMap.put("lichenification_hyperpigmentation", 87);
            resultMap.put("pustules_acne", 22);
            resultMap.put("erosion_ulceration", 44);
            resultMap.put("nodules_mass", 66);

            // Save to Firebase
            saveToFirebase(resultMap, imageUri);

            // Navigate to result screen
            Intent intent = new Intent(this, SkinResultActivity.class);
            intent.putExtra("result_map", new HashMap<>(resultMap));
            if (imageUri != null) {
                intent.putExtra("image_uri", imageUri);
            }
            startActivity(intent);
            finish();
        }, 1000);
    }

    private void saveToFirebase(Map<String, Integer> resultMap, String imageUri) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        String petId = CurrentPetManager.getInstance().getCurrentPetId();
        if (petId == null) return;

        String timestamp = new SimpleDateFormat("yyyy.MM.dd(EE) HH:mm", Locale.KOREAN).format(new Date());

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child(petId)
                .child("skinAnalysis")
                .push();

        Map<String, Object> record = new HashMap<>();
        record.put("createdAt", timestamp);
        record.put("imagePath", imageUri);
        record.put("prediction", resultMap);

        ref.setValue(record);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
