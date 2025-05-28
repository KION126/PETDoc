package com.petdoc.walklog;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class WalkLogRepository {

    private final DatabaseReference userRef;
    private final Context context;

    public WalkLogRepository(@NonNull Context context) {
        this.context = context;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child("WalkLog");
    }

    // 콜백 방식으로 외부 처리
    public void saveWalkLog(String walkTime, int steps, WalkLogCallback callback) {
        String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        HashMap<String, Object> walkData = new HashMap<>();
        walkData.put("walkTime", walkTime);
        walkData.put("steps", steps);

        userRef.child(dateKey).setValue(walkData)
                .addOnSuccessListener(aVoid -> {
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    callback.onFailure(e);
                });
    }

    // 콜백 인터페이스 정의 (Activity에서 구현할 수 있음)
    public interface WalkLogCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}