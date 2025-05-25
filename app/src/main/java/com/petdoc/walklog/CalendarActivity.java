package com.petdoc.walklog;

import android.os.Bundle;
import android.widget.GridView;

import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;

import java.util.ArrayList;
import java.util.List;

public class CalendarActivity extends AppCompatActivity {

    private GridView calendarGridView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        calendarGridView = findViewById(R.id.calendarGridView);

        // 예시 데이터: 4월 (30일)
        List<CalendarDayData> days = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            days.add(new CalendarDayData(i, "00:21"));
        }

        CalendarAdapter adapter = new CalendarAdapter(this, days);
        calendarGridView.setAdapter(adapter);
    }
}