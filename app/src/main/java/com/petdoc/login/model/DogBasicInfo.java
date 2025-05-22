package com.petdoc.login.model;

public class DogBasicInfo {
    public String 이름;
    public String 성별;
    public int 나이;
    public double 체중;
    public String 이미지파일경로;

    public DogBasicInfo() {}

    public DogBasicInfo(String 이름) {
        this.이름 = 이름;
    }
}

