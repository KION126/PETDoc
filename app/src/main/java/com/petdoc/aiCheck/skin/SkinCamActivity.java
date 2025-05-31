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

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.petdoc.R;
import com.petdoc.aiCheck.utils.ImageUtils;

import java.io.IOException;

/**
 * 피부 이미지 입력 페이지
 */
public class SkinCamActivity extends AppCompatActivity {

    // UI 요소
    private ImageView previewImage;
    private ImageView placeholderIcon;
    private LinearLayout albumButton;
    private LinearLayout cameraButton;
    private Button btnCheck;

    // 선택된 이미지
    private Bitmap selectedBitmap;

    // 앨범에서 이미지 선택 처리
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                            onImageSelected(bitmap);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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
                        onImageSelected(photo);
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

        // 앨범 버튼 클릭
        albumButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        // 카메라 버튼 클릭
        cameraButton.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(intent);
        });

//        // 검진하기 버튼 클릭
//        btnCheck.setOnClickListener(v -> {
//            if (selectedBitmap != null) {
//                Intent intent = new Intent(this, SkinLoadingActivity.class);
//                intent.putExtra("image_bitmap", ImageUtils.bitmapToByteArray(selectedBitmap));
//                startActivity(intent);
//            }
//        });
    }

    /**
     * 이미지 선택 시 미리보기와 버튼 상태 갱신
     */
    private void onImageSelected(Bitmap bitmap) {
        selectedBitmap = bitmap;
        if (bitmap != null) {
            previewImage.setImageBitmap(bitmap);
            previewImage.setVisibility(View.VISIBLE);
            placeholderIcon.setVisibility(View.GONE);
            btnCheck.setEnabled(true);
        } else {
            previewImage.setVisibility(View.INVISIBLE);
            placeholderIcon.setVisibility(View.VISIBLE);
            btnCheck.setEnabled(false);
        }
    }
}
