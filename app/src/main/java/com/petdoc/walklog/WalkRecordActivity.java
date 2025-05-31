package com.petdoc.walklog;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.petdoc.walklog.WalkLogRepository;
import com.petdoc.R;

public class WalkRecordActivity extends AppCompatActivity {
    private Button endWalkBtn;
    private TextView timeText, stepCountText;
    private long secondsElapsed = 0;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private int totalSteps = 0;
    private boolean isWalking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_walking_record);

        WalkLogRepository walkLogRepo = new WalkLogRepository(this, "Dog1");

        endWalkBtn = findViewById(R.id.endWalkBtn);
        timeText = findViewById(R.id.walkTimeText);
        stepCountText = findViewById(R.id.stepCountText);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        ImageButton btnBack = findViewById(R.id.btn_back);

        endWalkBtn.setBackgroundColor(Color.BLUE);

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                secondsElapsed++;
                int hours = (int) (secondsElapsed / 3600);
                int minutes = (int) ((secondsElapsed % 3600) / 60);
                int seconds = (int) (secondsElapsed % 60);
                timeText.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
                timerHandler.postDelayed(this, 1000); // 1초마다 반복
            }
        };
        timerHandler.post(timerRunnable);

        if (stepSensor != null) {
            isWalking = true;
            sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_UI);
        } else {
            stepCountText.setText("걸음 센서 없음");
        }

        endWalkBtn.setOnClickListener(v -> {
            timerHandler.removeCallbacks(timerRunnable); // 타이머 정지
            if (isWalking) {
                sensorManager.unregisterListener(stepListener);
            }

            // 저장할 산책 시간 (초 -> HH:mm:ss 포맷)
            int hours = (int) (secondsElapsed / 3600);
            int minutes = (int) ((secondsElapsed % 3600) / 60);
            int seconds = (int) (secondsElapsed % 60);
            String formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);

            // Firebase에 저장
            walkLogRepo.saveWalkLog(formattedTime, totalSteps, new WalkLogRepository.WalkLogCallback() {
                @Override
                public void onSuccess() {
                    finish(); // 저장 성공 시 종료
                }

                @Override
                public void onFailure(Exception e) {
                    // 실패 시 처리 (예: 토스트 출력)
                    Toast.makeText(WalkRecordActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        //뒤로가기
        btnBack.setOnClickListener(v -> finish());
    }

    private SensorEventListener stepListener = new SensorEventListener() {
        int initialStep = -1;

        @Override
        public void onSensorChanged(SensorEvent event) {
            int steps = (int) event.values[0];
            if (initialStep == -1) {
                initialStep = steps;
            }
            totalSteps = steps - initialStep;
            stepCountText.setText(String.valueOf(totalSteps));
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) { }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
        if (isWalking) {
            sensorManager.unregisterListener(stepListener);
        }
    }
}