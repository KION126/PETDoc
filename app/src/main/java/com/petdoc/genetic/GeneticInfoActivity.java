package com.petdoc.genetic;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GeneticInfoActivity extends AppCompatActivity {

    private Button btnNext;
    private ImageButton btnBack;
    private TextView btnAllInfo;
    private LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_genetic);

        // 뷰 연결
        linearLayout = findViewById(R.id.layout_container);
        btnNext = findViewById(R.id.btn_next);
        btnBack = findViewById(R.id.btn_back);
        btnAllInfo = findViewById(R.id.tv_all_info);

        // Intent로부터 전달받은 견종 예측 정보
        ArrayList<BreedScore> breedScoreList =
                (ArrayList<BreedScore>) getIntent().getSerializableExtra("breedScoreList");

        // 그래프 정의
        if (breedScoreList != null && !breedScoreList.isEmpty()) {
            // 유사도 기준 내림차순 정렬
            Collections.sort(breedScoreList, (a, b) -> b.getScore() - a.getScore());

            // 유사도 상위 3가지 출력
            int[] nameIds = {R.id.tv_breed_1, R.id.tv_breed_2, R.id.tv_breed_3};
            int[] progressBarIds = {R.id.pb_breed_1, R.id.pb_breed_2, R.id.pb_breed_3};
            int[] scoreTextIds = {R.id.tv_breed_score_1, R.id.tv_breed_score_2, R.id.tv_breed_score_3};

            for (int i = 0; i < Math.min(3, breedScoreList.size()); i++) {
                BreedScore item = breedScoreList.get(i);

                TextView nameView = findViewById(nameIds[i]);
                ProgressBar progressBar = findViewById(progressBarIds[i]);
                TextView scoreText = findViewById(scoreTextIds[i]);

                // 견종 이름 매핑
                try {
                    JSONObject jsonObject = new JSONObject(loadJSONFromAsset("genetic_data.json"));
                    JSONObject breedInfo = jsonObject.getJSONObject(item.getBreed());
                    String title = breedInfo.getString("title");
                    nameView.setText(title);
                } catch (JSONException e) {
                    nameView.setText(item.getBreed());
                }

                progressBar.setProgress(item.getScore());
                scoreText.setText(String.valueOf(item.getScore()));
            }
        }

        // JSON 파일 읽고 견종 객체 추가
        String jsonString = loadJSONFromAsset("genetic_data.json");
        if (jsonString != null && breedScoreList != null) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keys = jsonObject.keys();

                while (keys.hasNext()) {
                    String breedKey = keys.next();

                    for (BreedScore breedScore : breedScoreList) {
                        if (breedKey.equalsIgnoreCase(breedScore.getBreed())) {
                            JSONObject breedInfo = jsonObject.getJSONObject(breedKey);
                            String title = breedInfo.getString("title");
                            String desc = breedInfo.getString("desc");

                            View itemView = getLayoutInflater().inflate(R.layout.item_info_genetic, linearLayout, false);
                            TextView titleText = itemView.findViewById(R.id.tv_title);
                            TextView descText = itemView.findViewById(R.id.tv_des);

                            titleText.setText(title);
                            descText.setText(desc);

                            linearLayout.addView(itemView);
                            break;
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // 다음 페이지 (유전병 진단 메인 페이지)
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GeneticInfoActivity.this, GeneticNoteActivity.class);
                startActivity(intent);
            }
        });

        // 이전 페이지
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                finish();
            }
        });

        // 모든 유전병 정보 확인
        btnAllInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                Intent intent = new Intent(GeneticInfoActivity.this, GeneticAllInfoActivity.class);
                startActivity(intent);
            }
        });
    }

    // assets에서 JSON 읽기
    private String loadJSONFromAsset(String filename) {
        String json = null;
        try {
            InputStream is = getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }
}
