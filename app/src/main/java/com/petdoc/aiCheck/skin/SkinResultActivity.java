package com.petdoc.aiCheck.skin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;

import com.bumptech.glide.Glide;
import com.petdoc.R;
import com.petdoc.aiCheck.AICheckActivity;
import com.petdoc.main.BaseActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SkinResultActivity extends BaseActivity {

    private static final String[] LABELS = {
            "papules_plaques",
            "dandruff_scaling_epidermal_collarette",
            "lichenification_hyperpigmentation",
            "pustules_acne",
            "erosion_ulceration",
            "nodules_mass"
    };

    private static final Map<String, String> LABELS_MAP = new HashMap<>();
    static {
        LABELS_MAP.put("papules_plaques", "구진,플라크");
        LABELS_MAP.put("dandruff_scaling_epidermal_collarette", "비듬,각질,상피성잔고리");
        LABELS_MAP.put("lichenification_hyperpigmentation", "태선화,과다색소침착");
        LABELS_MAP.put("pustules_acne", "농포,여드름");
        LABELS_MAP.put("erosion_ulceration", "미란,궤양");
        LABELS_MAP.put("nodules_mass", "결절,종괴");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_skin_result);

        //  뒤로 가기 버튼
        ImageButton backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, AICheckActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }

        // 날짜 표시
        TextView dateText = findViewById(R.id.result_date);
        String now = new SimpleDateFormat("yyyy.MM.dd(E) HH:mm", Locale.KOREAN).format(new Date());
        dateText.setText(now);

        // 결과 및 이미지 URI 수신
        Map<String, Integer> resultMap = (HashMap<String, Integer>) getIntent().getSerializableExtra("result_map");
        if (resultMap == null) return;
        String imageUriStr = getIntent().getStringExtra("image_uri");

        // 종합 점수 계산
        int total = 0;
        for (int score : resultMap.values()) total += score;
        int average = resultMap.size() > 0 ? total / resultMap.size() : 0;

        // 종합 점수 표시
        TextView summaryScore = findViewById(R.id.summaryScore);
        if (summaryScore != null) summaryScore.setText(average + "%");

        //  종합 이미지 표시
        ImageView summaryImage = findViewById(R.id.summary_image);
        if (summaryImage != null) {
            if (imageUriStr != null) {
                Glide.with(this).load(imageUriStr).into(summaryImage);
            } else {
                summaryImage.setImageResource(R.drawable.ic_camera_placeholder);
            }
        }

        // 결과 카드 목록 구성
        LinearLayout resultContainer = findViewById(R.id.skin_result_card_container);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String label : LABELS) {
            Integer score = resultMap.get(label);
            if (score == null) continue;

            String displayLabel = LABELS_MAP.getOrDefault(label, label);

            int color;
            if (score < 35) {
                color = 0xFF377DFF; // 안심
            } else if (score < 70) {
                color = 0xFF4CAF50; // 주의
            } else {
                color = 0xFFD32F2F; // 의심
            }

            View card = inflater.inflate(R.layout.item_skin_result_card, resultContainer, false);
            ((TextView) card.findViewById(R.id.condition_title)).setText(displayLabel);

            TextView scoreView = card.findViewById(R.id.condition_score);
            scoreView.setText(score + "%");
            scoreView.setTextColor(color);

            ImageView imageView = card.findViewById(R.id.condition_image);
            if (imageUriStr != null) {
                Glide.with(this).load(imageUriStr).into(imageView);
            } else {
                imageView.setImageResource(R.drawable.ic_camera_placeholder);
            }

            resultContainer.addView(card);
        }
    }
}
