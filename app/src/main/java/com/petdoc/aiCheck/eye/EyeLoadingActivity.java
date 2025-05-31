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
 * EyeLoadingActivity
 * - 안구 이미지 분석 로딩 화면
 * - 이미지 예측 및 Firebase 저장 후 결과 화면으로 이동
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
        ImageView backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        // 로딩 텍스트에 점(.) 애니메이션 적용
        handler = new Handler(Looper.getMainLooper());
        dotAnimator = new Runnable() {
            int dotCount = 0;
            @Override
            public void run() {
                dotCount = (dotCount + 1) % 4;
                String dots = new String(new char[dotCount]).replace("\0", ".");
                processingText.setText("안구 분석 중" + dots);
                handler.postDelayed(this, 100);
            }
        };
        handler.post(dotAnimator);

        try {
            // TFLite 모델 로딩
            eyeDiseasePredictor = new EyeDiseasePredictor(getAssets(), "eye-010-0.7412.tflite");
        } catch (IOException e) {
            showErrorAndFinish("AI 모델 로드 실패");
            return;
        }

        // 전달받은 이미지 URI 및 반려동물 ID
        String leftUriStr = getIntent().getStringExtra("left_image_uri");
        String rightUriStr = getIntent().getStringExtra("right_image_uri");
        String petKey = getIntent().getStringExtra("pet_id");

        // petKey 없으면 CurrentPetManager 에서 가져옴
        if (petKey == null) petKey = CurrentPetManager.getInstance().getCurrentPetId();
        if (petKey == null) {
            showErrorAndFinish("반려동물을 선택해 주세요.");
            return;
        }

        // URI → Bitmap 변환
        Bitmap leftBitmap = loadBitmap(leftUriStr);
        Bitmap rightBitmap = loadBitmap(rightUriStr);

        // 이미지 분석 및 Firebase 저장
        processAndSaveBothEyes(leftBitmap, leftUriStr, rightBitmap, rightUriStr, petKey);
    }

    // 에러 표시 후 종료
    private void showErrorAndFinish(String msg) {
        handler.removeCallbacks(dotAnimator);
        runOnUiThread(() -> {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // URI 문자열 → Bitmap 객체 변환
    private Bitmap loadBitmap(String uriStr) {
        if (uriStr == null) return null;
        try (InputStream is = getContentResolver().openInputStream(Uri.parse(uriStr))) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            return null;
        }
    }

    // 좌/우 이미지 분석 및 저장 (스레드 분기 처리)
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

        // 왼쪽 이미지 처리
        if (hasLeft) {
            new Thread(() -> {
                float[] result = eyeDiseasePredictor.predict(ImageUtils.preprocess(leftBitmap, 224));
                logPrediction("왼쪽", result);
                saveToFirebase(Uri.parse(leftUriStr), result, "left", petKey);
                leftResultHolder[0] = result;
                checkDone(completed, total, leftResultHolder[0], rightResultHolder[0]);
            }).start();
        }

        // 오른쪽 이미지 처리
        if (hasRight) {
            new Thread(() -> {
                float[] result = eyeDiseasePredictor.predict(ImageUtils.preprocess(rightBitmap, 224));
                logPrediction("오른쪽", result);
                saveToFirebase(Uri.parse(rightUriStr), result, "right", petKey);
                rightResultHolder[0] = result;
                checkDone(completed, total, leftResultHolder[0], rightResultHolder[0]);
            }).start();
        }
    }

    // 이미지 분석 완료 후 결과 화면으로 이동
    private void checkDone(int[] completed, int total, float[] left, float[] right) {
        completed[0]++;
        if (completed[0] == total) {
            runOnUiThread(() -> {
                handler.removeCallbacks(dotAnimator);
                Intent intent = new Intent(this, EyeResultActivity.class);

                // 결과 전달
                if (left != null) {
                    intent.putExtra("left_result", left);
                    intent.putExtra("left_image_uri", getIntent().getStringExtra("left_image_uri"));
                }
                if (right != null) {
                    intent.putExtra("right_result", right);
                    intent.putExtra("right_image_uri", getIntent().getStringExtra("right_image_uri"));
                }

                // 평균 결과 계산
                float[] avg = computeAverage(left, right);
                intent.putExtra("result", avg);

                // 평균 점수 및 주요 질병 추출
                float avgScore = calculateAverageScore(avg);
                int maxIdx = getMaxIndex(avg);
                String mainDiseaseKo = getLabelKo(maxIdx);

                // 종합 진단 요약 객체 생성
                EyeHistoryItem summary = new EyeHistoryItem(
                        getNow("yyyy.MM.dd(E) HH:mm"),
                        avgScore,
                        (left != null && right != null) ? "both" : (left != null ? "left" : "right"),
                        mainDiseaseKo
                );
                intent.putExtra("summary_item", summary);

                Log.d("EyePrediction", "종합 평균 점수: " + avgScore);

                startActivity(intent);
                finish();
            });
        }
    }

    // 좌우 결과 평균 계산
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

    // 평균 점수 계산 (전체 질병 확률 평균)
    private float calculateAverageScore(float[] scores) {
        float sum = 0;
        for (float v : scores) sum += v;
        return sum / scores.length;
    }

    // Firebase Realtime Database에 예측 결과 저장
    private void saveToFirebase(Uri uri, float[] result, String side, String petKey) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("Users").child(user.getUid()).child(petKey)
                .child("eyeAnalysis").push();

        Map<String, Object> data = new HashMap<>();
        data.put("imagePath", uri.toString());
        data.put("side", side);
        data.put("timestamp", System.currentTimeMillis());

        String[] keys = {"blepharitis", "eyelid_tumor", "entropion", "epiphora",
                "pigmentary_keratitis", "corneal_disease", "nuclear_sclerosis",
                "conjunctivitis", "nonulcerative_keratitis", "other"};

        Map<String, Object> prediction = new HashMap<>();
        for (int i = 0; i < result.length; i++) {
            prediction.put(keys[i], Math.round(result[i] * 100)); // 정수화 (%)
        }

        data.put("prediction", prediction);
        ref.setValue(data);
    }

    // 예측 결과 로그 출력
    private void logPrediction(String label, float[] result) {
        Log.d("EyePrediction", "==== " + label + " 예측 결과 ====");
        String[] keys = {"blepharitis", "eyelid_tumor", "entropion", "epiphora",
                "pigmentary_keratitis", "corneal_disease", "nuclear_sclerosis",
                "conjunctivitis", "nonulcerative_keratitis", "other"};
        for (int i = 0; i < result.length; i++) {
            Log.d("EyePrediction", keys[i] + " = " + Math.round(result[i] * 100) + "%");
        }
    }

    // 가장 높은 확률 질병 인덱스 반환
    private int getMaxIndex(float[] arr) {
        int idx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[idx]) idx = i;
        }
        return idx;
    }

    // 질병 한글 라벨 반환
    private String getLabelKo(int index) {
        String[] ko = {"안검염", "안검종양", "안검내반증", "유루증", "색소침착성각막염",
                "각막질환", "핵경화", "결막염", "비궤양성각막질환", "기타"};
        return ko[index];
    }

    // 현재 시간 문자열 반환
    private String getNow(String format) {
        return new java.text.SimpleDateFormat(format, java.util.Locale.KOREAN).format(new java.util.Date());
    }

    // 액티비티 종료 시 리소스 정리
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (eyeDiseasePredictor != null) eyeDiseasePredictor.close();
        if (handler != null && dotAnimator != null) handler.removeCallbacks(dotAnimator);
    }

    // 추후 확장용 콜백 인터페이스 (미사용)
    private interface HistorySaveCallback {
        void onSaved(String historyId);
    }
}
