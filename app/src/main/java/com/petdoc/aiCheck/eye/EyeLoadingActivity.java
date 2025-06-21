package com.petdoc.aiCheck.eye;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView; // 이 클래스는 현재 코드에서 사용되지 않으므로 필요에 따라 제거 가능
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

import java.io.File; // 현재 코드에서 사용되지 않으므로 필요에 따라 제거 가능
import java.io.FileOutputStream; // 현재 코드에서 사용되지 않으므로 필요에 따라 제거 가능
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger; // 스레드 안전한 카운터 사용

public class EyeLoadingActivity extends BaseActivity {

    // EyeDiseasePredictor는 이제 스레드 로컬로 생성되므로 더 이상 멤버 변수로 유지하지 않습니다.
    // private EyeDiseasePredictor eyeDiseasePredictor; // 이 줄을 주석 처리하거나 삭제

    private Handler handler;
    private Runnable dotAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 전체 화면 UI 설정 (안드로이드 10 이상에서 시스템 바 영역까지 확장)
        EdgeToEdge.enable(this);
        // 레이아웃 설정
        setContentView(R.layout.activity_eye_loading);

        // UI 요소 초기화
        TextView processingText = findViewById(R.id.text_processing);

        // UI 업데이트를 위한 핸들러와 애니메이션 Runnable 초기화
        handler = new Handler(Looper.getMainLooper());
        dotAnimator = new Runnable() {
            int dotCount = 0; // 점 개수를 세는 변수
            @Override
            public void run() {
                dotCount = (dotCount + 1) % 4; // 0, 1, 2, 3 반복
                String dots = new String(new char[dotCount]).replace("\0", "."); // 점 문자열 생성
                processingText.setText("안구 분석 중" + dots); // 텍스트 업데이트
                handler.postDelayed(this, 500); // 0.5초마다 반복
            }
        };
        handler.post(dotAnimator); // 애니메이션 시작

        // 기존에는 여기서 모델을 로드했지만, 이제는 각 스레드에서 로드하므로 삭제합니다.
        /*
        try {
            eyeDiseasePredictor = new EyeDiseasePredictor(getAssets(), "eye-010-0.7412.tflite");
        } catch (IOException e) {
            showErrorAndFinish("AI 모델 로드 실패");
            return;
        }
        */

        // 인텐트로부터 이미지 URI와 반려동물 키를 가져옵니다.
        String leftUriStr = getIntent().getStringExtra("left_image_uri");
        String rightUriStr = getIntent().getStringExtra("right_image_uri");
        String petKey = getIntent().getStringExtra("pet_id");

        // 반려동물 키가 없을 경우 현재 선택된 반려동물 키를 사용합니다.
        if (petKey == null) petKey = CurrentPetManager.getInstance().getCurrentPetId();
        if (petKey == null) {
            // 반려동물 키가 없으면 에러 메시지를 표시하고 액티비티를 종료합니다.
            showErrorAndFinish("반려동물을 선택해 주세요.");
            return;
        }

        // 이미지 URI로부터 비트맵을 로드합니다.
        Bitmap leftBitmap = loadBitmap(leftUriStr);
        Bitmap rightBitmap = loadBitmap(rightUriStr);

