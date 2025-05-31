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
import com.petdoc.aiCheck.skin.SkinCamActivity;
import com.petdoc.aiCheck.skin.SkinResultActivity;
import com.petdoc.login.CurrentPetManager;
import com.petdoc.main.MainActivity;

import java.text.SimpleDateFormat;
import java.util.*;

public class AICheckActivity extends AppCompatActivity {

    private LinearLayout historySection;
    private TextView tabEye, tabSkin;
    private View divider;

    private DatabaseReference eyeHistoryRef;
    private ValueEventListener eyeHistoryListener;

    private DatabaseReference skinHistoryRef;
    private ValueEventListener skinHistoryListener;

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

        historySection = findViewById(R.id.historySection);
        tabEye = findViewById(R.id.tabEye);
        tabSkin = findViewById(R.id.tabSkin);
        divider = findViewById(R.id.divider);

        findViewById(R.id.backButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        tabEye.setOnClickListener(v -> {
            selectTab(true);
            startEyeHistoryRealtimeListener();
        });

        tabSkin.setOnClickListener(v -> {
            selectTab(false);
            clearHistoryList();
            removeEyeHistoryListener();
            startSkinHistoryRealtimeListener();
        });

        selectTab(true);
        startEyeHistoryRealtimeListener();

        findViewById(R.id.eye_button).setOnClickListener(v -> {
            startActivity(new Intent(this, EyeCamActivity.class));
        });

        findViewById(R.id.skin_button).setOnClickListener(v -> {
            startActivity(new Intent(this, SkinCamActivity.class));
        });
    }

    private void startEyeHistoryRealtimeListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        String petKey = CurrentPetManager.getInstance().getCurrentPetId();
        if (petKey == null) return;

        removeEyeHistoryListener();

        eyeHistoryRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child(petKey).child("eyeAnalysis");

        eyeHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                clearHistoryList();
                LayoutInflater inflater = LayoutInflater.from(AICheckActivity.this);

                for (DataSnapshot record : snapshot.getChildren()) {
                    DataSnapshot predSnap = record.child("prediction");
                    if (!predSnap.exists()) continue;

                    float[] leftResult = new float[LABELS.length];
                    float[] rightResult = new float[LABELS.length];
                    float sum = 0f;
                    int count = 0;

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

                    View card = inflater.inflate(R.layout.item_eye_history_card, historySection, false);
                    TextView dateView = card.findViewById(R.id.historyDate);
                    TextView titleView = card.findViewById(R.id.historyTitle);
                    TextView scoreView = card.findViewById(R.id.historyScore);

                    if (dateView != null) dateView.setText(item.dateTime);
                    if (titleView != null) titleView.setText("종합 안구 건강도");
                    if (scoreView != null) scoreView.setText(String.format("%.0f%%", item.score * 100));

                    card.setOnClickListener(view -> {
                        Intent intent = new Intent(AICheckActivity.this, EyeResultActivity.class);
                        intent.putExtra("summary_item", item);
                        intent.putExtra("left_result", leftResult);
                        intent.putExtra("right_result", rightResult);
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

    private void startSkinHistoryRealtimeListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        String petKey = CurrentPetManager.getInstance().getCurrentPetId();
        if (petKey == null) return;

        skinHistoryRef = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child(petKey).child("skinAnalysis");

        skinHistoryListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                clearHistoryList();
                LayoutInflater inflater = LayoutInflater.from(AICheckActivity.this);

                for (DataSnapshot record : snapshot.getChildren()) {
                    Map<String, Object> rawPrediction = (Map<String, Object>) record.child("prediction").getValue();
                    if (rawPrediction == null) continue;

                    Map<String, Integer> prediction = new HashMap<>();
                    int sum = 0;
                    int count = 0;
                    for (Map.Entry<String, Object> entry : rawPrediction.entrySet()) {
                        Object value = entry.getValue();
                        int intVal = 0;
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
                    TextView dateView = card.findViewById(R.id.historyDate);
                    TextView titleView = card.findViewById(R.id.historyTitle);
                    TextView scoreView = card.findViewById(R.id.historyScore);

                    if (dateView != null) dateView.setText(date);
                    if (titleView != null) titleView.setText("종합 피부 건강도");
                    if (scoreView != null) scoreView.setText(avg + "%");

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
            public void onCancelled(DatabaseError error) {}
        };

        skinHistoryRef.addValueEventListener(skinHistoryListener);
    }

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

    private void selectTab(boolean isEye) {
        tabEye.setTypeface(null, isEye ? Typeface.BOLD : Typeface.NORMAL);
        tabSkin.setTypeface(null, isEye ? Typeface.NORMAL : Typeface.BOLD);
        tabEye.setTextColor(isEye ? 0xFF222222 : 0xFF888888);
        tabSkin.setTextColor(isEye ? 0xFF888888 : 0xFF222222);
        tabEye.setBackgroundResource(isEye ? R.drawable.tab_selected_bg : R.drawable.tab_unselected_bg);
        tabSkin.setBackgroundResource(!isEye ? R.drawable.tab_selected_bg : R.drawable.tab_unselected_bg);
    }

    private void clearHistoryList() {
        int base = 3;
        while (historySection.getChildCount() > base)
            historySection.removeViewAt(base);
    }

    private String getNow(String format) {
        return new SimpleDateFormat(format, Locale.KOREAN).format(new Date());
    }
}
