package com.petdoc.aiCheck;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log; // Log import 추가
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.petdoc.R;
import com.petdoc.aiCheck.eye.EyeHistoryItem;
import com.petdoc.aiCheck.eye.EyeResultActivity;
import com.petdoc.aiCheck.eye.EyeCamActivity;
import com.petdoc.aiCheck.skin.SkinCamActivity;
import com.petdoc.aiCheck.skin.SkinResultActivity;
import com.petdoc.login.CurrentPetManager;
import com.petdoc.main.BaseActivity;
import com.petdoc.main.MainActivity;

import java.text.SimpleDateFormat;
import java.util.*;

public class AICheckActivity extends BaseActivity {

    private LinearLayout historySection;
    private TextView tabEye, tabSkin;
    private View divider;

    private DatabaseReference eyeHistoryRef;
    private ValueEventListener eyeHistoryListener;

    private DatabaseReference skinHistoryRef;
    private ValueEventListener skinHistoryListener;

    private boolean isEyeTabSelected = true;

    // 질병 라벨은 Firebase에서 읽어올 때 사용되므로 유지
    private static final String[] LABELS = {
            "blepharitis", "eyelid_tumor", "entropion", "epiphora",
            "pigmentary_keratitis", "corneal_disease", "nuclear_sclerosis",
            "conjunctivitis", "nonulcerative_keratitis", "other"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai_check);

        historySection = findViewById(R.id.historySection);
        tabEye = findViewById(R.id.tabEye);
        tabSkin = findViewById(R.id.tabSkin);
        divider = findViewById(R.id.divider);

