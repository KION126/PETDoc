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
import com.petdoc.aiCheck.eye.EyeCamActivity;
import com.petdoc.main.MainActivity;
import com.petdoc.login.CurrentPetManager;

import java.text.SimpleDateFormat;
import java.util.*;

public class AICheckActivity extends AppCompatActivity {

    private LinearLayout historySection;
    private TextView tabEye, tabSkin;
    private View divider;

    // 👇 리스너 관련 변수 선언
    private DatabaseReference eyeHistoryRef;
    private ValueEventListener eyeHistoryListener;

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

        // 기록 영역
        historySection = findViewById(R.id.historySection);

        // 안구/피부 탭
        tabEye = findViewById(R.id.tabEye);
        tabSkin = findViewById(R.id.tabSkin);

        divider = findViewById(R.id.divider);

        // 🔙 뒤로가기: MainActivity로 이동
        findViewById(R.id.backButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // 탭 리스너
        tabEye.setOnClickListener(v -> {
            selectTab(true);
            startEyeHistoryRealtimeListener(); // 실시간 리스너 등록
        });
        tabSkin.setOnClickListener(v -> {
            selectTab(false);
            clearHistoryList();
            removeEyeHistoryListener(); // 리스너 해제 (피부 탭일 땐 불필요)
        });

        // 첫 진입시 안구 기록 실시간 리스너
        selectTab(true);
        startEyeHistoryRealtimeListener();

        // "안구 AI 카메라" 버튼 클릭
        findViewById(R.id.eye_button).setOnClickListener(v -> {
            startActivity(new Intent(this, EyeCamActivity.class));
        });
        // (피부 버튼도 추가시 동일하게 연결)
    }

    /** 리스너 등록 (기존 리스너 해제 후 다시 등록) */
    private void startEyeHistoryRealtimeListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        String petKey = CurrentPetManager.getInstance().getCurrentPetId();
        if (petKey == null) return;

        // 기존 리스너 해제
        removeEyeHistoryListener();

        eyeHistoryRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child(petKey).child("eyeAnalysis");

        eyeHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<EyeHistoryItem> items = new ArrayList<>();
                for (DataSnapshot record : snapshot.getChildren()) {
                    DataSnapshot predSnap = record.child("prediction");
                    if (!predSnap.exists()) continue;

                    float max = -1f;
                    int maxIdx = 0, idx = 0;
                    for (String key : LABELS) {
                        Float v = predSnap.child(key).getValue(Float.class);
                        if (v == null) { idx++; continue; }
                        if (v > max) { max = v; maxIdx = idx; }
                        idx++;
                    }
                    String date = record.child("createdAt").getValue(String.class);
                    if (date == null) date = getNow("yyyy.MM.dd(EE) HH:mm");
                    String side = record.child("side").getValue(String.class); // "left"/"right"
                    items.add(new EyeHistoryItem(date, max, side, LABELS_KO[maxIdx]));
                }
                showHistory(items);
            }
            @Override public void onCancelled(DatabaseError error) {}
        };
        eyeHistoryRef.addValueEventListener(eyeHistoryListener);
    }

    /** 리스너 해제 함수 */
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

    /** 탭 토글 - true: 안구, false: 피부 */
    private void selectTab(boolean isEye) {
        tabEye.setTypeface(null, isEye ? Typeface.BOLD : Typeface.NORMAL);
        tabSkin.setTypeface(null, isEye ? Typeface.NORMAL : Typeface.BOLD);
        tabEye.setTextColor(isEye ? 0xFF222222 : 0xFF888888);
        tabSkin.setTextColor(isEye ? 0xFF888888 : 0xFF222222);
        tabEye.setBackgroundResource(isEye ? R.drawable.tab_selected_bg : R.drawable.tab_unselected_bg);
        tabSkin.setBackgroundResource(!isEye ? R.drawable.tab_selected_bg : R.drawable.tab_unselected_bg);
    }

    /** 기존 기록카드 모두 삭제 (타이틀, 탭, divider 3개만 남김) */
    private void clearHistoryList() {
        int base = 3; // 타이틀, 안구/피부 라벨(탭), divider
        while (historySection.getChildCount() > base)
            historySection.removeViewAt(base);
    }

    /** 기록 카드를 동적으로 추가 */
    private void showHistory(List<EyeHistoryItem> items) {
        clearHistoryList();

        LayoutInflater inflater = LayoutInflater.from(this);
        for (EyeHistoryItem item : items) {
            View card = inflater.inflate(R.layout.item_eye_history_card, historySection, false);
            ((TextView) card.findViewById(R.id.historyDate)).setText(item.dateTime);
            ((TextView) card.findViewById(R.id.historyTitle)).setText("종합 건강도");
            ((TextView) card.findViewById(R.id.historyScore)).setText(String.format("%.0f%%", item.score * 100));
            historySection.addView(card);
        }
    }

    /** 시간 포맷터 */
    private String getNow(String format) {
        return new SimpleDateFormat(format, Locale.KOREAN).format(new Date());
    }

    /** 기록 아이템 데이터 클래스 */
    public static class EyeHistoryItem {
        public String dateTime, side, labelKo;
        public float score;
        public EyeHistoryItem(String d, float s, String side, String labelKo) {
            this.dateTime = d; this.score = s; this.side = side; this.labelKo = labelKo;
        }
    }
}
