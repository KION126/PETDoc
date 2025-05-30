package com.petdoc.aiCheck.eye;

import java.io.Serializable;

public class EyeHistoryItem implements Serializable {
    public String dateTime, side, labelKo;
    public float score;

    public EyeHistoryItem(String dateTime, float score, String side, String labelKo) {
        this.dateTime = dateTime;
        this.score = score;
        this.side = side;
        this.labelKo = labelKo;
    }
}
