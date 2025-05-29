package com.petdoc.walklog;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class WalkLogRepository {

    private final DatabaseReference walkLogRef;
    private final Context context;

    public WalkLogRepository(@NonNull Context context, @NonNull String dogId) {
        this.context = context;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        walkLogRef = FirebaseDatabase.getInstance()
                .getReference("Users")
                .child(uid)
                .child(dogId)
                .child("WalkLog");
    }

    public void saveWalkLog(String walkTimeStr, int steps, WalkLogCallback callback) {
        String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        walkLogRef.child(dateKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int prevSteps = 0;
                long prevTimeSec = 0;

                if (snapshot.exists()) {
                    Long storedSteps = snapshot.child("steps").getValue(Long.class);
                    String storedTime = snapshot.child("walkTime").getValue(String.class);

                    if (storedSteps != null) prevSteps = storedSteps.intValue();
                    if (storedTime != null) {
                        String[] parts = storedTime.split(":");
                        if (parts.length == 3) {
                            int h = Integer.parseInt(parts[0]);
                            int m = Integer.parseInt(parts[1]);
                            int s = Integer.parseInt(parts[2]);
                            prevTimeSec = h * 3600L + m * 60L + s;
                        }
                    }
                }

                // 새 walkTime도 초로 변환
                String[] newParts = walkTimeStr.split(":");
                int h = Integer.parseInt(newParts[0]);
                int m = Integer.parseInt(newParts[1]);
                int s = Integer.parseInt(newParts[2]);
                long newTimeSec = h * 3600L + m * 60L + s;

                // 누적 결과 계산
                int totalSteps = prevSteps + steps;
                long totalSeconds = prevTimeSec + newTimeSec;

                // 다시 HH:mm:ss 포맷으로
                String totalTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d",
                        totalSeconds / 3600,
                        (totalSeconds % 3600) / 60,
                        totalSeconds % 60);

                HashMap<String, Object> walkData = new HashMap<>();
                walkData.put("walkTime", totalTimeFormatted);
                walkData.put("steps", totalSteps);

                walkLogRef.child(dateKey).setValue(walkData)
                        .addOnSuccessListener(aVoid -> callback.onSuccess())
                        .addOnFailureListener(callback::onFailure);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailure(error.toException());
            }
        });
    }

    public interface WalkLogCallback {
        void onSuccess();
        void onFailure(Exception e);
    }
}