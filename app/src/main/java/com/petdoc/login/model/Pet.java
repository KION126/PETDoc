package com.petdoc.login.model;

import java.util.Map;

/**
 * Firebase Realtime Database에 저장될 반려견 1마리의 전체 데이터를 나타내는 클래스입니다.
 * 기본 정보와 진단 결과, 유전병 정보, 산책 일지 등을 포함합니다.
 */
public class Pet {

    /** 반려견의 기본 정보 (이름, 나이, 성별 등) */
    public DogBasicInfo basicInfo;

    /**
     * Firebase에서 역직렬화를 위해 반드시 기본 생성자가 필요합니다.
     */
    public Pet() {}
}
