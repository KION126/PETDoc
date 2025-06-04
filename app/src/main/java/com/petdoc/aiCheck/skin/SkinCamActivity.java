package com.petdoc.aiCheck.skin;

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
import com.petdoc.aiCheck.utils.ImageUtils;
import com.petdoc.main.BaseActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 피부 이미지 입력 페이지
 */
public class SkinCamActivity extends BaseActivity {

    private ImageView previewImage;
    private ImageView placeholderIcon;
    private LinearLayout albumButton;
    private LinearLayout cameraButton;
    private Button btnCheck;

    private Bitmap selectedBitmap;
    private Uri selectedImageUri;

    // 갤러리에서 이미지 선택 처리
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        selectedImageUri = imageUri;
                        selectedBitmap = null;
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
                        selectedBitmap = photo;
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

        // 뷰 초기화
        previewImage = findViewById(R.id.previewImage);
        placeholderIcon = findViewById(R.id.placeholderIcon);
        albumButton = findViewById(R.id.albumButton);
        cameraButton = findViewById(R.id.cameraButton);
        btnCheck = findViewById(R.id.btnCheck);

        // 뒤로 가기
        findViewById(R.id.backButton).setOnClickListener(v -> finish());

        // 앨범에서 이미지 선택
        albumButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        // 카메라로 사진 촬영
        cameraButton.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        });

        // 검진하기 버튼 클릭 시 로딩 액티비티로 전환
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

    // 미리보기 이미지 갱신
    private void updatePreview() {
        if (selectedImageUri != null) {
            previewImage.setImageURI(selectedImageUri);
            previewImage.setVisibility(View.VISIBLE);
            placeholderIcon.setVisibility(View.GONE);
        } else if (selectedBitmap != null) {
            previewImage.setImageBitmap(selectedBitmap);
            previewImage.setVisibility(View.VISIBLE);
            placeholderIcon.setVisibility(View.GONE);
        } else {
            previewImage.setVisibility(View.INVISIBLE);
            placeholderIcon.setVisibility(View.VISIBLE);
        }
    }

    // 검진 버튼 활성화 여부 갱신
    private void updateCheckButtonState() {
        btnCheck.setEnabled(selectedImageUri != null);
    }

    // Bitmap 이미지를 파일로 저장하고 Uri 반환
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
}
