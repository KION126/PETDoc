package com.petdoc.aiCheck.skin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.petdoc.R;
import com.petdoc.login.CurrentPetManager;
import com.petdoc.main.BaseActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 피부 이미지 분석 로딩 화면 (뒤로가기 제거됨)
 */
public class SkinLoadingActivity extends BaseActivity {

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

        // 이미지 URI 전달 받기
        String uriStr = getIntent().getStringExtra("image_uri");
        if (uriStr == null) return;

        Uri imageUri = Uri.parse(uriStr);

        // 예측 시뮬레이션 + Storage 업로드
        simulatePrediction(imageUri);
    }

    private void simulatePrediction(Uri imageUri) {
        handler.postDelayed(() -> {
            Map<String, Integer> resultMap = new HashMap<>();
            resultMap.put("papules_plaques", 37);
            resultMap.put("dandruff_scaling_epidermal_collarette", 57);
            resultMap.put("lichenification_hyperpigmentation", 87);
            resultMap.put("pustules_acne", 22);
            resultMap.put("erosion_ulceration", 44);
            resultMap.put("nodules_mass", 66);

            uploadImageAndSave(imageUri, resultMap);
        }, 1000);
    }

    private void uploadImageAndSave(Uri imageUri, Map<String, Integer> resultMap) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        String petId = CurrentPetManager.getInstance().getCurrentPetId();
        if (petId == null) return;

        String fileName = "skin_" + System.currentTimeMillis() + ".jpg";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("skin_images/" + uid + "/" + fileName);

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String downloadUrl = uri.toString();
                    saveToFirebase(resultMap, downloadUrl);
                    moveToResult(resultMap, downloadUrl);
                }))
                .addOnFailureListener(Throwable::printStackTrace);
    }

    private void saveToFirebase(Map<String, Integer> resultMap, String imageUrl) {
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
        record.put("imagePath", imageUrl); // Firebase Storage download URL
        record.put("prediction", resultMap);

        ref.setValue(record);
    }

    private void moveToResult(Map<String, Integer> resultMap, String imageUrl) {
        Intent intent = new Intent(this, SkinResultActivity.class);
        intent.putExtra("result_map", new HashMap<>(resultMap));
        intent.putExtra("image_uri", imageUrl); // 그대로 Glide 등에서 사용 가능
        startActivity(intent);
        finish();
    }
}
