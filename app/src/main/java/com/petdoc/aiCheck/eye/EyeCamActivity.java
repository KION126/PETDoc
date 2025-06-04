package com.petdoc.aiCheck.eye;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;

import com.petdoc.R;
import com.petdoc.login.CurrentPetManager;
import com.petdoc.main.BaseActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class EyeCamActivity extends BaseActivity {

    private ImageView previewImage;
    private ImageView placeholderIcon;
    private ImageButton albumButton;
    private ImageButton cameraButton;
    private ImageView leftEyeIcon;
    private ImageView rightEyeIcon;
    private Button btnCheck;

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
                        Uri photoUri = saveBitmapAndGetUri(photo);
                        if (photoUri != null) {
                            onImageSelected(photoUri);
                        }
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
            if (leftEyeUri == null && rightEyeUri == null) {
                Toast.makeText(this, "왼쪽 또는 오른쪽 이미지를 선택해 주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, EyeLoadingActivity.class);
            if (leftEyeUri != null) intent.putExtra("left_image_uri", leftEyeUri.toString());
            if (rightEyeUri != null) intent.putExtra("right_image_uri", rightEyeUri.toString());

            String petId = CurrentPetManager.getInstance().getCurrentPetId();
            if (petId != null) intent.putExtra("pet_id", petId);

            startActivity(intent);
        });

        updateCheckButtonState();
    }

    private void onImageSelected(Uri uri) {
        if (isLeftSelected) {
            leftEyeUri = uri;
        } else {
            rightEyeUri = uri;
        }
        updatePreview();
        updateCheckButtonState();
    }

    private void updatePreview() {
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

    private void updateCheckButtonState() {
        btnCheck.setEnabled(leftEyeUri != null || rightEyeUri != null);
    }

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
