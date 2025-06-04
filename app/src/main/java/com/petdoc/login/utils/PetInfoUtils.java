package com.petdoc.login.utils;

import android.os.Bundle;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.petdoc.login.CurrentPetManager;

import java.util.HashMap;
import java.util.Map;

public class PetInfoUtils {

    //  나중에 등록하기일 경우 → 기본 구조 전체 세팅
    public static void savePartialDogInfo(Bundle extras, String petKey) {
        if (extras == null || petKey == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference basicInfoRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child(uid)
                .child(petKey)
                .child("basicInfo");

        Map<String, Object> fullStructure = new HashMap<>();
        fullStructure.put("name", extras.getString("petName", null));
        fullStructure.put("gender", extras.getString("petGender", null));
        fullStructure.put("neutered", extras.containsKey("petNeutered") ? extras.getBoolean("petNeutered") : null);
        fullStructure.put("weight", extras.containsKey("petWeight") ? extras.get("petWeight") : null);
        fullStructure.put("imagePath", extras.getString("petImagePath", null));

        basicInfoRef.setValue(fullStructure); // 전체 구조를 한번에 저장
        CurrentPetManager.getInstance().setCurrentPetId(petKey);
    }

    //  모든 값이 다 입력되었을 경우 (사진까지 완료) → 필요한 항목만 업데이트
    public static void updateCompletedDogInfo(Bundle extras, String petKey) {
        if (extras == null || petKey == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference basicInfoRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("Users")
                .child(uid)
                .child(petKey)
                .child("basicInfo");

        Map<String, Object> updateData = new HashMap<>();
        if (extras.containsKey("petName")) updateData.put("name", extras.getString("petName"));
        if (extras.containsKey("petGender")) updateData.put("gender", extras.getString("petGender"));
        if (extras.containsKey("petNeutered")) updateData.put("neutered", extras.getBoolean("petNeutered"));
        if (extras.containsKey("petWeight")) updateData.put("weight", extras.get("petWeight"));
        if (extras.containsKey("petImagePath")) updateData.put("imagePath", extras.getString("petImagePath"));

        basicInfoRef.updateChildren(updateData); // 필요한 데이터만 덮어씀
        CurrentPetManager.getInstance().setCurrentPetId(petKey);
    }
}
