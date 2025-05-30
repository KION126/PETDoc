package com.petdoc.aiCheck;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.petdoc.R;
import com.petdoc.aiCheck.eye.EyeHistoryItem;
import com.petdoc.aiCheck.eye.EyeResultActivity;
import com.petdoc.aiCheck.eye.EyeCamActivity;
import com.petdoc.login.CurrentPetManager;
import com.petdoc.main.MainActivity;

import java.text.SimpleDateFormat;
import java.util.*;

public class AICheckActivity extends AppCompatActivity {

    // UI 요소
    private LinearLayout historySection;
    private TextView tabEye, tabSkin;
    private View divider;

    // Firebase 데이터 참조 및 리스너
    private DatabaseReference eyeHistoryRef;
    private ValueEventListener eyeHistoryListener;

    // 라벨 정의 (모델 키 & 한글)
    private static final String[] LABELS = {
            "blepharitis", "eyelid_tumor", "entropion", "epiphora",
            "pigmentary_keratitis", "corneal_disease", "nuclear_sclerosis",
            "conjunctivitis", "nonulcerative_keratitis", "other"
    };
    private static final String[] LABELS_KO = {
            "안검염", "안검종양", "안검내반증", "유루증", "색소침착성각막염",
            "각막질환", "핵경화", "결막염", "비궤양성각막질환", "기타"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai_check);

        // UI 초기화
        historySection = findViewById(R.id.historySection);
        tabEye = findViewById(R.id.tabEye);
        tabSkin = findViewById(R.id.tabSkin);
        divider = findViewById(R.id.divider);

        // 뒤로가기 → 메인화면으로 이동
        findViewById(R.id.backButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // 탭 클릭 이벤트 설정
        tabEye.setOnClickListener(v -> {
            selectTab(true);
            startEyeHistoryRealtimeListener();
        });
        tabSkin.setOnClickListener(v -> {
            selectTab(false);
            clearHistoryList();
            removeEyeHistoryListener(); // 현재는 스킨 기능 미구현
        });

        // 기본으로 안구 탭 선택
        selectTab(true);
        startEyeHistoryRealtimeListener();

        // 검사 시작 버튼 클릭 시
        findViewById(R.id.eye_button).setOnClickListener(v -> {
            startActivity(new Intent(this, EyeCamActivity.class));
        });
    }

    /**
     * 실시간 안구 분석 이력 리스너 시작
     */
    private void startEyeHistoryRealtimeListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        String petKey = CurrentPetManager.getInstance().getCurrentPetId();
        if (petKey == null) return;

        removeEyeHistoryListener(); // 중복 리스너 방지

        eyeHistoryRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child(petKey).child("eyeAnalysis");

        eyeHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                clearHistoryList(); // 기존 이력 제거
                LayoutInflater inflater = LayoutInflater.from(AICheckActivity.this);

                // 각 기록 카드 생성
                for (DataSnapshot record : snapshot.getChildren()) {
                    DataSnapshot predSnap = record.child("prediction");
                    if (!predSnap.exists()) continue;

                    float[] leftResult = new float[LABELS.length];
                    float[] rightResult = new float[LABELS.length]; // 기본은 null (현재는 한쪽만 저장됨)
                    float sum = 0f;
                    int count = 0;

                    // Firebase 예측 결과를 배열로 변환
                    for (int i = 0; i < LABELS.length; i++) {
                        Float value = predSnap.child(LABELS[i]).getValue(Float.class);
                        if (value != null) {
                            leftResult[i] = value / 100f;
                            sum += leftResult[i];
                            count++;
                        }
                    }

                    float avg = count > 0 ? sum / count : 0f;
                    String date = record.child("createdAt").getValue(String.class);
                    if (date == null) date = getNow("yyyy.MM.dd(EE) HH:mm");
                    String side = record.child("side").getValue(String.class);
                    String leftUri = record.child("imagePath").getValue(String.class);

                    EyeHistoryItem item = new EyeHistoryItem(date, avg, side, "");

                    // 카드 레이아웃 구성
                    View card = inflater.inflate(R.layout.item_eye_history_card, historySection, false);
                    ((TextView) card.findViewById(R.id.historyDate)).setText(item.dateTime);
                    ((TextView) card.findViewById(R.id.historyTitle)).setText("종합 건강도");
                    ((TextView) card.findViewById(R.id.historyScore)).setText(String.format("%.0f%%", item.score * 100));

                    // 카드 클릭 시 결과 화면으로 이동
                    card.setOnClickListener(view -> {
                        Intent intent = new Intent(AICheckActivity.this, EyeResultActivity.class);
                        intent.putExtra("summary_item", item);
                        intent.putExtra("left_result", leftResult);
                        intent.putExtra("right_result", rightResult); // 현재는 null
                        intent.putExtra("left_image_uri", leftUri);
                        intent.putExtra("right_image_uri", (String) null);
                        startActivity(intent);
                    });

                    historySection.addView(card);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        };

        eyeHistoryRef.addValueEventListener(eyeHistoryListener);
    }

    /**
     * 리스너 제거
     */
    private void removeEyeHistoryListener() {
        if (eyeHistoryRef != null && eyeHistoryListener != null) {
            eyeHistoryRef.removeEventListener(eyeHistoryListener);
            eyeHistoryListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeEyeHistoryListener();
    }

    /**
     * 탭 UI 상태 변경
     */
    private void selectTab(boolean isEye) {
        tabEye.setTypeface(null, isEye ? Typeface.BOLD : Typeface.NORMAL);
        tabSkin.setTypeface(null, isEye ? Typeface.NORMAL : Typeface.BOLD);
        tabEye.setTextColor(isEye ? 0xFF222222 : 0xFF888888);
        tabSkin.setTextColor(isEye ? 0xFF888888 : 0xFF222222);
        tabEye.setBackgroundResource(isEye ? R.drawable.tab_selected_bg : R.drawable.tab_unselected_bg);
        tabSkin.setBackgroundResource(!isEye ? R.drawable.tab_selected_bg : R.drawable.tab_unselected_bg);
    }

    /**
     * 히스토리 카드 제거 (기본 UI 요소 제외)
     */
    private void clearHistoryList() {
        int base = 3; // 기본 UI 요소 이후부터 제거
        while (historySection.getChildCount() > base)
            historySection.removeViewAt(base);
    }

    /**
     * 현재 시간 반환
     */
    private String getNow(String format) {
        return new SimpleDateFormat(format, Locale.KOREAN).format(new Date());
    }
}
