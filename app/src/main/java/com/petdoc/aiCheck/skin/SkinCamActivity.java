package com.petdoc.aiCheck.skin;

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
import com.petdoc.main.BaseActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 피부 이미지 입력 페이지
 */
public class SkinCamActivity extends BaseActivity {

    private ImageView previewImage;
    private ImageView placeholderIcon;
    private ImageButton albumButton;
    private ImageButton cameraButton;
    private Button btnCheck;

    private Uri selectedImageUri;

    // 갤러리에서 이미지 선택 처리
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri originalUri = result.getData().getData();
                    if (originalUri != null) {
                        selectedImageUri = copyToAppCache(originalUri);
                        updatePreview();
                        updateCheckButtonState();
                    }
                }
            });

    // 카메라 촬영 이미지 처리
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                    if (photo != null) {
                        selectedImageUri = saveBitmapAndGetUri(photo);
                        updatePreview();
                        updateCheckButtonState();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_skin_cam);

        previewImage = findViewById(R.id.previewImage);
        placeholderIcon = findViewById(R.id.placeholderIcon);
        albumButton = findViewById(R.id.albumButton);
        cameraButton = findViewById(R.id.cameraButton);
        btnCheck = findViewById(R.id.btnCheck);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        albumButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        cameraButton.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        });

        btnCheck.setOnClickListener(v -> {
            if (selectedImageUri != null) {
                Intent intent = new Intent(this, SkinLoadingActivity.class);
                intent.putExtra("image_uri", selectedImageUri.toString());
                startActivity(intent);
            } else {
                Toast.makeText(this, "이미지를 선택해 주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        updateCheckButtonState();
    }

    private void updatePreview() {
        if (selectedImageUri != null) {
            previewImage.setImageURI(selectedImageUri);
            previewImage.setVisibility(View.VISIBLE);
            placeholderIcon.setVisibility(View.GONE);
        } else {
            previewImage.setVisibility(View.INVISIBLE);
            placeholderIcon.setVisibility(View.VISIBLE);
        }
    }

    private void updateCheckButtonState() {
        btnCheck.setEnabled(selectedImageUri != null);
    }

    private Uri saveBitmapAndGetUri(Bitmap bitmap) {
        try {
            File file = new File(getCacheDir(), "skin_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream out = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            }
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Uri copyToAppCache(Uri srcUri) {
        try {
            InputStream in = getContentResolver().openInputStream(srcUri);
            if (in == null) return null;

            File file = new File(getCacheDir(), "skin_" + System.currentTimeMillis() + ".jpg");
            try (FileOutputStream out = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }
            return FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
