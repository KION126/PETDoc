package com.petdoc.walklog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
        }

        TextView dayText = convertView.findViewById(R.id.dayText);
        TextView walkTimeText = convertView.findViewById(R.id.walkTimeText);

        if (day.day == 0) {
            dayText.setText("");
            walkTimeText.setText("");
        } else {
            dayText.setText(String.valueOf(day.day));
            walkTimeText.setText(day.walkTime);
        }
        return convertView;
    }
}