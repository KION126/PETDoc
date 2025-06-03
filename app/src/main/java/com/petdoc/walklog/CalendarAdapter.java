package com.petdoc.walklog;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.petdoc.R;

import java.util.List;

public class CalendarAdapter extends BaseAdapter {

    private final Context context;
    private final List<CalendarDayData> dayList;

    public CalendarAdapter(Context context, List<CalendarDayData> dayList) {
        this.context = context;
        this.dayList = dayList;
    }

    @Override
    public int getCount() {
        return dayList.size();
    }

    @Override
    public Object getItem(int position) {
        return dayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CalendarDayData day = dayList.get(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_calendar_day, parent, false);

            // ✅ 정사각형 셀 크기 설정
            int cellSize = parent.getWidth() / 7;
            convertView.setLayoutParams(new AbsListView.LayoutParams(cellSize, cellSize));
        }

        TextView dayText = convertView.findViewById(R.id.dayText);
        TextView walkTimeText = convertView.findViewById(R.id.walkTimeText);
        View container = convertView.findViewById(R.id.dayContainer);

        if (day.day == 0) {
            dayText.setText("");
            walkTimeText.setText("");
            container.setBackgroundColor(Color.TRANSPARENT);
        } else {
            dayText.setText(String.valueOf(day.day));

            if (day.walkTime != null && !day.walkTime.isEmpty()) {
                String[] parts = day.walkTime.split(":");
                int hour = Integer.parseInt(parts[0]);
                int min = Integer.parseInt(parts[1]);
                int sec = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
                int totalSeconds = hour * 3600 + min * 60 + sec;

                int bgColor;
                if (totalSeconds <= 60) {
                    bgColor = Color.parseColor("#E0F7FA");
                } else if (totalSeconds <= 1800) {
                    bgColor = Color.parseColor("#80DEEA");
                } else if (totalSeconds <= 3600) {
                    bgColor = Color.parseColor("#26C6DA");
                } else {
                    bgColor = Color.parseColor("#0097A7");
                }
                container.setBackgroundColor(bgColor);

                walkTimeText.setText(day.walkTime.substring(0, 5));
            } else {
                walkTimeText.setText("");
                container.setBackgroundColor(Color.TRANSPARENT);
            }
        }

        return convertView;
    }
}