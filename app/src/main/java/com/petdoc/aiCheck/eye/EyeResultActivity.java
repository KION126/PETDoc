package com.petdoc.aiCheck.eye;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.petdoc.R;
import com.petdoc.main.BaseActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * EyeResultActivity
 * - 안구 진단 결과를 시각적으로 보여주는 화면
 * - 전체 건강도 요약, 지병별 확률, 이미지 미리보기 등을 표시
 */
public class EyeResultActivity extends BaseActivity {

    private static final String[] LABELS = {
            "blepharitis", "eyelid_tumor", "entropion", "epiphora",
            "pigmentary_keratitis", "corneal_disease", "nuclear_sclerosis",
            "conjunctivitis", "nonulcerative_keratitis", "other"
    };

    private static final String[] LABELS_KO = {
            "안감엔", "안감종양", "안감내반증", "유루증", "색소치착성갑림엔",
            "각림질학", "향가화", "결림엔", "비귀양성각림질학", "기타"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_eye_result);

        findViewById(R.id.back_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, com.petdoc.aiCheck.AICheckActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        TextView dateView = findViewById(R.id.result_date);
        dateView.setText(getNow("yyyy.MM.dd(E) HH:mm"));

        float[] leftResult = (float[]) getIntent().getSerializableExtra("left_result");
        float[] rightResult = (float[]) getIntent().getSerializableExtra("right_result");
        EyeHistoryItem summary = (EyeHistoryItem) getIntent().getSerializableExtra("summary_item");

        Uri leftEyeUri = parseUri(getIntent().getStringExtra("left_image_uri"));
        Uri rightEyeUri = parseUri(getIntent().getStringExtra("right_image_uri"));

        View summaryBlock = findViewById(R.id.summary_block_wrapper);
        if (summaryBlock != null && summary != null) {
            TextView summaryTitle = summaryBlock.findViewById(R.id.summary_title);
            TextView leftScore = summaryBlock.findViewById(R.id.left_score);
            TextView rightScore = summaryBlock.findViewById(R.id.right_score);
            ImageView leftImg = summaryBlock.findViewById(R.id.left_eye_image);
            ImageView rightImg = summaryBlock.findViewById(R.id.right_eye_image);

            summaryTitle.setText("종합 안구 건강도");

            int avgPercent = Math.round(summary.score * 100);
            int color = getStatusColor(getStatus(summary.score));

            if ("left".equals(summary.side) || "both".equals(summary.side)) {
                leftScore.setText(avgPercent + "%");
                leftScore.setTextColor(color);
            } else {
                leftScore.setText("선택안함");
                leftScore.setTextColor(getStatusColor("선택안함"));
            }

            if ("right".equals(summary.side) || "both".equals(summary.side)) {
                rightScore.setText(avgPercent + "%");
                rightScore.setTextColor(color);
            } else {
                rightScore.setText("선택안함");
                rightScore.setTextColor(getStatusColor("선택안함"));
            }

            if (leftEyeUri != null) Glide.with(this).load(leftEyeUri).into(leftImg);
            if (rightEyeUri != null) Glide.with(this).load(rightEyeUri).into(rightImg);
        }

        LinearLayout cardContainer = findViewById(R.id.result_card_container);

        for (int i = 0; i < LABELS.length; i++) {
            View card = getLayoutInflater().inflate(R.layout.item_eye_result_card, cardContainer, false);

            TextView label = card.findViewById(R.id.eye_part_label);
            TextView leftScore = card.findViewById(R.id.left_eye_value);
            TextView rightScore = card.findViewById(R.id.right_eye_value);
            ImageView leftImg = card.findViewById(R.id.left_eye_image);
            ImageView rightImg = card.findViewById(R.id.right_eye_image);

            label.setText(LABELS_KO[i]);

            if (leftResult != null && i < leftResult.length) {
                int percent = Math.round(leftResult[i] * 100);
                leftScore.setText(percent + "%");
                leftScore.setTextColor(getStatusColor(getStatus(leftResult[i])));
            } else {
                leftScore.setText("선택안함");
                leftScore.setTextColor(getStatusColor("선택안함"));
            }

            if (rightResult != null && i < rightResult.length) {
                int percent = Math.round(rightResult[i] * 100);
                rightScore.setText(percent + "%");
                rightScore.setTextColor(getStatusColor(getStatus(rightResult[i])));
            } else {
                rightScore.setText("\uc120\ud0dd\uc548\ud568");
                rightScore.setTextColor(getStatusColor("\uc120\ud0dd\uc548\ud568"));
            }

            if (leftEyeUri != null) Glide.with(this).load(leftEyeUri).into(leftImg);
            if (rightEyeUri != null) Glide.with(this).load(rightEyeUri).into(rightImg);

            cardContainer.addView(card);
        }
    }

    private String getStatus(float v) {
        if (v >= 0.6f) return "의심";
        else if (v >= 0.3f) return "주의";
        else return "안심";
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "의심": return 0xFFD32F2F;
            case "주의": return 0xFF4CAF50;
            case "안심": return 0xFF377DFF;
            default: return 0xFFBDBDBD;
        }
    }

    private String getNow(String format) {
        return new SimpleDateFormat(format, Locale.KOREAN).format(new Date());
    }

    private Uri parseUri(String uriStr) {
        try {
            return (uriStr != null) ? Uri.parse(uriStr) : null;
        } catch (Exception e) {
            return null;
        }
    }
}