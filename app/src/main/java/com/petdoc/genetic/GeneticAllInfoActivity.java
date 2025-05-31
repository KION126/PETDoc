package com.petdoc.genetic;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

public class GeneticAllInfoActivity extends AppCompatActivity {

    private LinearLayout linearLayout;

    private Button btnNext;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_info_genetic);

        // 뷰 연결
        linearLayout = findViewById(R.id.layout_container);
        btnNext = findViewById(R.id.btn_next);
        btnBack = findViewById(R.id.btn_back);

        // JSON 읽기 및 파싱
        String jsonString = loadJSONFromAsset("genetic_data.json");
        if (jsonString != null) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keys = jsonObject.keys();

                while (keys.hasNext()) {
                    String breedKey = keys.next();
                    JSONObject breedInfo = jsonObject.getJSONObject(breedKey);

                    String title = breedInfo.getString("title");
                    String desc = breedInfo.getString("desc");

                    // 카드뷰 레이아웃 inflate
                    View itemView = getLayoutInflater().inflate(R.layout.item_info_genetic, linearLayout, false);

                    TextView titleText = itemView.findViewById(R.id.tv_title);
                    TextView descText = itemView.findViewById(R.id.tv_des);

                    titleText.setText(title);
                    descText.setText(desc);

                    linearLayout.addView(itemView);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // 다음 페이지 (유전병 진단 메인 페이지)
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(GeneticAllInfoActivity.this, GeneticNoteActivity.class);
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
