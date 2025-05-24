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

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;

/**
 * 안구 이미지 입력 페이지
 */
public class EyeCamActivity extends AppCompatActivity {

    private ImageView previewImage;
    private ImageView placeholderIcon;
    private LinearLayout albumButton;
    private LinearLayout cameraButton;
    private ImageView leftEyeIcon;
    private ImageView rightEyeIcon;
    private Button btnCheck;

    private Bitmap leftEyeBitmap;
    private Bitmap rightEyeBitmap;
    private Uri leftEyeUri;
    private Uri rightEyeUri;

    private boolean isLeftSelected = true;

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

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    if (photo != null) {
                        // 카메라는 임시 URI가 없기 때문에 직접 처리 필요 (예: 저장 후 URI 생성)
                        onCameraCaptured(photo);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_eye_cam);

        previewImage = findViewById(R.id.previewImage);
        placeholderIcon = findViewById(R.id.placeholderIcon);
        albumButton = findViewById(R.id.albumButton);
        cameraButton = findViewById(R.id.cameraButton);
        leftEyeIcon = findViewById(R.id.leftEyeIcon);
        rightEyeIcon = findViewById(R.id.rightEyeIcon);
        btnCheck = findViewById(R.id.btnCheck);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        leftEyeIcon.setOnClickListener(v -> {
            isLeftSelected = true;
            updateEyeToggle();
            updatePreview();
        });

        rightEyeIcon.setOnClickListener(v -> {
            isLeftSelected = false;
            updateEyeToggle();
            updatePreview();
        });

        albumButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        cameraButton.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        });

        btnCheck.setOnClickListener(v -> {
            Uri selectedUri = isLeftSelected ? leftEyeUri : rightEyeUri;
            if (selectedUri != null) {
                Intent intent = new Intent(this, EyeLoadingActivity.class);
                intent.putExtra("eye_side", isLeftSelected ? "left" : "right");
                intent.putExtra("image_uri", selectedUri.toString());
                startActivity(intent);
            }
        });
    }

    private void onImageSelected(Uri uri) {
        if (isLeftSelected) {
            leftEyeUri = uri;
        } else {
            rightEyeUri = uri;
        }
        updatePreview();
        btnCheck.setEnabled(true);
    }

    private void onCameraCaptured(Bitmap bitmap) {
        // 미리보기만 업데이트하고 URI는 설정하지 않음
        if (isLeftSelected) {
            leftEyeBitmap = bitmap;
            leftEyeUri = saveBitmapAndGetUri(bitmap);
        } else {
            rightEyeBitmap = bitmap;
            rightEyeUri = saveBitmapAndGetUri(bitmap);
        }
        updatePreview();
        btnCheck.setEnabled(true);
    }

    private void updatePreview() {
        Bitmap currentBitmap = isLeftSelected ? leftEyeBitmap : rightEyeBitmap;
        Uri currentUri = isLeftSelected ? leftEyeUri : rightEyeUri;

        if (currentUri != null) {
            previewImage.setImageURI(currentUri);
            previewImage.setVisibility(View.VISIBLE);
            placeholderIcon.setVisibility(View.GONE);
        } else {
            previewImage.setVisibility(View.INVISIBLE);
            placeholderIcon.setVisibility(View.VISIBLE);
        }
    }

    private void updateEyeToggle() {
        leftEyeIcon.setImageResource(isLeftSelected ? R.drawable.ic_eye_black : R.drawable.ic_eye_gray);
        rightEyeIcon.setImageResource(isLeftSelected ? R.drawable.ic_eye_gray : R.drawable.ic_eye_black);
    }

    private Uri saveBitmapAndGetUri(Bitmap bitmap) {
        // 실제 구현 필요: 비트맵을 파일로 저장한 후 URI 반환
        return null; // TODO: 구현할 것
    }
}
