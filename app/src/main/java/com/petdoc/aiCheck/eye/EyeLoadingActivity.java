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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.petdoc.R;
import com.petdoc.aiCheck.eye.model.EyeDiseasePredictor;
import com.petdoc.aiCheck.utils.ImageUtils;
import com.petdoc.login.CurrentPetManager;
import com.petdoc.main.BaseActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EyeLoadingActivity extends BaseActivity {

    private EyeDiseasePredictor eyeDiseasePredictor;
    private Handler handler;
    private Runnable dotAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_eye_loading);

        TextView processingText = findViewById(R.id.text_processing);

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

        try {
            eyeDiseasePredictor = new EyeDiseasePredictor(getAssets(), "eye-010-0.7412.tflite");
        } catch (IOException e) {
            showErrorAndFinish("AI 모델 로드 실패");
            return;
        }

        String leftUriStr = getIntent().getStringExtra("left_image_uri");
        String rightUriStr = getIntent().getStringExtra("right_image_uri");
        String petKey = getIntent().getStringExtra("pet_id");

        if (petKey == null) petKey = CurrentPetManager.getInstance().getCurrentPetId();
        if (petKey == null) {
            showErrorAndFinish("반려동물을 선택해 주세요.");
            return;
        }

        Bitmap leftBitmap = loadBitmap(leftUriStr);
        Bitmap rightBitmap = loadBitmap(rightUriStr);

        processAndSaveBothEyes(leftBitmap, leftUriStr, rightBitmap, rightUriStr, petKey);
    }

    private void showErrorAndFinish(String msg) {
        handler.removeCallbacks(dotAnimator);
        runOnUiThread(() -> {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private Bitmap loadBitmap(String uriStr) {
        if (uriStr == null) return null;
        try (InputStream is = getContentResolver().openInputStream(Uri.parse(uriStr))) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            return null;
        }
    }

    private void processAndSaveBothEyes(Bitmap leftBitmap, String leftUriStr,
                                        Bitmap rightBitmap, String rightUriStr, String petKey) {
        final float[][] leftResultHolder = {null};
        final float[][] rightResultHolder = {null};
        final int[] completed = {0};

        boolean hasLeft = leftBitmap != null;
        boolean hasRight = rightBitmap != null;
        int total = (hasLeft ? 1 : 0) + (hasRight ? 1 : 0);

        if (total == 0) {
            showErrorAndFinish("이미지를 선택해 주세요.");
            return;
        }

        if (hasLeft) {
            new Thread(() -> {
                float[] result = eyeDiseasePredictor.predict(ImageUtils.preprocess(leftBitmap, 224));
                logPrediction("왼쪽", result);
                saveToFirebase(leftUriStr, result, "left", petKey);
                leftResultHolder[0] = result;
                checkDone(completed, total, leftResultHolder[0], rightResultHolder[0]);
            }).start();
        }

        if (hasRight) {
            new Thread(() -> {
                float[] result = eyeDiseasePredictor.predict(ImageUtils.preprocess(rightBitmap, 224));
                logPrediction("오른쪽", result);
                saveToFirebase(rightUriStr, result, "right", petKey);
                rightResultHolder[0] = result;
                checkDone(completed, total, leftResultHolder[0], rightResultHolder[0]);
            }).start();
        }
    }

    private void checkDone(int[] completed, int total, float[] left, float[] right) {
        completed[0]++;
        if (completed[0] == total) {
            runOnUiThread(() -> {
                handler.removeCallbacks(dotAnimator);
                Intent intent = new Intent(this, EyeResultActivity.class);

                if (left != null) {
                    intent.putExtra("left_result", left);
                    intent.putExtra("left_image_uri", getIntent().getStringExtra("left_image_uri"));
                }
                if (right != null) {
                    intent.putExtra("right_result", right);
                    intent.putExtra("right_image_uri", getIntent().getStringExtra("right_image_uri"));
                }

                float[] avg = computeAverage(left, right);
                intent.putExtra("result", avg);

                float avgScore = calculateAverageScore(avg);
                int maxIdx = getMaxIndex(avg);
                String mainDiseaseKo = getLabelKo(maxIdx);

                EyeHistoryItem summary = new EyeHistoryItem(
                        getNow("yyyy.MM.dd(E) HH:mm"),
                        avgScore,
                        (left != null && right != null) ? "both" : (left != null ? "left" : "right"),
                        mainDiseaseKo
                );
                intent.putExtra("summary_item", summary);

                startActivity(intent);
                finish();
            });
        }
    }

    private float[] computeAverage(float[] left, float[] right) {
        if (left != null && right != null) {
            float[] avg = new float[left.length];
            for (int i = 0; i < left.length; i++) {
                avg[i] = (left[i] + right[i]) / 2f;
            }
            return avg;
        }
        return (left != null) ? left : right;
    }

    private float calculateAverageScore(float[] scores) {
        float sum = 0;
        for (float v : scores) sum += v;
        return sum / scores.length;
    }

    private void saveToFirebase(String uriStr, float[] result, String side, String petKey) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("Users").child(user.getUid()).child(petKey)
                .child("eyeAnalysis").push();

        Map<String, Object> data = new HashMap<>();
        data.put("imagePath", uriStr);
        data.put("side", side);

        // 날짜 포맷으로 저장
        String timestamp = new SimpleDateFormat("yyyy.MM.dd(EE) HH:mm", Locale.KOREAN).format(new java.util.Date());
        data.put("createdAt", timestamp);

        String[] keys = {"blepharitis", "eyelid_tumor", "entropion", "epiphora",
                "pigmentary_keratitis", "corneal_disease", "nuclear_sclerosis",
                "conjunctivitis", "nonulcerative_keratitis", "other"};

        Map<String, Object> prediction = new HashMap<>();
        for (int i = 0; i < result.length; i++) {
            prediction.put(keys[i], Math.round(result[i] * 100));
        }

        data.put("prediction", prediction);
        ref.setValue(data);
    }


    private void logPrediction(String label, float[] result) {
        Log.d("EyePrediction", "==== " + label + " 예측 결과 ====\n");
        String[] keys = {"blepharitis", "eyelid_tumor", "entropion", "epiphora",
                "pigmentary_keratitis", "corneal_disease", "nuclear_sclerosis",
                "conjunctivitis", "nonulcerative_keratitis", "other"};
        for (int i = 0; i < result.length; i++) {
            Log.d("EyePrediction", keys[i] + " = " + Math.round(result[i] * 100) + "%");
        }
    }

    private int getMaxIndex(float[] arr) {
        int idx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[idx]) idx = i;
        }
        return idx;
    }

    private String getLabelKo(int index) {
        String[] ko = {"안검염", "안검종양", "안검내반증", "유루증", "색소침착성각막염",
                "각막질환", "핵경화", "결막염", "비궤양성각막질환", "기타"};
        return ko[index];
    }

    private String getNow(String format) {
        return new java.text.SimpleDateFormat(format, java.util.Locale.KOREAN).format(new java.util.Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eyeDiseasePredictor != null) eyeDiseasePredictor.close();
        if (handler != null && dotAnimator != null) handler.removeCallbacks(dotAnimator);
    }
}