        // 양쪽 눈 이미지 처리 및 Firebase 저장을 시작합니다.
        processAndSaveBothEyes(leftBitmap, leftUriStr, rightBitmap, rightUriStr, petKey);
    }

    /**
     * 오류 메시지를 토스트로 표시하고 현재 액티비티를 종료합니다.
     * @param msg 표시할 오류 메시지
     */
    private void showErrorAndFinish(String msg) {
        if (handler != null && dotAnimator != null) {
            handler.removeCallbacks(dotAnimator); // 애니메이션 중지
        }
        runOnUiThread(() -> {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); // 메인 스레드에서 토스트 메시지 표시
            finish(); // 액티비티 종료
        });
    }

    /**
     * 이미지 URI로부터 비트맵을 로드합니다.
     * @param uriStr 이미지 URI 문자열
     * @return 로드된 비트맵, 로드 실패 시 null
     */
    private Bitmap loadBitmap(String uriStr) {
        if (uriStr == null) return null;
        try (InputStream is = getContentResolver().openInputStream(Uri.parse(uriStr))) {
            return BitmapFactory.decodeStream(is);
        } catch (IOException e) {
            Log.e("EyeLoadingActivity", "비트맵 로드 실패: " + uriStr, e);
            return null;
        }
    }

    /**
     * 왼쪽 및 오른쪽 눈 이미지를 별도의 스레드에서 처리하고 Firebase에 저장합니다.
     * 각 스레드는 자체 EyeDiseasePredictor 인스턴스를 사용하여 동시성 문제를 방지합니다.
     * @param leftBitmap 왼쪽 눈 비트맵
     * @param leftUriStr 왼쪽 눈 이미지 URI
     * @param rightBitmap 오른쪽 눈 비트맵
     * @param rightUriStr 오른쪽 눈 이미지 URI
     * @param petKey 반려동물 키
     */
    private void processAndSaveBothEyes(Bitmap leftBitmap, String leftUriStr,
                                        Bitmap rightBitmap, String rightUriStr, String petKey) {
        final float[][] leftResultHolder = {null}; // 왼쪽 눈 예측 결과를 담을 배열
        final float[][] rightResultHolder = {null}; // 오른쪽 눈 예측 결과를 담을 배열
        // 스레드 안전한 카운터를 사용하여 완료된 작업 수를 추적
        final AtomicInteger completed = new AtomicInteger(0);

        boolean hasLeft = leftBitmap != null;
        boolean hasRight = rightBitmap != null;
        int total = (hasLeft ? 1 : 0) + (hasRight ? 1 : 0); // 처리할 총 이미지 수

        if (total == 0) {
            showErrorAndFinish("이미지를 선택해 주세요.");
            return;
        }

        // 왼쪽 눈 이미지 처리 스레드 시작
        if (hasLeft) {
            new Thread(() -> {
                EyeDiseasePredictor localPredictor = null; // 이 스레드에서만 사용할 로컬 예측기
                try {
                    // 각 스레드 내에서 새로운 EyeDiseasePredictor 인스턴스 생성
                    localPredictor = new EyeDiseasePredictor(getAssets(), "eye-010-0.7412.tflite");
                    Log.d("EyeLoadingActivity", "왼쪽 이미지 전처리 시작...");
                    float[] preprocessedLeft = ImageUtils.preprocess(leftBitmap, 224); // 이미지 전처리
                    if (preprocessedLeft == null) {
                        Log.e("EyeLoadingActivity", "왼쪽 이미지 전처리 실패: preprocessedLeft가 null입니다.");
                        showErrorAndFinish("왼쪽 이미지 처리 중 오류가 발생했습니다.");
                        return;
                    }
                    Log.d("EyeLoadingActivity", "왼쪽 이미지 전처리 완료. 배열 크기: " + preprocessedLeft.length);

                    float[] result = localPredictor.predict(preprocessedLeft); // 모델 예측 실행
                    logPrediction("왼쪽", result); // 예측 결과 로그 출력
                    leftResultHolder[0] = result; // 결과 저장
                    // 작업 완료 여부 확인 및 Firebase 저장/결과 화면 이동
                    checkDoneAndSaveToFirebase(completed, total, leftResultHolder[0], leftUriStr, rightResultHolder[0], rightUriStr, petKey);

                } catch (IOException e) {
                    Log.e("EyeLoadingActivity", "왼쪽 눈 예측기 로드 실패: " + e.getMessage(), e);
                    showErrorAndFinish("왼쪽 눈 분석을 위한 모델 로드 중 오류가 발생했습니다.");
                } catch (Exception e) { // 예측 중 발생할 수 있는 일반적인 예외 처리
                    Log.e("EyeLoadingActivity", "왼쪽 눈 예측 중 오류 발생: " + e.getMessage(), e);
                    showErrorAndFinish("왼쪽 눈 분석 중 오류가 발생했습니다.");
                } finally {
                    // 사용 후 반드시 리소스 해제
                    if (localPredictor != null) {
                        localPredictor.close();
                        Log.d("EyeLoadingActivity", "왼쪽 눈 예측기 리소스 해제 완료.");
                    }
                }
            }).start();
        }

        // 오른쪽 눈 이미지 처리 스레드 시작
        if (hasRight) {
            new Thread(() -> {
                EyeDiseasePredictor localPredictor = null; // 이 스레드에서만 사용할 로컬 예측기
                try {
                    // 각 스레드 내에서 새로운 EyeDiseasePredictor 인스턴스 생성
                    localPredictor = new EyeDiseasePredictor(getAssets(), "eye-010-0.7412.tflite");
                    Log.d("EyeLoadingActivity", "오른쪽 이미지 전처리 시작...");
                    float[] preprocessedRight = ImageUtils.preprocess(rightBitmap, 224); // 이미지 전처리
                    if (preprocessedRight == null) {
                        Log.e("EyeLoadingActivity", "오른쪽 이미지 전처리 실패: preprocessedRight가 null입니다.");
                        showErrorAndFinish("오른쪽 이미지 처리 중 오류가 발생했습니다.");
                        return;
                    }
                    Log.d("EyeLoadingActivity", "오른쪽 이미지 전처리 완료. 배열 크기: " + preprocessedRight.length);

                    float[] result = localPredictor.predict(preprocessedRight); // 모델 예측 실행
                    logPrediction("오른쪽", result); // 예측 결과 로그 출력
                    rightResultHolder[0] = result; // 결과 저장
                    // 작업 완료 여부 확인 및 Firebase 저장/결과 화면 이동
                    checkDoneAndSaveToFirebase(completed, total, leftResultHolder[0], leftUriStr, rightResultHolder[0], rightUriStr, petKey);

                } catch (IOException e) {
                    Log.e("EyeLoadingActivity", "오른쪽 눈 예측기 로드 실패: " + e.getMessage(), e);
                    showErrorAndFinish("오른쪽 눈 분석을 위한 모델 로드 중 오류가 발생했습니다.");
                } catch (Exception e) { // 예측 중 발생할 수 있는 일반적인 예외 처리
                    Log.e("EyeLoadingActivity", "오른쪽 눈 예측 중 오류 발생: " + e.getMessage(), e);
                    showErrorAndFinish("오른쪽 눈 분석 중 오류가 발생했습니다.");
                } finally {
                    // 사용 후 반드시 리소스 해제
                    if (localPredictor != null) {
                        localPredictor.close();
                        Log.d("EyeLoadingActivity", "오른쪽 눈 예측기 리소스 해제 완료.");
                    }
                }
            }).start();
        }
    }

    /**
     * 왼쪽/오른쪽 눈 이미지 처리가 모두 완료되었는지 확인하고, 완료되면 Firebase에 결과를 저장합니다.
     * 그 후 결과 화면으로 전환합니다.
     * @param completed 완료된 작업의 수를 추적하는 AtomicInteger
     * @param total 처리해야 할 총 작업 수
     * @param left 왼쪽 눈 예측 결과 배열
     * @param leftUriStr 왼쪽 눈 이미지 URI
     * @param right 오른쪽 눈 예측 결과 배열
     * @param rightUriStr 오른쪽 눈 이미지 URI
     * @param petKey 반려동물 키
     */
    private void checkDoneAndSaveToFirebase(AtomicInteger completed, int total,
                                            float[] left, String leftUriStr,
                                            float[] right, String rightUriStr,
                                            String petKey) {
        // AtomicInteger의 incrementAndGet()을 사용하여 스레드 안전하게 카운트 증가
        if (completed.incrementAndGet() == total) {
            // 모든 작업이 완료되었으면 메인 스레드에서 UI 업데이트 및 Firebase 저장
            runOnUiThread(() -> {
                // 애니메이션 중지
                if (handler != null && dotAnimator != null) {
                    handler.removeCallbacks(dotAnimator);
                }

                // 1. 파이어베이스에 한 번에 저장
                saveBothToFirebase(left, leftUriStr, right, rightUriStr, petKey);

                // 2. 결과 화면으로 이동
                Intent intent = new Intent(this, EyeResultActivity.class);

                // 왼쪽 눈 결과가 있으면 인텐트에 추가
                if (left != null) {
                    intent.putExtra("left_result", left);
                    intent.putExtra("left_image_uri", getIntent().getStringExtra("left_image_uri"));
                }
                // 오른쪽 눈 결과가 있으면 인텐트에 추가
                if (right != null) {
                    intent.putExtra("right_result", right);
                    intent.putExtra("right_image_uri", getIntent().getStringExtra("right_image_uri"));
                }

                // 평균 예측 결과 계산 및 인텐트에 추가
                float[] avg = computeAverage(left, right);
                intent.putExtra("result", avg);

                // 요약 정보 계산 및 인텐트에 추가
                float avgScore = calculateAverageScore(avg);
                int maxIdx = getMaxIndex(avg);
                String mainDiseaseKo = getLabelKo(maxIdx);

                // EyeHistoryItem 객체 생성하여 전달 (Parcelable 또는 Serializable 구현 필요)
                EyeHistoryItem summary = new EyeHistoryItem(
                        getNow("yyyy.MM.dd(E) HH:mm"),
                        avgScore,
                        (left != null && right != null) ? "both" : (left != null ? "left" : "right"),
                        mainDiseaseKo
                );
                intent.putExtra("summary_item", summary);

                // 결과 액티비티 시작 및 현재 액티비티 종료
                startActivity(intent);
                finish();
            });
        }
    }

    /**
     * 왼쪽과 오른쪽 눈 예측 결과를 평균 냅니다.
     * @param left 왼쪽 눈 예측 결과 배열
     * @param right 오른쪽 눈 예측 결과 배열
     * @return 평균 결과 배열 (둘 중 하나만 있으면 해당 결과를 반환)
     */
    private float[] computeAverage(float[] left, float[] right) {
        if (left != null && right != null) {
            float[] avg = new float[left.length];
            for (int i = 0; i < left.length; i++) {
                avg[i] = (left[i] + right[i]) / 2f;
            }
            return avg;
        }
        return (left != null) ? left : right; // 둘 중 하나만 있으면 그 결과 반환
    }

    /**
     * 예측 점수 배열의 평균을 계산합니다.
     * @param scores 예측 점수 배열
     * @return 평균 점수
     */
    private float calculateAverageScore(float[] scores) {
        if (scores == null || scores.length == 0) return 0.0f;
        float sum = 0;
        for (float v : scores) sum += v;
        return sum / scores.length;
    }

    /**
     * 왼쪽/오른쪽 눈 예측 결과를 Firebase Realtime Database에 저장합니다.
     * @param left 왼쪽 눈 예측 결과 배열
     * @param leftUriStr 왼쪽 눈 이미지 URI 문자열
     * @param right 오른쪽 눈 예측 결과 배열
     * @param rightUriStr 오른쪽 눈 이미지 URI 문자열
     * @param petKey 현재 선택된 반려동물의 키
     */
    private void saveBothToFirebase(float[] left, String leftUriStr,
                                    float[] right, String rightUriStr,
                                    String petKey) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e("EyeLoadingActivity", "Firebase 사용자 인증되지 않음. 데이터 저장 불가.");
            return;
        }

        // Firebase Realtime Database 경로 설정
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference()
                .child("Users").child(user.getUid()).child(petKey)
                .child("eyeAnalysis").push(); // 새로운 고유 키 생성

        Map<String, Object> data = new HashMap<>();
        // 현재 날짜 및 시간 저장
        String timestamp = new SimpleDateFormat("yyyy.MM.dd(EE) HH:mm", Locale.KOREAN).format(new java.util.Date());
        data.put("createdAt", timestamp);

        // 질병 라벨 키 배열 (모델 출력 순서와 일치해야 함)
        String[] keys = {"blepharitis", "eyelid_tumor", "entropion", "epiphora",
                "pigmentary_keratitis", "corneal_disease", "nuclear_sclerosis",
                "conjunctivitis", "nonulcerative_keratitis", "other"};

        // 왼쪽 눈 결과가 있으면 저장
        if (left != null) {
            Map<String, Object> leftObj = new HashMap<>();
            leftObj.put("imagePath", leftUriStr); // 이미지 URI 저장
            Map<String, Object> leftPrediction = new HashMap<>();
            for (int i = 0; i < left.length; i++) {
                leftPrediction.put(keys[i], Math.round(left[i] * 100)); // 확률을 100% 기준으로 정수화
            }
            leftObj.put("prediction", leftPrediction);
            data.put("left", leftObj);
        }
        // 오른쪽 눈 결과가 있으면 저장
        if (right != null) {
            Map<String, Object> rightObj = new HashMap<>();
            rightObj.put("imagePath", rightUriStr); // 이미지 URI 저장
            Map<String, Object> rightPrediction = new HashMap<>();
            for (int i = 0; i < right.length; i++) {
                rightPrediction.put(keys[i], Math.round(right[i] * 100)); // 확률을 100% 기준으로 정수화
            }
            rightObj.put("prediction", rightPrediction);
            data.put("right", rightObj);
        }

        // 최종 데이터 Firebase에 저장
        ref.setValue(data)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Eye analysis data saved successfully."))
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to save eye analysis data.", e));
    }

    /**
     * 예측 결과를 로그로 출력합니다.
     * @param label 눈의 구분 (예: "왼쪽", "오른쪽")
     * @param result 예측 결과 배열 (각 클래스별 확률)
     */
    private void logPrediction(String label, float[] result) {
        Log.d("EyePrediction", "==== " + label + " 예측 결과 ====\n");
        String[] keys = {"blepharitis", "eyelid_tumor", "entropion", "epiphora",
                "pigmentary_keratitis", "corneal_disease", "nuclear_sclerosis",
                "conjunctivitis", "nonulcerative_keratitis", "other"};
        for (int i = 0; i < result.length; i++) {
            Log.d("EyePrediction", keys[i] + " = " + Math.round(result[i] * 100) + "%");
        }
    }

    /**
     * 배열에서 가장 높은 값을 가진 인덱스를 찾습니다.
     * @param arr float 배열
     * @return 가장 높은 값의 인덱스
     */
    private int getMaxIndex(float[] arr) {
        int idx = 0;
        if (arr == null || arr.length == 0) return -1; // 빈 배열 예외 처리
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[idx]) idx = i;
        }
        return idx;
    }

    /**
     * 인덱스에 해당하는 질병의 한글 라벨을 반환합니다.
     * @param index 질병 인덱스
     * @return 해당 질병의 한글 이름
     */
    private String getLabelKo(int index) {
        String[] ko = {"안검염", "안검종양", "안검내반증", "유루증", "색소침착성각막염",
                "각막질환", "핵경화", "결막염", "비궤양성각막질환", "기타"};
        if (index >= 0 && index < ko.length) { // 유효성 검사 추가
            return ko[index];
        }
        return "알 수 없음"; // 유효하지 않은 인덱스 처리
    }

    /**
     * 현재 날짜와 시간을 지정된 형식으로 반환합니다.
     * @param format 날짜 형식 문자열 (예: "yyyy.MM.dd(E) HH:mm")
     * @return 형식화된 날짜/시간 문자열
     */
    private String getNow(String format) {
        return new java.text.SimpleDateFormat(format, java.util.Locale.KOREAN).format(new java.util.Date());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // EyeDiseasePredictor는 이제 각 스레드에서 생성되고 해제되므로 여기서 처리할 필요가 없습니다.
        // if (eyeDiseasePredictor != null) eyeDiseasePredictor.close(); // 이 줄을 삭제 또는 주석 처리

        // 핸들러와 애니메이션 콜백을 해제하여 메모리 누수 방지
        if (handler != null && dotAnimator != null) {
            handler.removeCallbacks(dotAnimator);
            Log.d("EyeLoadingActivity", "점 애니메이션 콜백 제거 완료.");
        }
    }
}