package com.petdoc.genetic;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import com.petdoc.R;
import com.petdoc.login.CurrentPetManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class GeneticInfoActivity extends AppCompatActivity {

    private Button btnNext;
    private ImageButton btnBack;
    private TextView btnAllInfo;
    private LinearLayout linearLayout;
    private TextView tvDate;

    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_genetic);

        // 뷰 연결
        linearLayout = findViewById(R.id.layout_container);
        btnNext = findViewById(R.id.btn_next);
        btnBack = findViewById(R.id.btn_back);
        btnAllInfo = findViewById(R.id.tv_all_info);
        tvDate = findViewById(R.id.tv_date);

        // 현재 유저 확인
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e("GeneticInfoActivity", "로그인된 사용자 없음");
            return;
        }

        // 현재 반려견 확인
        String uid = currentUser.getUid();
        String currentPetId = CurrentPetManager.getInstance().getCurrentPetId();
        if (currentPetId == null) {
            Log.e("GeneticInfoActivity", "현재 반려견 ID 없음");
            return;
        }

        // 이전 유전병 예측 데이터 확인
        DatabaseReference geneticRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child(currentPetId)
                .child("Genetic");

        geneticRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e("GeneticInfoActivity", "Genetic 데이터 없음");
                    return;
                }

                DataSnapshot predictionSnapshot = null;
                for (DataSnapshot child : snapshot.getChildren()) {
                    predictionSnapshot = child;
                    break;
                }

                if (predictionSnapshot == null || !predictionSnapshot.exists()) {
                    Log.e("GeneticInfoActivity", "예측 데이터 없음");
                    return;
                }

                String timestamp = predictionSnapshot.getKey();
                Map<String, Object> data = (Map<String, Object>) predictionSnapshot.getValue();

                tvDate.setText(formatDate(timestamp));

                List<BreedScore> breedScoreList = extractBreedScores(data);
                showBreedSummary(breedScoreList);
                showBreedDetail(breedScoreList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("GeneticInfoActivity", "데이터 로딩 실패: " + error.getMessage());
            }
        });

        // 다음 페이지 (유전병 이미지 삽입 페이지)
        btnNext.setOnClickListener(v -> {
            Intent intent = new Intent(GeneticInfoActivity.this, GeneticNoteActivity.class);
            startActivity(intent);
        });

        // 이전 페이지
        btnBack.setOnClickListener(v -> finish());

        // 모든 유전병 정보 페이지
        btnAllInfo.setOnClickListener(v -> {
            Intent intent = new Intent(GeneticInfoActivity.this, GeneticAllInfoActivity.class);
            startActivity(intent);
        });
    }

    // 예측 결과 데이터 KEY (년월일시간분초) TextView에 넣기 위한 형변환 메서드
    private String formatDate(String timestamp) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
            Date date = inputFormat.parse(timestamp);
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault());
            return outputFormat.format(date);
        } catch (Exception e) {
            return timestamp;
        }
    }

    // 예측 결과 데이터 견종:유사도(Score) 매핑 메서드
    private List<BreedScore> extractBreedScores(Map<String, Object> data) {
        List<BreedScore> result = new ArrayList<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (key.equals("imageUrl")) continue;

            try {
                int score = Integer.parseInt(entry.getValue().toString());
                result.add(new BreedScore(key, score));
            } catch (NumberFormatException ignored) {}
        }
        // 점수 내림차순 정렬
        Collections.sort(result, (a, b) -> b.getScore() - a.getScore());
        return result;
    }

    // 예측 결과에 따른 유사도 상위 3견종 JSON파일 매핑 메서드
    private void showBreedSummary(List<BreedScore> breedScoreList) {
        int[] nameIds = {R.id.tv_breed_1, R.id.tv_breed_2, R.id.tv_breed_3};
        int[] progressBarIds = {R.id.pb_breed_1, R.id.pb_breed_2, R.id.pb_breed_3};
        int[] scoreTextIds = {R.id.tv_breed_score_1, R.id.tv_breed_score_2, R.id.tv_breed_score_3};

        for (int i = 0; i < Math.min(3, breedScoreList.size()); i++) {
            BreedScore item = breedScoreList.get(i);
            TextView nameView = findViewById(nameIds[i]);
            ProgressBar progressBar = findViewById(progressBarIds[i]);
            TextView scoreText = findViewById(scoreTextIds[i]);

            try {
                JSONObject jsonObject = new JSONObject(loadJSONFromAsset("genetic_data.json"));
                JSONObject breedInfo = jsonObject.getJSONObject(item.getBreed());
                nameView.setText(breedInfo.getString("title"));
            } catch (JSONException e) {
                nameView.setText(item.getBreed());
            }

            progressBar.setProgress(item.getScore());
            scoreText.setText(String.valueOf(item.getScore()));
        }
    }

    // 예측 결과에 따른 유사도 상위 3견종에 대한 유전병 설명 JSON파일 매핑 메서드
    private void showBreedDetail(List<BreedScore> breedScoreList) {
        String jsonString = loadJSONFromAsset("genetic_data.json");
        if (jsonString == null) return;

        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            for (BreedScore breedScore : breedScoreList) {
                if (jsonObject.has(breedScore.getBreed())) {
                    JSONObject breedInfo = jsonObject.getJSONObject(breedScore.getBreed());
                    String title = breedInfo.getString("title");
                    String desc = breedInfo.getString("desc");

                    View itemView = getLayoutInflater().inflate(R.layout.item_info_genetic, linearLayout, false);
                    TextView titleText = itemView.findViewById(R.id.tv_title);
                    TextView descText = itemView.findViewById(R.id.tv_des);
                    titleText.setText(title);
                    descText.setText(desc);
                    linearLayout.addView(itemView);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // 유전병 정보 JSON파일 load 메서드
    private String loadJSONFromAsset(String filename) {
        String json;
        try (InputStream is = getAssets().open(filename)) {
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}
