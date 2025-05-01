package com.petdoc.aicheck.eye;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;
import com.petdoc.aicheck.eye.model.EyeDiseasePredictor;
import com.petdoc.aicheck.utils.ImageUtils;
import com.petdoc.aicheck.utils.ModelUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * 안구 검진 로딩 페이지
 * 해당 페이지에서 EyeDisesePredictor을 통해 출력 된 검진 결과를 FB에 저장
 */
public class EyeLoadingActivity extends AppCompatActivity {

    // 안구 검진 모델
    private EyeDiseasePredictor eyeDiseasePredictor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //setContentView(R.layout.);

        // 모델 파일명 정의
        String modelPath = "eye-010-0.7412.tflite";

        // 모델 초기화
        try {
            eyeDiseasePredictor = new EyeDiseasePredictor(getAssets(), modelPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // (테스트) 샘플 이미지 로딩
        Bitmap bitmap = ModelUtils.loadSampleImage(getAssets(), "img_sample_eye.jpg");

        // 이미지 로딩 성공 시 검진 진행
        if (bitmap != null) {
            // [1] 전처리: Bitmap -> float[] 변환
            float[] inputData = ImageUtils.preprocess(bitmap, 224);

            // [2] 예측 수행
            float[] result = eyeDiseasePredictor.predict(inputData);

            // [3] 결과 FB에 저장
        } else {
            Log.e("EyeLoadingActivity/BitmapLoad", "Bitmap 로드 실패");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 사용 후 EyeDiseasePredictor 자원 해제
        if (eyeDiseasePredictor != null) {
            eyeDiseasePredictor.close();
        }
    }

}
