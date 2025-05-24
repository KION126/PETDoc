package com.petdoc.genetic;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;
import com.petdoc.login.LoginActivity;

import java.util.ArrayList;

public class GeneticLoadingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_genetic);

        ArrayList<BreedScore> breedScoreList = new ArrayList<>();
        breedScoreList.add(new BreedScore("pug", 88));
        breedScoreList.add(new BreedScore("siberian_husky", 55));
        breedScoreList.add(new BreedScore("beagle", 22));

        // 3초 후 유전병 예측 결과 페이지로 전환
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(GeneticLoadingActivity.this, GeneticInfoActivity.class);
            intent.putExtra("breedScoreList", breedScoreList);
            startActivity(intent);
            finish();
        }, 3000);
    }
}
