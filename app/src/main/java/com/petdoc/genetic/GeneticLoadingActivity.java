package com.petdoc.genetic;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.petdoc.R;
import com.petdoc.genetic.model.BreedDiseasePredictor;
import com.petdoc.genetic.model.ImageUtils;
import com.petdoc.login.CurrentPetManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class GeneticLoadingActivity extends AppCompatActivity {

    private BreedDiseasePredictor breedDiseasePredictor;
    private TextView tvProcessing;
    private Handler handler;
    private Runnable dotAnimator;
    private FirebaseUser currentUser;
    private long startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_genetic);

        // 뷰 연결
        tvProcessing = findViewById(R.id.tv_processing);
        // 페이지 시작 시간 저장
        startTime = System.currentTimeMillis();
        // 로딩 애니메이션 적용
        setupLoadingAnimation();

        // 현재 유저 확인
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("GeneticLoadingActivity", "로그인된 사용자 없음");
            return;
        }

        // 견종 예측 모델 정의
        try {
            breedDiseasePredictor = new BreedDiseasePredictor(getAssets(), "breed-010-0.8489.tflite", 18);
        } catch (IOException e) {
            Log.e("GeneticLoadingActivity", "모델 초기화 실패", e);
            return;
        }

        // 견종 이미지 로드
        String uriString = getIntent().getStringExtra("image_uri");
        if (uriString == null) {
            Log.e("GeneticLoadingActivity", "이미지 URI 누락");
            return;
        }

        Uri imageUri = Uri.parse(uriString);
        Bitmap bitmap = null;
        try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            Log.e("GeneticLoadingActivity", "이미지 로딩 실패", e);
        }

        if (bitmap != null) {
            handlePrediction(bitmap);
        } else {
            Log.e("GeneticLoadingActivity", "Bitmap 복원 실패");
        }
    }

    // 로딩 애니메이션 메서드
    private void setupLoadingAnimation() {
        handler = new Handler(Looper.getMainLooper());
        dotAnimator = new Runnable() {
            int dotCount = 0;
            @Override
            public void run() {
                dotCount = (dotCount + 1) % 4;
                String dots = new String(new char[dotCount]).replace("\0", ".");
                tvProcessing.setText("유전병 분석 중" + dots);
                handler.postDelayed(this, 500);
            }
        };
        handler.post(dotAnimator);
    }

    // 견종 에측 수행 메서드
    private void handlePrediction(Bitmap bitmap) {
        float[] inputData = ImageUtils.preprocess(bitmap, 224);
        float[] result = breedDiseasePredictor.predict(inputData);

        int[] topIndices = getTopKIndices(result, 3);
        String[] breedLabels = {
                "maltese_dog", "miniature_poodle", "pomeranian", "shih-tzu",
                "chihuahua", "yorkshire_terrier", "beagle", "french_bulldog",
                "labrador_retriever", "miniature_schnauzer", "cocker_spaniel",
                "samoyed", "pug", "pekinese", "pembroke", "siberian_husky",
                "west_highland_white_terrier", "italian_greyhound"
        };

        Map<String, Object> breedMap = new LinkedHashMap<>();

        for (int idx : topIndices) {
            String breedName = breedLabels[idx];
            int score = Math.round(result[idx] * 100);
            breedMap.put(breedName, score);
        }

        uploadToFirebase(bitmap, breedMap, () -> {
            handler.removeCallbacks(dotAnimator);

            long elapsed = System.currentTimeMillis() - startTime;
            long delay = Math.max(0, 3000 - elapsed);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(GeneticLoadingActivity.this, GeneticInfoActivity.class);
                startActivity(intent);
                finish();
            }, delay);
        });
    }

    // 예측 결과 데이터 FB저장 메서드
    private void uploadToFirebase(Bitmap bitmap, Map<String, Object> breedMap, Runnable onComplete) {
        String uid = currentUser.getUid();
        String currentPetId = CurrentPetManager.getInstance().getCurrentPetId();
        if (currentPetId == null) {
            Log.e("GeneticLoadingActivity", "현재의 반려견 ID 없음");
        }

        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());

        String imagePath = "ai_genetic_images/" + uid + "/" + currentPetId + "/" + timestamp + ".jpg";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference(imagePath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] imageData = baos.toByteArray();

        UploadTask uploadTask = storageRef.putBytes(imageData);
        uploadTask.addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            String imageUrl = uri.toString();

            Map<String, Object> data = new HashMap<>(breedMap);
            data.put("imageUrl", imageUrl);

            assert currentPetId != null;

            // [1] 기존 Genetic 데이터 삭제 후 저장
            FirebaseDatabase.getInstance()
                    .getReference("Users")
                    .child(uid)
                    .child(currentPetId)
                    .child("Genetic")
                    .removeValue()
                    .addOnCompleteListener(removeTask -> {
                        if (removeTask.isSuccessful()) {
                            // [2] 삭제 성공 후 새로운 예측 결과 저장
                            FirebaseDatabase.getInstance()
                                    .getReference("Users")
                                    .child(uid)
                                    .child(currentPetId)
                                    .child("Genetic")
                                    .child(timestamp)
                                    .setValue(data)
                                    .addOnCompleteListener(setTask -> {
                                        if (setTask.isSuccessful()) {
                                            Log.d("Firebase", "새 데이터 저장 성공");
                                        } else {
                                            Log.e("Firebase", "새 데이터 저장 실패", setTask.getException());
                                        }
                                        onComplete.run();
                                    });
                        } else {
                            Log.e("Firebase", "기존 데이터 삭제 실패", removeTask.getException());
                            onComplete.run();
                        }
                    });

        })).addOnFailureListener(e -> Log.e("Firebase", "이미지 업로드 실패", e));
    }

    // 예측 결과 유사도 후가공 메서드
    public static int[] getTopKIndices(float[] array, int k) {
        int[] indices = new int[k];
        float[] topScores = new float[k];
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < k; j++) {
                if (array[i] > topScores[j]) {
                    for (int l = k - 1; l > j; l--) {
                        topScores[l] = topScores[l - 1];
                        indices[l] = indices[l - 1];
                    }
                    topScores[j] = array[i];
                    indices[j] = i;
                    break;
                }
            }
        }
        return indices;
    }

    // 리소스 해제
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (breedDiseasePredictor != null) {
            breedDiseasePredictor.close();
        }
        if (handler != null && dotAnimator != null) {
            handler.removeCallbacks(dotAnimator);
        }
    }
}
