package com.petdoc.aiCheck.eye;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log; // Log import 추가
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
// import androidx.appcompat.app.AppCompatActivity; // BaseActivity 사용 중이므로 이 import는 필요 없을 수 있습니다.

import com.bumptech.glide.Glide;
import com.petdoc.R;
import com.petdoc.main.BaseActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * EyeResultActivity
 * - 안구 진단 결과를 시각적으로 보여주는 화면
 * - 전체 건강도 요약, 질병별 확률, 이미지 미리보기 등을 표시
 */
public class EyeResultActivity extends BaseActivity {

    // 질병 라벨 (영어)
    private static final String[] LABELS = {
            "blepharitis", "eyelid_tumor", "entropion", "epiphora",
            "pigmentary_keratitis", "corneal_disease", "nuclear_sclerosis",
            "conjunctivitis", "nonulcerative_keratitis", "other"
    };

    // 질병 라벨 (한글)
    private static final String[] LABELS_KO = {
            "안검염", "안검종양", "안검내반증", "유루증", "색소침착성각막염",
            "각막질환", "핵경화", "결막염", "비궤양성각막질환", "기타"
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

        float[] leftResult = getIntent().getFloatArrayExtra("left_result");
        float[] rightResult = getIntent().getFloatArrayExtra("right_result");
        // summary_item은 이제 이 액티비티에서는 직접 사용하지 않을 수도 있지만,
        // AICheckActivity에서 전달하므로, 일단 받아는 두겠습니다.
        EyeHistoryItem summary = (EyeHistoryItem) getIntent().getSerializableExtra("summary_item");

        Uri leftEyeUri = parseUri(getIntent().getStringExtra("left_image_uri"));
        Uri rightEyeUri = parseUri(getIntent().getStringExtra("right_image_uri"));

        // 레이아웃 참조
        View highestDiseaseCard = findViewById(R.id.highest_disease_card);
        LinearLayout cardContainer = findViewById(R.id.result_card_container);

        // 결과가 모두 없으면 처리 (선택안함 상태)
        if (leftResult == null && rightResult == null) {
            // highestDiseaseCard를 숨기거나, "결과 없음" 메시지를 표시
            if (highestDiseaseCard != null) {
                highestDiseaseCard.setVisibility(View.GONE);
            }
            if (cardContainer != null) {
                cardContainer.setVisibility(View.GONE);
            }
            // 필요한 경우 사용자에게 보여줄 메시지 TextView 등을 추가할 수 있습니다.
            Log.w("EyeResultActivity", "No eye diagnosis results available.");
            return; // 이후 로직 실행 중단
        }

        // 왼쪽/오른쪽 결과를 합산하여 가장 높은 확률의 질병 찾기
        // combinedResult를 먼저 계산하여, 가장 높은 확률의 질병을 찾는데 사용합니다.
        float[] combinedResult = combineResults(leftResult, rightResult);
        int highestIndex = -1;
        float highestProbability = -1.0f;

        if (combinedResult != null) {
            for (int i = 0; i < combinedResult.length; i++) {
                if (combinedResult[i] > highestProbability) {
                    highestProbability = combinedResult[i];
                    highestIndex = i;
                }
            }
        }

        // 가장 높은 확률의 질병 정보를 highestDiseaseCard에 표시
        if (highestIndex != -1) {
            if (highestDiseaseCard != null) { // highestDiseaseCard가 null이 아닌지 확인
                TextView labelTextView = highestDiseaseCard.findViewById(R.id.eye_part_label);
                TextView leftScoreTextView = highestDiseaseCard.findViewById(R.id.left_eye_value);
                TextView rightScoreTextView = highestDiseaseCard.findViewById(R.id.right_eye_value);
                ImageView leftImg = highestDiseaseCard.findViewById(R.id.left_eye_image);
                ImageView rightImg = highestDiseaseCard.findViewById(R.id.right_eye_image);

                if (labelTextView != null) labelTextView.setText(LABELS_KO[highestIndex]); // 가장 높은 확률의 질병 이름 설정

                // 왼쪽 눈 점수 설정
                if (leftScoreTextView != null) {
                    if (leftResult != null && highestIndex < leftResult.length) {
                        int percent = Math.round(leftResult[highestIndex] * 100);
                        leftScoreTextView.setText(percent + "%");
                        leftScoreTextView.setTextColor(getStatusColor(getStatus(leftResult[highestIndex])));
                    } else {
                        leftScoreTextView.setText("선택안함");
                        leftScoreTextView.setTextColor(getStatusColor("선택안함"));
                    }
                }

                // 오른쪽 눈 점수 설정
                if (rightScoreTextView != null) {
                    if (rightResult != null && highestIndex < rightResult.length) {
                        int percent = Math.round(rightResult[highestIndex] * 100);
                        rightScoreTextView.setText(percent + "%");
                        rightScoreTextView.setTextColor(getStatusColor(getStatus(rightResult[highestIndex])));
                    } else {
                        rightScoreTextView.setText("선택안함");
                        rightScoreTextView.setTextColor(getStatusColor("선택안함"));
                    }
                }

                // 이미지 로드
                if (leftImg != null) {
                    if (leftEyeUri != null) {
                        Glide.with(this).load(leftEyeUri).into(leftImg);
                    } else {
                        leftImg.setImageResource(R.drawable.ic_eye_gray);
                    }
                }
                if (rightImg != null) {
                    if (rightEyeUri != null) {
                        Glide.with(this).load(rightEyeUri).into(rightImg);
                    } else {
                        rightImg.setImageResource(R.drawable.ic_eye_gray);
                    }
                }
            }
        } else {
            // 최고 확률 질병을 찾지 못했을 경우 (예: combinedResult가 null이거나 비어있을 경우), 이 카드 숨기기
            if (highestDiseaseCard != null) {
                highestDiseaseCard.setVisibility(View.GONE);
            }
        }


        // --- 2. 각 질병별 상세 결과 카드 동적 생성 및 표시 (가장 높은 확률 질병은 제외) ---
        // cardContainer가 null이 아닌지 확인
        if (cardContainer != null) {
            for (int i = 0; i < LABELS.length; i++) {
                // 가장 높은 확률의 질병은 이미 위에서 표시했으므로, 여기서는 건너뜁니다.
                if (i == highestIndex) {
                    continue;
                }

                View card = getLayoutInflater().inflate(R.layout.item_eye_result_card, cardContainer, false);

                TextView labelTextView = card.findViewById(R.id.eye_part_label);
                TextView leftScoreTextView = card.findViewById(R.id.left_eye_value);
                TextView rightScoreTextView = card.findViewById(R.id.right_eye_value);
                ImageView leftImg = card.findViewById(R.id.left_eye_image);
                ImageView rightImg = card.findViewById(R.id.right_eye_image);

                if (labelTextView != null) labelTextView.setText(LABELS_KO[i]); // 한글 질병 라벨 설정

                // 왼쪽 눈 개별 질병 확률 표시
                if (leftScoreTextView != null) {
                    if (leftResult != null && i < leftResult.length) {
                        int percent = Math.round(leftResult[i] * 100);
                        leftScoreTextView.setText(percent + "%");
                        leftScoreTextView.setTextColor(getStatusColor(getStatus(leftResult[i])));
                    } else {
                        leftScoreTextView.setText("선택안함");
                        leftScoreTextView.setTextColor(getStatusColor("선택안함"));
                    }
                }

                // 오른쪽 눈 개별 질병 확률 표시
                if (rightScoreTextView != null) {
                    if (rightResult != null && i < rightResult.length) {
                        int percent = Math.round(rightResult[i] * 100);
                        rightScoreTextView.setText(percent + "%");
                        rightScoreTextView.setTextColor(getStatusColor(getStatus(rightResult[i])));
                    } else {
                        rightScoreTextView.setText("선택안함");
                        rightScoreTextView.setTextColor(getStatusColor("선택안함"));
                    }
                }

                // 이미지 로드
                if (leftImg != null) {
                    if (leftEyeUri != null) {
                        Glide.with(this).load(leftEyeUri).into(leftImg);
                    } else {
                        leftImg.setImageResource(R.drawable.ic_eye_gray);
                    }
                }
                if (rightImg != null) {
                    if (rightEyeUri != null) {
                        Glide.with(this).load(rightEyeUri).into(rightImg);
                    } else {
                        rightImg.setImageResource(R.drawable.ic_eye_gray);
                    }
                }

                cardContainer.addView(card);
            }
        }
    }

