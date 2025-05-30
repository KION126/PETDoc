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

    private LinearLayout historySection;
    private TextView tabEye, tabSkin;
    private View divider;

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
        });

        selectTab(true);
        startEyeHistoryRealtimeListener();

        findViewById(R.id.eye_button).setOnClickListener(v -> {
            startActivity(new Intent(this, EyeCamActivity.class));
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
                    String leftUri = record.child("imagePath").getValue(String.class); // leftImageUri → imagePath

                    EyeHistoryItem item = new EyeHistoryItem(date, avg, side, "");
                    View card = inflater.inflate(R.layout.item_eye_history_card, historySection, false);

                    ((TextView) card.findViewById(R.id.historyDate)).setText(item.dateTime);
                    ((TextView) card.findViewById(R.id.historyTitle)).setText("종합 건강도");
                    ((TextView) card.findViewById(R.id.historyScore)).setText(String.format("%.0f%%", item.score * 100));

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
            @Override public void onCancelled(DatabaseError error) {}
        };
        eyeHistoryRef.addValueEventListener(eyeHistoryListener);
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