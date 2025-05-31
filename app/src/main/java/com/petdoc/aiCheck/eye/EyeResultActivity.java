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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * EyeResultActivity
 * - 안구 진단 결과를 시각적으로 보여주는 화면
 * - 전체 건강도 요약, 질병별 확률, 이미지 미리보기 등을 표시
 */
public class EyeResultActivity extends AppCompatActivity {

    // 영문 질병 라벨 (모델 키)
    private static final String[] LABELS = {
            "blepharitis", "eyelid_tumor", "entropion", "epiphora",
            "pigmentary_keratitis", "corneal_disease", "nuclear_sclerosis",
            "conjunctivitis", "nonulcerative_keratitis", "other"
    };

    // 한글 라벨 (UI 표시용)
    private static final String[] LABELS_KO = {
            "안검염", "안검종양", "안검내반증", "유루증", "색소침착성각막염",
            "각막질환", "핵경화", "결막염", "비궤양성각막질환", "기타"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_eye_result);

        // 뒤로 가기 → AI 검사 메인 화면으로 이동
        findViewById(R.id.back_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, com.petdoc.aiCheck.AICheckActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });

        // 현재 날짜 표시
        TextView dateView = findViewById(R.id.result_date);
        dateView.setText(getNow("yyyy.MM.dd(E) HH:mm"));

        // 예측 결과 및 요약 정보 수신
        float[] leftResult = (float[]) getIntent().getSerializableExtra("left_result");
        float[] rightResult = (float[]) getIntent().getSerializableExtra("right_result");
        EyeHistoryItem summary = (EyeHistoryItem) getIntent().getSerializableExtra("summary_item");

        // 이미지 URI 파싱
        Uri leftEyeUri = null;
        Uri rightEyeUri = null;
        try {
            String leftStr = getIntent().getStringExtra("left_image_uri");
            String rightStr = getIntent().getStringExtra("right_image_uri");
            if (leftStr != null) leftEyeUri = Uri.parse(leftStr);
            if (rightStr != null) rightEyeUri = Uri.parse(rightStr);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 종합 진단 요약 블록 처리
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

            // 왼쪽 눈 결과 표시
            if ("left".equals(summary.side) || "both".equals(summary.side)) {
                leftScore.setText(avgPercent + "%");
                leftScore.setTextColor(color);
            } else {
                leftScore.setText("선택안함");
                leftScore.setTextColor(getStatusColor("선택안함"));
            }

            // 오른쪽 눈 결과 표시
            if ("right".equals(summary.side) || "both".equals(summary.side)) {
                rightScore.setText(avgPercent + "%");
                rightScore.setTextColor(color);
            } else {
                rightScore.setText("선택안함");
                rightScore.setTextColor(getStatusColor("선택안함"));
            }

            // 이미지 미리보기 로드
            if (leftEyeUri != null) Glide.with(this).load(leftEyeUri).into(leftImg);
            if (rightEyeUri != null) Glide.with(this).load(rightEyeUri).into(rightImg);
        }

        // 질병별 결과 카드 생성
        LinearLayout cardContainer = findViewById(R.id.result_card_container);

        for (int i = 0; i < LABELS.length; i++) {
            // 카드 레이아웃 inflate
            View card = getLayoutInflater().inflate(R.layout.item_eye_result_card, cardContainer, false);

            TextView label = card.findViewById(R.id.eye_part_label);
            TextView leftScore = card.findViewById(R.id.left_eye_value);
            TextView rightScore = card.findViewById(R.id.right_eye_value);
            ImageView leftImg = card.findViewById(R.id.left_eye_image);
            ImageView rightImg = card.findViewById(R.id.right_eye_image);

            label.setText(LABELS_KO[i]);

            // 왼쪽 눈 질병 확률
            if (leftResult != null && i < leftResult.length) {
                int percent = Math.round(leftResult[i] * 100);
                leftScore.setText(percent + "%");
                leftScore.setTextColor(getStatusColor(getStatus(leftResult[i])));
            } else {
                leftScore.setText("선택안함");
                leftScore.setTextColor(getStatusColor("선택안함"));
            }

            // 오른쪽 눈 질병 확률
            if (rightResult != null && i < rightResult.length) {
                int percent = Math.round(rightResult[i] * 100);
                rightScore.setText(percent + "%");
                rightScore.setTextColor(getStatusColor(getStatus(rightResult[i])));
            } else {
                rightScore.setText("선택안함");
                rightScore.setTextColor(getStatusColor("선택안함"));
            }

            // 이미지 표시 (같은 이미지 반복 표시)
            if (leftEyeUri != null) Glide.with(this).load(leftEyeUri).into(leftImg);
            if (rightEyeUri != null) Glide.with(this).load(rightEyeUri).into(rightImg);

            cardContainer.addView(card);
        }
    }

    /**
     * 점수값에 따른 상태 분류
     */
    private String getStatus(float v) {
        if (v >= 0.6f) return "의심";     // 위험
        else if (v >= 0.3f) return "주의"; // 경고
        else return "안심";              // 정상
    }

    /**
     * 상태에 따른 텍스트 색상 반환
     */
    private int getStatusColor(String status) {
        switch (status) {
            case "의심": return 0xFFD32F2F; // 빨강
            case "주의": return 0xFF4CAF50; // 초록
            case "안심": return 0xFF377DFF; // 파랑
            default: return 0xFFBDBDBD;     // 회색 (선택안함 등)
        }
    }

    /**
     * 현재 시간 문자열 반환
     */
    private String getNow(String format) {
        return new SimpleDateFormat(format, Locale.KOREAN).format(new Date());
    }
}
