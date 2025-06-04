package com.petdoc.walklog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.petdoc.R;
import com.petdoc.main.BaseActivity;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends BaseActivity {

    private GridView calendarGridView;
    private TextView monthText;
    private ImageView prevMonth, nextMonth;
    private YearMonth currentMonth;
    private Button startWalkingBtn;

    // 센서 권한 요청 식별자 정의
    private static final int PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_calendar);

        calendarGridView = findViewById(R.id.calendarGridView);
        monthText = findViewById(R.id.monthText);
        prevMonth = findViewById(R.id.prevMonth);
        nextMonth = findViewById(R.id.nextMonth);
        startWalkingBtn = findViewById(R.id.startWalkingBtn);
        ImageButton btnBack = findViewById(R.id.btn_back);

        startWalkingBtn.setBackgroundColor(Color.BLUE); // 얘가 있어야 버튼이 보임 왜 그런지 모르겠음

        // 현재 월 초기화
        currentMonth = YearMonth.now();
        updateCalendar(); // 처음 화면 세팅

        // 이전/다음 버튼 클릭 리스너
        prevMonth.setOnClickListener(v -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendar();
        });

        nextMonth.setOnClickListener(v -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendar();
        });

        List<CalendarDayData> days = generateCalendarDays(2025, 5);

        CalendarAdapter adapter = new CalendarAdapter(this, days);
        calendarGridView.setAdapter(adapter);

        startWalkingBtn.setOnClickListener(v -> {
            // 걸음 센서 권한 요청
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION},
                        PERMISSION_REQUEST_ACTIVITY_RECOGNITION);
            } else {
                String dogId = getIntent().getStringExtra("dogId");
                Intent intent = new Intent(CalendarActivity.this, WalkRecordActivity.class);
                intent.putExtra("dogId", dogId);
                startActivity(intent);
            }
        });

        //뒤로가기
        btnBack.setOnClickListener(v -> finish());
    }

    private List<CalendarDayData> generateCalendarDays(int year, int month) {
        List<CalendarDayData> days = new ArrayList<>();

        // 1일의 요일을 구함
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int dayOfWeek = firstDay.getDayOfWeek().getValue(); // 1=월 ~ 7=일
        int offset = dayOfWeek % 7; // 일요일부터 시작하게 맞춤

        // 앞에 offset만큼 빈 셀 추가
        for (int i = 0; i < offset; i++) {
            days.add(new CalendarDayData(0, "")); // 0은 비어있는 칸
        }

        // 실제 날짜 추가
        int lastDay = firstDay.lengthOfMonth();
        for (int i = 1; i <= lastDay; i++) {
            days.add(new CalendarDayData(i, "00:00"));
        }

        return days;
    }

    private void updateCalendar() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM월");
        monthText.setText(currentMonth.format(formatter));

        generateCalendarDaysWithWalkData(currentMonth.getYear(), currentMonth.getMonthValue(), days -> {
            CalendarAdapter adapter = new CalendarAdapter(this, days);
            calendarGridView.setAdapter(adapter);
            calculateMonthlyStats(days);
        });
    }

    public void generateCalendarDaysWithWalkData(int year, int month, OnCalendarDataLoaded callback) {
        List<CalendarDayData> days = new ArrayList<>();
        YearMonth ym = YearMonth.of(year, month);
        LocalDate firstDay = ym.atDay(1);
        int offset = firstDay.getDayOfWeek().getValue() % 7;

        for (int i = 0; i < offset; i++) {
            days.add(new CalendarDayData(0, "", 0));
        }

        int lastDay = ym.lengthOfMonth();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String dogId = getIntent().getStringExtra("dogId");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child(dogId).child("WalkLog");

        ref.get().addOnSuccessListener(snapshot -> {
            for (int day = 1; day <= lastDay; day++) {
                String dateKey = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day);

                int totalSteps = 0;
                long totalSeconds = 0;
                boolean hasData = false;

                if (snapshot.hasChild(dateKey)) {
                    hasData = true;

                    for (DataSnapshot logSnap : snapshot.child(dateKey).getChildren()) {
                        String time = logSnap.child("walkTime").getValue(String.class);
                        Integer steps = logSnap.child("steps").getValue(Integer.class);

                        if (steps != null) {
                            totalSteps += steps;
                        }

                        if (time != null) {
                            String[] parts = time.split(":");
                            try {
                                int h = Integer.parseInt(parts[0]);
                                int m = Integer.parseInt(parts[1]);
                                int s = parts.length >= 3 ? Integer.parseInt(parts[2]) : 0;
                                totalSeconds += h * 3600L + m * 60L + s;
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }

                if (hasData) {
                    String totalTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d",
                            totalSeconds / 3600, (totalSeconds % 3600) / 60);
                    days.add(new CalendarDayData(day, totalTimeFormatted, totalSteps));
                } else {
                    // 아무 기록이 없는 경우, 시간은 표시하지 않음
                    days.add(new CalendarDayData(day, "", 0));
                }
            }

            callback.onDataReady(days);
        });
    }

    private void calculateMonthlyStats(List<CalendarDayData> dayDataList) {
        int totalSteps = 0;
        int totalSeconds = 0;
        int daysWithData = 0;

        for (CalendarDayData data : dayDataList) {
            if (data.day == 0 || data.walkTime == null || data.walkTime.isEmpty()) continue;

            String[] parts = data.walkTime.split(":");
            if (parts.length >= 2) {
                try {
                    int hour = Integer.parseInt(parts[0]);
                    int minute = Integer.parseInt(parts[1]);
                    totalSeconds += hour * 3600 + minute * 60;
                    daysWithData++;
                } catch (NumberFormatException ignored) {
                }
            }

            if (data.steps > 0) {
                totalSteps += data.steps;
            }
        }

        // 평균 계산
        int avgSteps = daysWithData == 0 ? 0 : totalSteps / daysWithData;
        int avgSeconds = daysWithData == 0 ? 0 : totalSeconds / daysWithData;

        int avgMinutes = (avgSeconds % 3600) / 60;

        TextView totalStepsView = findViewById(R.id.totalSteps);
        TextView avgStepsView = findViewById(R.id.avgSteps);
        TextView totalTimeView = findViewById(R.id.totalTime);
        TextView avgTimeView = findViewById(R.id.avgTime);

        totalStepsView.setText(String.format(Locale.getDefault(), "%,d", totalSteps));
        avgStepsView.setText(String.format(Locale.getDefault(), "%,d", avgSteps));
        totalTimeView.setText(String.valueOf(totalSeconds / 60)); // 시간 단위로 하고 싶으면 / 3600으로
        avgTimeView.setText(String.format(Locale.getDefault(), "%02d:%02d", avgSeconds / 3600, avgMinutes));
    }

    public interface OnCalendarDataLoaded {
        void onDataReady(List<CalendarDayData> data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCalendar();
        String dogId = getIntent().getStringExtra("dogId");
        updateWalkButtonText(dogId);
    }

    // 걸음 센서 권한 요청 받기
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "걸음 수 인식 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(CalendarActivity.this, WalkRecordActivity.class);
                intent.putExtra("dogId", getIntent().getStringExtra("dogId"));
                startActivity(intent);
            } else {
                Toast.makeText(this, "산책 일지 기능을 사용하기 위해서는\n걸음 수 인식 권한이 필요합니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateWalkButtonText(String dogId) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String todayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("Users").child(uid).child(dogId).child("WalkLog").child(todayKey);

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                    startWalkingBtn.setText("한번 더 산책하기!");
                } else {
                    startWalkingBtn.setText("오늘 첫번째 산책 시작!");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
}