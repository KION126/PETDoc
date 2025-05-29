package com.petdoc.aiCheck.eye;

import java.io.Serializable;

public class EyeHistoryItem implements Serializable {
    public String dateTime;
    public float score;
    public String side;
    public String labelKo;

    public EyeHistoryItem(String dateTime, float score, String side, String labelKo) {
        this.dateTime = dateTime;
        this.score = score;
        this.side = side;
        this.labelKo = labelKo;
    }
}
