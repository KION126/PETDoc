package com.petdoc.walklog;

public class CalendarDayData {
    public int day;
    public String walkTime;
    public int steps;

    public CalendarDayData(int day, String walkTime) {
        this.day = day;
        this.walkTime = walkTime;
        this.steps = 0;
    }

    public CalendarDayData(int day, String walkTime, int steps) {
        this.day = day;
        this.walkTime = walkTime;
        this.steps = steps;
    }
}