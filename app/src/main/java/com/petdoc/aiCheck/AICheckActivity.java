package com.petdoc.aiCheck;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
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
                removeSkinHistoryListener();
                startEyeHistoryRealtimeListener();
            }
        });

        tabSkin.setOnClickListener(v -> {
            if (isEyeTabSelected) {
                isEyeTabSelected = false;
                selectTab(false);
                clearHistoryList();
                removeEyeHistoryListener();
                startSkinHistoryRealtimeListener();
            }
        });

        isEyeTabSelected = true;
        selectTab(true);
        startEyeHistoryRealtimeListener();

        findViewById(R.id.eye_button).setOnClickListener(v ->
                startActivity(new Intent(this, EyeCamActivity.class))
        );

        findViewById(R.id.skin_button).setOnClickListener(v ->
                startActivity(new Intent(this, SkinCamActivity.class))
        );
    }

    private void startEyeHistoryRealtimeListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        String petKey = CurrentPetManager.getInstance().getCurrentPetId();
        if (petKey == null) return;

        eyeHistoryRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child(petKey).child("eyeAnalysis");

        eyeHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
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

                // 기록 아이템 생성 및 화면에 추가
                for (DataSnapshot record : recordList) {
                    DataSnapshot predSnap = record.child("prediction");
                    if (!predSnap.exists()) continue;

                    float[] result = new float[LABELS.length];
                    float sum = 0f;
                    int count = 0;

                    // 예측값 계산
                    for (int i = 0; i < LABELS.length; i++) {
                        Float value = predSnap.child(LABELS[i]).getValue(Float.class);
                        if (value != null) {
                            result[i] = value / 100f;
                            sum += result[i];
                            count++;
                        }
                    }

                    float avg = count > 0 ? sum / count : 0f;
                    String date = record.child("createdAt").getValue(String.class);
                    if (date == null) date = getNow("yyyy.MM.dd(EE) HH:mm");
                    String side = record.child("side").getValue(String.class);
                    String uri = record.child("imagePath").getValue(String.class);

                    EyeHistoryItem item = new EyeHistoryItem(date, avg, side, "");

                    // 카드 뷰 생성 및 데이터 설정
                    View card = inflater.inflate(R.layout.item_eye_history_card, historySection, false);
                    ((TextView) card.findViewById(R.id.historyDate)).setText(item.dateTime);
                    ((TextView) card.findViewById(R.id.historyTitle)).setText("종합 안구 건강도");
                    ((TextView) card.findViewById(R.id.historyScore)).setText(String.format("%.0f%%", item.score * 100));

                    // 결과 상세 화면으로 이동하는 클릭 리스너 설정
                    card.setOnClickListener(view -> {
                        Intent intent = new Intent(AICheckActivity.this, EyeResultActivity.class);
                        intent.putExtra("summary_item", item);
                        intent.putExtra("left_result", result);
                        intent.putExtra("left_image_uri", uri);
                        intent.putExtra("right_image_uri", (String) null);
                        startActivity(intent);
                    });

                    historySection.addView(card);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // 데이터베이스 에러 발생 시 처리 로직 (필요 시 로깅 등 추가 가능)
            }
        };

        // 최초 1회 데이터만 불러오도록 설정
        eyeHistoryRef.addListenerForSingleValueEvent(eyeHistoryListener);
    }


    private void startSkinHistoryRealtimeListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        String petKey = CurrentPetManager.getInstance().getCurrentPetId();
        if (petKey == null) return;

        removeSkinHistoryListener();

        skinHistoryRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child(petKey).child("skinAnalysis");

        skinHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (isEyeTabSelected) return;
                clearHistoryList();

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

                    int avg = count > 0 ? sum / count : 0;

                    String date = record.child("createdAt").getValue(String.class);
                    if (date == null) date = getNow("yyyy.MM.dd(EE) HH:mm");

                    View card = inflater.inflate(R.layout.item_skin_history_card, historySection, false);
                    ((TextView) card.findViewById(R.id.historyDate)).setText(date);
                    ((TextView) card.findViewById(R.id.historyTitle)).setText("종합 피부 건강도");
                    ((TextView) card.findViewById(R.id.historyScore)).setText(avg + "%");

                    card.setOnClickListener(view -> {
                        Intent intent = new Intent(AICheckActivity.this, SkinResultActivity.class);
                        intent.putExtra("result_map", new HashMap<>(prediction));
                        intent.putExtra("image_uri", record.child("imagePath").getValue(String.class));
                        startActivity(intent);
                    });

                    historySection.addView(card);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // 데이터베이스 오류 발생 시 처리 필요하면 추가
            }
        };

        skinHistoryRef.addValueEventListener(skinHistoryListener);
    }

    private void removeEyeHistoryListener() {
        if (eyeHistoryRef != null && eyeHistoryListener != null) {
            eyeHistoryRef.removeEventListener(eyeHistoryListener);
            eyeHistoryListener = null;
        }
    }

    private void removeSkinHistoryListener() {
        if (skinHistoryRef != null && skinHistoryListener != null) {
            skinHistoryRef.removeEventListener(skinHistoryListener);
            skinHistoryListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeEyeHistoryListener();
        removeSkinHistoryListener();
    }

    private void selectTab(boolean isEye) {
        tabEye.setTypeface(null, isEye ? Typeface.BOLD : Typeface.NORMAL);
        tabSkin.setTypeface(null, isEye ? Typeface.NORMAL : Typeface.BOLD);
        tabEye.setTextColor(isEye ? 0xFF222222 : 0xFF888888);
        tabSkin.setTextColor(isEye ? 0xFF888888 : 0xFF222222);
        tabEye.setBackgroundResource(isEye ? R.drawable.tab_selected_bg : R.drawable.tab_unselected_bg);
        tabSkin.setBackgroundResource(!isEye ? R.drawable.tab_selected_bg : R.drawable.tab_unselected_bg);
    }

    private void clearHistoryList() {
        historySection.removeAllViews();
    }

    private String getNow(String format) {
        return new SimpleDateFormat(format, Locale.KOREAN).format(new Date());
    }
}