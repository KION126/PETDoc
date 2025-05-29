package com.petdoc.aiCheck.eye;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.petdoc.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EyeResultActivity extends AppCompatActivity {

    // 검진 결과 라벨 (영문/한글)
    private static final String[] LABELS = {
            "blepharitis",           // 안검염
            "eyelid_tumor",          // 안검종양
            "entropion",             // 안검내반증
            "epiphora",              // 유루증
            "pigmentary_keratitis",  // 색소침착성각막염
            "corneal_disease",       // 각막질환
            "nuclear_sclerosis",     // 핵경화
            "conjunctivitis",        // 결막염
            "nonulcerative_keratitis", // 비궤양성각막질환
            "other"                  // 기타
    };
    private static final String[] LABELS_KO = {
            "안검염", "안검종양", "안검내반증", "유루증", "색소침착성각막염",
            "각막질환", "핵경화", "결막염", "비궤양성각막질환", "기타"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_eye_result);

        // 1. 뒤로가기 버튼
        ImageView backButton = findViewById(R.id.back_button);
        if (backButton != null) backButton.setOnClickListener(v -> finish());

        // 2. 날짜 표시
        TextView dateView = findViewById(R.id.result_date);
        if (dateView != null) {
            dateView.setText(getNow("yyyy.MM.dd(E) HH:mm"));
        }

        // 3. 예측 결과(float[]), 왼/오른 눈 점수 받아오기 (여기선 한쪽 값만 있다고 가정: result[0~9])
        float[] result = (float[]) getIntent().getSerializableExtra("result");
        // 확장: 만약 왼쪽/오른쪽 분리라면 getIntent().getFloatArrayExtra("left_result") ... 등으로 받을 것
        if (result == null) return;

        // 4. 종합 건강도 카드 (item_eye_result_block 안에 왼/오른 눈 View 있는 구조라면)
        FrameLayout summaryBlock = findViewById(R.id.summary_block_wrapper);
        if (summaryBlock != null) {
            View included = summaryBlock.findViewById(R.id.summary_block);
            if (included != null) {
                // 왼쪽/오른쪽 눈의 종합 score와 상태, indicator 색상 바인딩
                TextView leftScore = included.findViewById(R.id.left_eye_score);
                TextView rightScore = included.findViewById(R.id.right_eye_score);
                View leftIndicator = included.findViewById(R.id.left_eye_indicator);
                View rightIndicator = included.findViewById(R.id.right_eye_indicator);

                // 예시: 전체 result에서 가장 높은 확률을 종합 점수로 가정 (왼쪽/오른쪽 분리 시에는 따로 계산)
                int maxIdx = getMaxIndex(result);
                float maxScore = result[maxIdx];
                String status = getStatus(maxScore);

                // 값 바인딩 (한쪽만 있는 경우)
                if (leftScore != null) leftScore.setText(String.format("%.0f%%", maxScore * 100));
                if (rightScore != null) rightScore.setText("-");
                if (leftIndicator != null) leftIndicator.setBackgroundTintList(ColorStateList.valueOf(getStatusColor(status)));
                if (rightIndicator != null) rightIndicator.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gray_300)));

                // 종합 결과 타이틀 변경 (optional)
                TextView title = included.findViewById(R.id.eye_result_title);
                if (title != null) title.setText("종합 안구 건강도\n" + LABELS_KO[maxIdx] + " (" + LABELS[maxIdx] + ")");
            }
        }

        // 5. 검진 항목별 상세 결과 (item_eye_result_block summary_text에 모두 표기)
        FrameLayout resultContainer = findViewById(R.id.result_container);
        if (resultContainer != null) {
            View included = resultContainer.findViewById(R.id.result_item_1);
            if (included != null) {
                TextView detailText = included.findViewById(R.id.summary_text);
                if (detailText != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < result.length; i++) {
                        String status = getStatus(result[i]);
                        sb.append(LABELS_KO[i]).append(" (").append(LABELS[i]).append("): ")
                                .append(String.format("%.1f", result[i] * 100)).append("%  ")
                                .append(status).append("\n");
                    }
                    detailText.setText(sb.toString());
                }
            }
        }
    }

    // 최고 확률 항목 반환
    private int getMaxIndex(float[] arr) {
        int idx = 0;
        for (int i = 1; i < arr.length; i++)
            if (arr[i] > arr[idx]) idx = i;
        return idx;
    }

    // 상태 분류: 안심/주의/의심
    private String getStatus(float v) {
        if (v >= 0.6f) return "의심";
        else if (v >= 0.3f) return "주의";
        else return "안심";
    }

    // 상태별 색상 반환 (indicator에 바인딩)
    private int getStatusColor(String status) {
        switch (status) {
            case "의심":
                return 0xFFD32F2F; // 빨강
            case "주의":
                return 0xFF4CAF50; // 초록
            default:
                return 0xFF377DFF; // 파랑
        }
    }

    // 날짜 포맷 함수
    private String getNow(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.KOREAN);
        return sdf.format(new Date());
    }
}
