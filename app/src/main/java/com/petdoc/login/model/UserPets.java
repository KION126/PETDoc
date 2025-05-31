package com.petdoc.login.model;

import java.util.Map;

/**
 * Firebase Realtime Database에 저장되는 사용자의 반려견 전체 목록을 담는 클래스입니다.
 * 각 반려견은 고유한 ID(Key)로 구분되며, 해당 키는 Firebase의 하위 노드로 사용됩니다.
 */
public class UserPets {

    /**
     * 사용자의 반려견 목록입니다.
     * Key: 반려견 고유 ID (예: "dog1", "dog_123")
     * Value: Pet 객체 (기본 정보, 진단 결과, 산책일지 등 포함)
     */
    public Map<String, Pet> petList;

    public UserPets() {}
}