        findViewById(R.id.backButton).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
            finish();
        });

        tabEye.setOnClickListener(v -> {
            if (!isEyeTabSelected) {
                isEyeTabSelected = true;
                selectTab(true);
                clearHistoryList();
                removeSkinHistoryListener(); // 스킨 리스너 제거
                startEyeHistoryRealtimeListener(); // 눈 이력 리스너 시작
            }
        });

        tabSkin.setOnClickListener(v -> {
            if (isEyeTabSelected) {
                isEyeTabSelected = false;
                selectTab(false);
                clearHistoryList();
                removeEyeHistoryListener(); // 눈 리스너 제거
                startSkinHistoryRealtimeListener(); // 피부 이력 리스너 시작
            }
        });

        // 초기 탭 선택 및 리스너 시작
        isEyeTabSelected = true;
        selectTab(true);
        startEyeHistoryRealtimeListener();

        // 진단 시작 버튼 리스너
        findViewById(R.id.eye_button).setOnClickListener(v ->
                startActivity(new Intent(this, EyeCamActivity.class))
        );

        findViewById(R.id.skin_button).setOnClickListener(v ->
                startActivity(new Intent(this, SkinCamActivity.class))
        );
    }

    /**
     * 눈 진단 이력을 Firebase에서 실시간으로 불러와 화면에 표시합니다.
     * Firebase 데이터 구조 변경에 맞춰 수정되었습니다.
     */
    private void startEyeHistoryRealtimeListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w("AICheckActivity", "Firebase 사용자 인증되지 않음.");
            return;
        }
        String uid = user.getUid();
        String petKey = CurrentPetManager.getInstance().getCurrentPetId();
        if (petKey == null) {
            Log.w("AICheckActivity", "현재 선택된 반려동물 없음.");
            return;
        }

        // 기존 리스너가 있다면 제거
        removeEyeHistoryListener();

        eyeHistoryRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child(petKey).child("eyeAnalysis");

        eyeHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // 현재 탭이 눈 탭이 아니면 업데이트를 건너뜁니다.
                if (!isEyeTabSelected) return;

                clearHistoryList(); // 기존 목록 초기화
                LayoutInflater inflater = LayoutInflater.from(AICheckActivity.this);
                List<DataSnapshot> recordList = new ArrayList<>();

                // 스냅샷을 리스트로 변환
                for (DataSnapshot record : snapshot.getChildren()) {
                    recordList.add(record);
                }

                // createdAt 기준으로 내림차순 정렬 (최신순)
                recordList.sort((a, b) -> {
                    String aTime = a.child("createdAt").getValue(String.class);
                    String bTime = b.child("createdAt").getValue(String.class);
                    if (aTime == null) aTime = "";
                    if (bTime == null) bTime = "";
                    return bTime.compareTo(aTime); // 최신순 정렬
                });

                // ... (이전 코드 생략)

                // 기록 아이템 생성 및 화면에 추가
                for (DataSnapshot record : recordList) {
                    DataSnapshot leftSnap = record.child("left");
                    DataSnapshot rightSnap = record.child("right");

                    float[] leftResult = null;
                    String leftImageUri = null;
                    if (leftSnap.exists()) {
                        leftImageUri = leftSnap.child("imagePath").getValue(String.class);
                        DataSnapshot leftPredSnap = leftSnap.child("prediction");
                        if (leftPredSnap.exists()) {
                            leftResult = new float[LABELS.length];
                            for (int i = 0; i < LABELS.length; i++) {
                                Long value = leftPredSnap.child(LABELS[i]).getValue(Long.class);
                                if (value != null) {
                                    leftResult[i] = value / 100f;
                                }
                            }
                        }
                    }

                    float[] rightResult = null;
                    String rightImageUri = null;
                    if (rightSnap.exists()) {
                        rightImageUri = rightSnap.child("imagePath").getValue(String.class);
                        DataSnapshot rightPredSnap = rightSnap.child("prediction");
                        if (rightPredSnap.exists()) {
                            rightResult = new float[LABELS.length];
                            for (int i = 0; i < LABELS.length; i++) {
                                Long value = rightPredSnap.child(LABELS[i]).getValue(Long.class);
                                if (value != null) {
                                    rightResult[i] = value / 100f;
                                }
                            }
                        }
                    }

                    // 왼쪽, 오른쪽 결과 중 하나라도 존재해야 이력으로 표시
                    if (leftResult == null && rightResult == null) {
                        Log.d("AICheckActivity", "유효한 눈 진단 결과 (left 또는 right) 없음: " + record.getKey());
                        continue;
                    }

                    // 람다에서 참조할 최종(effectively final) 변수 생성
                    // 현재 루프의 leftResult, rightResult, leftImageUri, rightImageUri 값을 복사합니다.
                    final float[] currentLeftResult = leftResult;
                    final float[] currentRightResult = rightResult;
                    final String currentLeftImageUri = leftImageUri;
                    final String currentRightImageUri = rightImageUri;

                    // EyeHistoryItem을 구성하기 위한 평균 점수 계산 (summary item용)
                    float[] combinedResult = combineResults(currentLeftResult, currentRightResult);
                    float avgScore = calculateAverageScore(combinedResult);
                    String date = record.child("createdAt").getValue(String.class);
                    if (date == null) date = getNow("yyyy.MM.dd(EE) HH:mm");

                    // 종합적인 진단 결과의 주된 질병 (가장 높은 확률) 찾기
                    int maxIdx = getMaxIndex(combinedResult);
                    String mainDiseaseKo = getLabelKo(maxIdx);
                    String sideSummary = getSideSummary(currentLeftResult, currentRightResult);

                    EyeHistoryItem item = new EyeHistoryItem(date, avgScore, sideSummary, mainDiseaseKo);

                    // 카드 뷰 생성 및 데이터 설정
                    View card = inflater.inflate(R.layout.item_eye_history_card, historySection, false);
                    ((TextView) card.findViewById(R.id.historyDate)).setText(item.dateTime);
                    ((TextView) card.findViewById(R.id.historyTitle)).setText("종합 안구 건강도");
                    ((TextView) card.findViewById(R.id.historyScore)).setText(String.format(Locale.getDefault(), "%.0f%%", item.score * 100));

                    // 결과 상세 화면으로 이동하는 클릭 리스너 설정
                    card.setOnClickListener(view -> {
                        Intent intent = new Intent(AICheckActivity.this, EyeResultActivity.class);
                        intent.putExtra("summary_item", item);
                        // 람다 안에서는 새로 선언한 final 변수를 사용합니다.
                        if (currentLeftResult != null) {
                            intent.putExtra("left_result", currentLeftResult);
                            intent.putExtra("left_image_uri", currentLeftImageUri);
                        }
                        if (currentRightResult != null) {
                            intent.putExtra("right_result", currentRightResult);
                            intent.putExtra("right_image_uri", currentRightImageUri);
                        }
                        startActivity(intent);
                    });

                    historySection.addView(card);
                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("AICheckActivity", "눈 진단 이력 불러오기 실패: " + error.getMessage());
            }
        };

        // Firebase 데이터 변경을 실시간으로 감지하기 위해 addValueEventListener 사용
        // (이전에는 addListenerForSingleValueEvent 였지만, 실시간 업데이트를 위해 변경)
        eyeHistoryRef.addValueEventListener(eyeHistoryListener);
    }

    /**
     * 피부 진단 이력을 Firebase에서 실시간으로 불러와 화면에 표시합니다.
     */
    private void startSkinHistoryRealtimeListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w("AICheckActivity", "Firebase 사용자 인증되지 않음.");
            return;
        }
        String uid = user.getUid();
        String petKey = CurrentPetManager.getInstance().getCurrentPetId();
        if (petKey == null) {
            Log.w("AICheckActivity", "현재 선택된 반려동물 없음.");
            return;
        }

        removeSkinHistoryListener(); // 기존 리스너 제거

        skinHistoryRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child(petKey).child("skinAnalysis");

        skinHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (isEyeTabSelected) return; // 현재 탭이 피부 탭이 아니면 업데이트를 건너뜁니다.
                clearHistoryList(); // 기존 목록 초기화

                LayoutInflater inflater = LayoutInflater.from(AICheckActivity.this);

                List<DataSnapshot> recordList = new ArrayList<>();
                // 스냅샷을 리스트로 변환
                for (DataSnapshot record : snapshot.getChildren()) {
                    recordList.add(record);
                }

                // createdAt 기준 내림차순 정렬 (최신순)
                recordList.sort((a, b) -> {
                    String aTime = a.child("createdAt").getValue(String.class);
                    String bTime = b.child("createdAt").getValue(String.class);
                    if (aTime == null) aTime = "";
                    if (bTime == null) bTime = "";
                    return bTime.compareTo(aTime);
                });

                // 정렬된 리스트를 기준으로 기록 화면에 추가
                for (DataSnapshot record : recordList) {
                    Map<String, Object> rawPrediction = (Map<String, Object>) record.child("prediction").getValue();
                    if (rawPrediction == null) continue;

                    Map<String, Integer> prediction = new HashMap<>();
                    int sum = 0;
                    int count = 0;
                    for (Map.Entry<String, Object> entry : rawPrediction.entrySet()) {
                        int intVal = 0;
                        Object value = entry.getValue();
                        if (value instanceof Long) intVal = ((Long) value).intValue();
                        else if (value instanceof Integer) intVal = (Integer) value;
                        prediction.put(entry.getKey(), intVal);
                        sum += intVal;
                        count++;
                    }

                    int avg = count > 0 ? sum / count : 0; // 피부는 퍼센트 값으로 바로 저장되므로 100으로 나눌 필요 없음

                    String date = record.child("createdAt").getValue(String.class);
                    if (date == null) date = getNow("yyyy.MM.dd(EE) HH:mm");

                    View card = inflater.inflate(R.layout.item_skin_history_card, historySection, false);
                    ((TextView) card.findViewById(R.id.historyDate)).setText(date);
                    ((TextView) card.findViewById(R.id.historyTitle)).setText("종합 피부 건강도");
                    ((TextView) card.findViewById(R.id.historyScore)).setText(avg + "%");

                    card.setOnClickListener(view -> {
                        Intent intent = new Intent(AICheckActivity.this, SkinResultActivity.class);
                        // prediction 맵을 HashMap으로 전달 (Serializable)
                        intent.putExtra("result_map", new HashMap<>(prediction));
                        intent.putExtra("image_uri", record.child("imagePath").getValue(String.class));
                        startActivity(intent);
                    });

                    historySection.addView(card);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("AICheckActivity", "피부 진단 이력 불러오기 실패: " + error.getMessage());
            }
        };

        // Firebase 데이터 변경을 실시간으로 감지하기 위해 addValueEventListener 사용
        skinHistoryRef.addValueEventListener(skinHistoryListener);
    }

    /**
     * 눈 진단 이력 리스너를 제거합니다.
     */
    private void removeEyeHistoryListener() {
        if (eyeHistoryRef != null && eyeHistoryListener != null) {
            eyeHistoryRef.removeEventListener(eyeHistoryListener);
            eyeHistoryListener = null;
            Log.d("AICheckActivity", "Eye history listener removed.");
        }
    }

    /**
     * 피부 진단 이력 리스너를 제거합니다.
     */
    private void removeSkinHistoryListener() {
        if (skinHistoryRef != null && skinHistoryListener != null) {
            skinHistoryRef.removeEventListener(skinHistoryListener);
            skinHistoryListener = null;
            Log.d("AICheckActivity", "Skin history listener removed.");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 액티비티 종료 시 모든 리스너 제거
        removeEyeHistoryListener();
        removeSkinHistoryListener();
    }

    /**
     * 선택된 탭의 UI를 업데이트합니다.
     * @param isEye 눈 탭이 선택되었는지 여부
     */
    private void selectTab(boolean isEye) {
        tabEye.setTypeface(null, isEye ? Typeface.BOLD : Typeface.NORMAL);
        tabSkin.setTypeface(null, isEye ? Typeface.NORMAL : Typeface.BOLD);
        tabEye.setTextColor(isEye ? 0xFF222222 : 0xFF888888); // 선택된 탭은 진한 색, 아닌 탭은 연한 색
        tabSkin.setTextColor(isEye ? 0xFF888888 : 0xFF222222);
        tabEye.setBackgroundResource(isEye ? R.drawable.tab_selected_bg : R.drawable.tab_unselected_bg); // 선택된 탭 배경
        tabSkin.setBackgroundResource(!isEye ? R.drawable.tab_selected_bg : R.drawable.tab_unselected_bg);
    }

    /**
     * 이력 목록을 초기화합니다.
     */
    private void clearHistoryList() {
        historySection.removeAllViews();
    }

    /**
     * 현재 날짜와 시간을 지정된 형식으로 반환합니다.
     * @param format 날짜 형식 문자열
     * @return 형식화된 날짜 문자열
     */
    private String getNow(String format) {
        return new SimpleDateFormat(format, Locale.KOREAN).format(new Date());
    }

    /**
     * 두 눈의 예측 결과를 합쳐서 하나의 float 배열로 만듭니다.
     * 한쪽 눈만 결과가 있다면 그 결과를 반환합니다.
     * @param leftResult 왼쪽 눈 예측 결과
     * @param rightResult 오른쪽 눈 예측 결과
     * @return 합쳐진 또는 단일 눈의 예측 결과 배열
     */
    private float[] combineResults(float[] leftResult, float[] rightResult) {
        if (leftResult != null && rightResult != null) {
            float[] combined = new float[LABELS.length];
            for (int i = 0; i < LABELS.length; i++) {
                combined[i] = (leftResult[i] + rightResult[i]) / 2f;
            }
            return combined;
        } else if (leftResult != null) {
            return leftResult;
        } else if (rightResult != null) {
            return rightResult;
        }
        return null; // 둘 다 null인 경우
    }

    /**
     * 예측 점수 배열의 평균을 계산합니다.
     * @param scores 예측 점수 배열
     * @return 평균 점수 (0.0f ~ 1.0f)
     */
    private float calculateAverageScore(float[] scores) {
        if (scores == null || scores.length == 0) return 0.0f;
        float sum = 0;
        for (float v : scores) sum += v;
        return sum / scores.length;
    }

    /**
     * 배열에서 가장 높은 값을 가진 인덱스를 찾습니다.
     * @param arr float 배열
     * @return 가장 높은 값의 인덱스, 배열이 null 또는 비어있으면 -1
     */
    private int getMaxIndex(float[] arr) {
        if (arr == null || arr.length == 0) return -1;
        int idx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[idx]) idx = i;
        }
        return idx;
    }

    /**
     * 인덱스에 해당하는 질병의 한글 라벨을 반환합니다.
     * LABELS_KO와 동일한 질병 순서로 가정합니다.
     * @param index 질병 인덱스
     * @return 해당 질병의 한글 이름, 유효하지 않은 인덱스인 경우 "알 수 없음"
     */
    private String getLabelKo(int index) {
        String[] ko = {"안검염", "안검종양", "안검내반증", "유루증", "색소침착성각막염",
                "각막질환", "핵경화", "결막염", "비궤양성각막질환", "기타"};
        if (index >= 0 && index < ko.length) {
            return ko[index];
        }
        return "알 수 없음";
    }

    /**
     * 왼쪽/오른쪽 눈 중 어떤 눈이 진단되었는지에 따라 요약 문자열을 반환합니다.
     * @param leftResult 왼쪽 눈 결과 배열
     * @param rightResult 오른쪽 눈 결과 배열
     * @return "both", "left", "right" 중 하나
     */
    private String getSideSummary(float[] leftResult, float[] rightResult) {
        if (leftResult != null && rightResult != null) {
            return "both";
        } else if (leftResult != null) {
            return "left";
        } else if (rightResult != null) {
            return "right";
        }
        return ""; // 둘 다 없는 경우
    }
}