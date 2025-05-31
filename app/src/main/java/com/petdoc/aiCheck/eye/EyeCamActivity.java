package com.petdoc.aiCheck.eye;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.petdoc.R;
import com.petdoc.login.CurrentPetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 안구 이미지 입력 페이지
 * 사용자로부터 왼쪽/오른쪽 눈 이미지를 선택(앨범/카메라) 받고
 * 이후 진단을 시작하는 액티비티
 */
public class EyeCamActivity extends AppCompatActivity {

    // UI 요소
    private ImageView previewImage;
    private ImageView placeholderIcon;
    private LinearLayout albumButton;
    private LinearLayout cameraButton;
    private ImageView leftEyeIcon;
    private ImageView rightEyeIcon;
    private Button btnCheck;

    // 좌우 눈 이미지 정보 (Bitmap 및 Uri)
    private Bitmap leftEyeBitmap;
    private Bitmap rightEyeBitmap;
    private Uri leftEyeUri;
    private Uri rightEyeUri;

    // 현재 선택된 눈 (true: 왼쪽, false: 오른쪽)
    private boolean isLeftSelected = true;

    // 갤러리에서 이미지 선택 후 결과 처리
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        onImageSelected(selectedImageUri);
                    }
                }
            });

    // 카메라로 사진 촬영 후 결과 처리
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    if (photo != null) {
                        onCameraCaptured(photo);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_eye_cam);

        // UI 요소 초기화
        previewImage = findViewById(R.id.previewImage);
        placeholderIcon = findViewById(R.id.placeholderIcon);
        albumButton = findViewById(R.id.albumButton);
        cameraButton = findViewById(R.id.cameraButton);
        leftEyeIcon = findViewById(R.id.leftEyeIcon);
        rightEyeIcon = findViewById(R.id.rightEyeIcon);
        btnCheck = findViewById(R.id.btnCheck);

        // 뒤로가기 버튼
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        // 왼쪽 눈 선택
        leftEyeIcon.setOnClickListener(v -> {
            isLeftSelected = true;
            updateEyeToggle();
            updatePreview();
        });

        // 오른쪽 눈 선택
        rightEyeIcon.setOnClickListener(v -> {
            isLeftSelected = false;
            updateEyeToggle();
            updatePreview();
        });

        // 앨범 버튼 클릭 → 갤러리 실행
        albumButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        // 카메라 버튼 클릭 → 카메라 실행
        cameraButton.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        });

        // 진단 시작 버튼
        btnCheck.setOnClickListener(v -> {
            boolean hasLeft = leftEyeUri != null;
            boolean hasRight = rightEyeUri != null;

            if (!hasLeft && !hasRight) {
                Toast.makeText(this, "왼쪽 또는 오른쪽 이미지를 선택해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 진단 로딩 액티비티로 이동
            Intent intent = new Intent(this, EyeLoadingActivity.class);
            if (hasLeft) {
                intent.putExtra("left_image_uri", leftEyeUri.toString());
            }
            if (hasRight) {
                intent.putExtra("right_image_uri", rightEyeUri.toString());
            }

            // 현재 선택된 반려동물 ID 전달
            String petId = CurrentPetManager.getInstance().getCurrentPetId();
            if (petId != null) {
                intent.putExtra("pet_id", petId);
            }

            startActivity(intent);
        });

        // 버튼 초기 상태 업데이트
        updateCheckButtonState();
    }

    /**
     * 갤러리에서 이미지 선택 처리
     */
    private void onImageSelected(Uri uri) {
        if (isLeftSelected) {
            leftEyeUri = uri;
            leftEyeBitmap = null;
        } else {
            rightEyeUri = uri;
            rightEyeBitmap = null;
        }
        updatePreview();
        updateCheckButtonState();
    }

    /**
     * 카메라 촬영 이미지 처리 및 저장
     */
    private void onCameraCaptured(Bitmap bitmap) {
        if (isLeftSelected) {
            leftEyeBitmap = bitmap;
            leftEyeUri = saveBitmapAndGetUri(bitmap);
        } else {
            rightEyeBitmap = bitmap;
            rightEyeUri = saveBitmapAndGetUri(bitmap);
        }
        updatePreview();
        updateCheckButtonState();
    }

    /**
     * 현재 선택된 눈의 이미지 미리보기 표시
     */
    private void updatePreview() {
        Bitmap currentBitmap = isLeftSelected ? leftEyeBitmap : rightEyeBitmap;
        Uri currentUri = isLeftSelected ? leftEyeUri : rightEyeUri;

        if (currentUri != null) {
            previewImage.setImageURI(currentUri);
            previewImage.setVisibility(View.VISIBLE);
            placeholderIcon.setVisibility(View.GONE);
        } else if (currentBitmap != null) {
            previewImage.setImageBitmap(currentBitmap);
            previewImage.setVisibility(View.VISIBLE);
            placeholderIcon.setVisibility(View.GONE);
        } else {
            previewImage.setVisibility(View.INVISIBLE);
            placeholderIcon.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 좌/우 눈 선택 토글 UI 업데이트
     */
    private void updateEyeToggle() {
        leftEyeIcon.setImageResource(isLeftSelected ? R.drawable.ic_eye_black : R.drawable.ic_eye_gray);
        rightEyeIcon.setImageResource(isLeftSelected ? R.drawable.ic_eye_gray : R.drawable.ic_eye_black);
    }

    /**
     * 진단 버튼 활성화 상태 업데이트
     */
    private void updateCheckButtonState() {
        btnCheck.setEnabled(leftEyeUri != null || rightEyeUri != null);
    }

    /**
     * Bitmap 이미지를 임시 파일로 저장하고 Uri 반환
     */
    private Uri saveBitmapAndGetUri(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "eye_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
