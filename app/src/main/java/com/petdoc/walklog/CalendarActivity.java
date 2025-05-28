package com.petdoc.walklog;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class CalendarActivity extends AppCompatActivity {

    private GridView calendarGridView;
    private TextView monthText;
    private ImageView prevMonth, nextMonth;
    private YearMonth currentMonth;
    private Button startWalkingBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            Intent intent = new Intent(CalendarActivity.this, WalkRecordActivity.class);
            startActivity(intent);
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

        // 실제 날짜 추가 (예: 4월은 30일)
        int lastDay = firstDay.lengthOfMonth();
        for (int i = 1; i <= lastDay; i++) {
            days.add(new CalendarDayData(i, "00:21")); // 임의 시간 데이터
        }

        return days;
    }

    private void updateCalendar() {
        // 월 텍스트 표시 (예: "04월")
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM월");
        monthText.setText(currentMonth.format(formatter));

        // 해당 월의 날짜 리스트 생성
        List<CalendarDayData> days = generateCalendarDays(currentMonth.getYear(), currentMonth.getMonthValue());

        CalendarAdapter adapter = new CalendarAdapter(this, days);
        calendarGridView.setAdapter(adapter);
    }
}