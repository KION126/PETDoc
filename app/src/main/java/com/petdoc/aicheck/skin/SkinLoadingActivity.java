package com.petdoc.aicheck.skin;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;

/**
 * 피부 검진 로딩 페이지
 * 해당 페이지에서 EyeDisesePredictor을 통해 출력 된 검진 결과를 FB에 저장
 */
public class SkinLoadingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        //setContentView(R.layout.);
    }

}
