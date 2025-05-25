package com.petdoc.aiCheck.eye;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;
import com.petdoc.aiCheck.eye.model.EyeDiseasePredictor;
import com.petdoc.aiCheck.utils.ImageUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 안구 검진 로딩 페이지
 * - 선택된 안구 이미지를 통해 EyeDiseasePredictor 모델로 분석
 * - 분석 결과는 로그에 출력하며, 이후 DB 저장/화면 출력으로 확장 가능
 */
public class EyeLoadingActivity extends AppCompatActivity {

    // 안구 검진 모델
    private EyeDiseasePredictor eyeDiseasePredictor;

    // 분석 중 애니메이션 처리용
    private Handler handler;
    private Runnable dotAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_eye_loading);

        TextView processingText = findViewById(R.id.text_processing);

        // [1] 애니메이션 설정: 안구 분석 중... → .. → . 순환
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

        // [2] 모델 파일명 지정
        String modelPath = "eye-010-0.7412.tflite";

        try {
            eyeDiseasePredictor = new EyeDiseasePredictor(getAssets(), modelPath);
        } catch (IOException e) {
            Log.e("EyeLoadingActivity", "모델 초기화 실패", e);
            return;
        }

        // [3] 이미지 URI 받아오기 (권장 방식: 대용량 데이터 안전 전달)
        String uriString = getIntent().getStringExtra("image_uri");
        if (uriString == null) {
            Log.e("EyeLoadingActivity", "이미지 URI 누락");
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
            // [4] 전처리 및 예측
            float[] inputData = ImageUtils.preprocess(bitmap, 224);
            float[] result = eyeDiseasePredictor.predict(inputData);

            // [5] 예측 결과 출력 (확장 가능)
            Log.d("EyeLoadingActivity", "예측 결과: " + formatResult(result));

            // 애니메이션 중단
            handler.removeCallbacks(dotAnimator);

            // TODO: 분석 완료 후 결과 페이지 전환 or UI 업데이트

        } else {
            Log.e("EyeLoadingActivity", "Bitmap 복원 실패");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 자원 해제
        if (eyeDiseasePredictor != null) {
            eyeDiseasePredictor.close();
        }
        if (handler != null && dotAnimator != null) {
            handler.removeCallbacks(dotAnimator);
        }
    }

    /**
     * 예측 결과 배열을 문자열로 포맷
     */
    private String formatResult(float[] result) {
        StringBuilder sb = new StringBuilder();
        for (float v : result) {
            sb.append(String.format("[%.4f] ", v));
        }
        return sb.toString();
    }
}