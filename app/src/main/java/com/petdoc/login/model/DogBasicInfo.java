package com.petdoc.login.model;

/**
 * Firebase Realtime Database에서 사용되는 반려견 기본 정보 클래스입니다.
 * 이 클래스는 Firebase의 자동 직렬화/역직렬화 기능과 호환되도록 구성되어 있습니다.
 */
public class DogBasicInfo {

    /** 반려견의 이름 (예: "초코") */
    public String name;

    /** 반려견의 성별 (예: "수컷", "암컷") */
    public String gender;

    /** 반려견의 나이 (단위: 년) */
    public int age;

    /** 반려견의 체중 (단위: kg) */
    public double weight;

    /** 반려견의 이미지 파일 경로 또는 URL (Firebase Storage 연동 가능) */
    public String imagePath;

    /**
     * Firebase에서 데이터를 읽어올 때 반드시 필요한 기본 생성자입니다.
     * 생략 시 Realtime Database에서 자동 매핑되지 않습니다.
     */
    public DogBasicInfo() {}

    /**
     * 이름만 초기화하는 생성자 (테스트 또는 부분 저장용)
     * @param name 반려견 이름
     */
    public DogBasicInfo(String name) {
        this.name = name;
    }
}
