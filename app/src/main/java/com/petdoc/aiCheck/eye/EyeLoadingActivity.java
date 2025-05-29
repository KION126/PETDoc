package com.petdoc.aiCheck.eye;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.petdoc.R;
import com.petdoc.aiCheck.eye.model.EyeDiseasePredictor;
import com.petdoc.aiCheck.utils.ImageUtils;
import com.petdoc.login.CurrentPetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Eye analysis loading page
 * - Analyzes selected eye images with EyeDiseasePredictor model
 * - Saves analysis results to Firebase
 */
public class EyeLoadingActivity extends AppCompatActivity {

    private EyeDiseasePredictor eyeDiseasePredictor;
    private Handler handler;
    private Runnable dotAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_eye_loading);

        TextView processingText = findViewById(R.id.text_processing);

        // 🔙 Back button
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // "Analyzing..." animation
        handler = new Handler(Looper.getMainLooper());
        dotAnimator = new Runnable() {
            int dotCount = 0;
            @Override
            public void run() {
                dotCount = (dotCount + 1) % 4;
                String dots = new String(new char[dotCount]).replace("\0", ".");
                processingText.setText("안구 분석 중" + dots);
                handler.postDelayed(this, 500);
            }
        };
        handler.post(dotAnimator);

        // 모델 로드
        String modelPath = "eye-010-0.7412.tflite";
        try {
            eyeDiseasePredictor = new EyeDiseasePredictor(getAssets(), modelPath);
        } catch (IOException e) {
            Log.e("EyeLoadingActivity", "모델 초기화 실패", e);
            return;
        }

        // 인텐트 데이터 수신
        String uriString = getIntent().getStringExtra("image_uri");
        String eyeSide = getIntent().getStringExtra("eye_side"); // "left" or "right"
        String petKey = getIntent().getStringExtra("pet_id");    // 선택된 반려견 ID

        if (uriString == null || eyeSide == null) {
            Log.e("EyeLoadingActivity", "image_uri 또는 eye_side 누락");
            return;
        }

        // petKey가 없으면 매니저에서 가져옴
        final String finalPetKey;
        if (petKey == null) {
            finalPetKey = CurrentPetManager.getInstance().getCurrentPetId();
        } else {
            finalPetKey = petKey;
        }
        if (finalPetKey == null) {
            Log.e("Firebase", "현재 선택된 반려견 ID 없음");
            return;
        }

        Uri imageUri = Uri.parse(uriString);
        Bitmap bitmap = null;
        try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            Log.e("EyeLoadingActivity", "이미지 로딩 실패", e);
        }

        if (bitmap != null) {
            float[] inputData = ImageUtils.preprocess(bitmap, 224);
            float[] result = eyeDiseasePredictor.predict(inputData);

            Log.d("EyeLoadingActivity", "예측 결과: " + formatResult(result));

            // 애니메이션 2초 유지 후 결과 화면 이동
            handler.postDelayed(() -> {
                handler.removeCallbacks(dotAnimator);

                // 결과 저장
                saveToFirebase(imageUri, result, eyeSide, finalPetKey);

                // 결과 화면으로 이동
                Intent intent = new Intent(EyeLoadingActivity.this, EyeResultActivity.class);
                intent.putExtra("eye_side", eyeSide);
                intent.putExtra("result", result);
                startActivity(intent);
                finish();
            }, 2000);
        } else {
            Log.e("EyeLoadingActivity", "Bitmap 복원 실패");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eyeDiseasePredictor != null) eyeDiseasePredictor.close();
        if (handler != null && dotAnimator != null) handler.removeCallbacks(dotAnimator);
    }

    /**
     * Save result to Firebase
     */
    private void saveToFirebase(Uri imageUri, float[] result, String eyeSide, String petKey) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("Firebase", "로그인된 사용자 없음");
            return;
        }

        String uid = user.getUid();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

        Map<String, Object> data = new HashMap<>();
        data.put("imagePath", imageUri.toString());

        // 영어(한글)로 필드 저장
        Map<String, Object> predictions = new HashMap<>();
        predictions.put("blepharitis", result[0]);            // 안검염
        predictions.put("eyelid_tumor", result[1]);           // 안검종양
        predictions.put("entropion", result[2]);              // 안검내반증
        predictions.put("epiphora", result[3]);               // 유루증
        predictions.put("pigmentary_keratitis", result[4]);   // 색소침착성각막염
        predictions.put("corneal_disease", result[5]);        // 각막질환
        predictions.put("nuclear_sclerosis", result[6]);      // 핵경화
        predictions.put("conjunctivitis", result[7]);         // 결막염
        predictions.put("nonulcerative_keratitis", result[8]);// 비궤양성 각막질환
        predictions.put("other", result[9]);                  // 기타

        data.put("prediction", predictions);

        dbRef.child("Users").child(uid)
                .child(petKey)
                .child("eyeAnalysis")
                .child(eyeSide)
                .setValue(data)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "분석 결과 저장 성공"))
                .addOnFailureListener(e -> Log.e("Firebase", "저장 실패", e));
    }

    /**
     * 예측 결과 배열을 문자열로 변환
     */
    private String formatResult(float[] result) {
        StringBuilder sb = new StringBuilder();
        for (float v : result) {
            sb.append(String.format("[%.4f] ", v));
        }
        return sb.toString();
    }
}