    /**
     * 예측 확률에 따른 상태 문자열을 반환합니다.
     * @param value 예측 확률 (0.0f ~ 1.0f)
     * @return "의심", "주의", "안심" 중 하나
     */
    private String getStatus(float value) {
        if (value >= 0.6f) return "의심"; // 60% 이상이면 "의심"
        else if (value >= 0.3f) return "주의"; // 30% 이상이면 "주의"
        else return "안심"; // 30% 미만이면 "안심"
    }

    /**
     * 상태 문자열에 따른 색상 값을 반환합니다.
     * @param status 상태 문자열 ("의심", "주의", "안심", "선택안함" 등)
     * @return 해당 상태에 맞는 ARGB 색상 값
     */
    private int getStatusColor(String status) {
        switch (status) {
            case "의심": return 0xFFD32F2F; // 빨간색
            case "주의": return 0xFF4CAF50; // 녹색 (안심과 동일하게 표시)
            case "안심": return 0xFF377DFF; // 파란색
            case "선택안함": return 0xFFBDBDBD; // 회색
            default: return 0xFFBDBDBD; // 기본값 회색
        }
    }

    /**
     * 현재 날짜와 시간을 지정된 형식으로 반환합니다.
     * @param format 날짜 형식 문자열 (예: "yyyy.MM.dd(E) HH:mm")
     * @return 형식화된 날짜/시간 문자열
     */
    private String getNow(String format) {
        return new SimpleDateFormat(format, Locale.KOREAN).format(new Date());
    }

