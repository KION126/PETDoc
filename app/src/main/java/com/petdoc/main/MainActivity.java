package com.petdoc.main;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.petdoc.R;
import com.petdoc.main.model.EyeDiseasePredictor;

import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity {

    private EyeDiseasePredictor eyeDiseasePredictor;
    private TextView resultTextView;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        resultTextView = findViewById(R.id.resultTextView);
        imageView = findViewById(R.id.imageView);

        // Firestore 연동 확인
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (db != null) {
            Log.d("Firestore", "Firestore 연동 성공!");
        } else {
            Log.w("Firestore", "Firestore 연동 실패...");
        }

        String modelPath = "eye-010-0.7412.tflite";

        try {
            // EyeDiseasePredictor 초기화
            eyeDiseasePredictor = new EyeDiseasePredictor(getAssets(), modelPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 비트맵 변수를 try 블록 밖에서 선언
        Bitmap bitmap = null;

        // InputStream을 열고 비트맵으로 변환
        InputStream inputStream = null;
        try {
            inputStream = getAssets().open("img_sample_eye.jpg");
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // 비트맵이 제대로 로드되었으면 예측을 진행
        if (bitmap != null) {
            // 이미지 전처리 및 변환
            float[] inputData = preprocessImage(bitmap);  // 1D 배열 반환

            // 예측 결과 얻기
            float[] result = eyeDiseasePredictor.predict(inputData);

            // 예측 결과를 UI에 출력
            displayResults(result);
        } else {
            Log.e("BitmapError", "Bitmap을 로드하는 데 실패했습니다.");
        }
    }


    private float[] preprocessImage(Bitmap bitmap) {
        int size = 224;  // 모델에 맞는 크기
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);

        // 입력 데이터 형태: (1, 224, 224, 3) -> 이를 평탄화하여 1D 배열로 변환
        float[] inputData = new float[size * size * 3];  // 1D 배열로 선언 (224 * 224 * 3)

        // 각 픽셀의 색상 값을 float 배열로 변환 (정규화)
        int index = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int pixel = resizedBitmap.getPixel(i, j);
                inputData[index++] = ((float) ((pixel >> 16) & 0xFF)) / 255.0f;  // R
                inputData[index++] = ((float) ((pixel >> 8) & 0xFF)) / 255.0f;   // G
                inputData[index++] = ((float) (pixel & 0xFF)) / 255.0f;           // B
            }
        }

        return inputData;  // 1D 배열을 반환
    }



    private void displayResults(float[] result) {
        // 모델 예측 결과를 화면에 표시
        StringBuilder resultText = new StringBuilder("Predicted Disease: \n");

        // 예시로 상위 3개의 질병을 출력
        for (int i = 0; i < result.length; i++) {
            resultText.append("Class " + (i + 1) + ": " + result[i] + "\n");
        }

        resultTextView.setText(resultText.toString());
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