    /**
     * URI 문자열을 Uri 객체로 파싱합니다.
     * @param uriStr URI 문자열
     * @return 파싱된 Uri 객체, 파싱 실패 또는 null인 경우 null
     */
    private Uri parseUri(String uriStr) {
        try {
            return (uriStr != null) ? Uri.parse(uriStr) : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 예측 점수 배열의 평균을 계산합니다.
     * 이 메서드는 EyeLoadingActivity에서 이미 구현되었을 수 있으나,
     * EyeResultActivity에서 개별 눈의 종합 건강도를 계산하기 위해 다시 추가했습니다.
     * @param scores 예측 점수 배열
     * @return 평균 점수
     */
    private float calculateAverageScore(float[] scores) {
        if (scores == null || scores.length == 0) return 0.0f;
        float sum = 0;
        for (float v : scores) sum += v;
        return sum / scores.length;
    }

    /**
     * 왼쪽 및 오른쪽 눈의 예측 결과를 합산하여 각 질병에 대한 종합 확률을 계산합니다.
     * 한쪽 눈만 데이터가 있다면 그 눈의 확률을 사용하고, 양쪽 다 있다면 평균을 냅니다.
     * @param leftResult 왼쪽 눈 예측 결과 배열
     * @param rightResult 오른쪽 눈 예측 결과 배열
     * @return 합산된 예측 결과 배열
     */
    private float[] combineResults(float[] leftResult, float[] rightResult) {
        int numLabels = LABELS.length;
        float[] combined = new float[numLabels];

        for (int i = 0; i < numLabels; i++) {
            float leftProb = (leftResult != null && i < leftResult.length) ? leftResult[i] : -1.0f;
            float rightProb = (rightResult != null && i < rightResult.length) ? rightResult[i] : -1.0f;

            if (leftProb != -1.0f && rightProb != -1.0f) {
                // 양쪽 눈 모두 결과가 있다면 평균 사용
                combined[i] = (leftProb + rightProb) / 2.0f;
            } else if (leftProb != -1.0f) {
                // 왼쪽 눈만 결과가 있다면 왼쪽 눈 결과 사용
                combined[i] = leftProb;
            } else if (rightProb != -1.0f) {
                // 오른쪽 눈만 결과가 있다면 오른쪽 눈 결과 사용
                combined[i] = rightProb;
            } else {
                // 양쪽 눈 모두 결과가 없다면 0 또는 낮은 값 설정
                combined[i] = 0.0f;
            }
        }
        return combined;
    }
}