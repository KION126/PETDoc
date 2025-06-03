package com.petdoc.genetic;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.petdoc.R;
import com.petdoc.main.BaseActivity;
import com.petdoc.main.MainActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GeneticNoteActivity extends BaseActivity {

    private Button btnNext;
    private ImageButton btnBack;
    private ImageView btnAlbem;
    private ImageView btnCamera;
    private Uri selectedImageUri = null;
    private ImageView ivPhotoFrame;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ai_genetic);

        // 뷰 연결
        btnNext = findViewById(R.id.btn_next);
        btnBack = findViewById(R.id.btn_back);
        btnAlbem = findViewById(R.id.btn_album);
        btnCamera = findViewById(R.id.btn_carmera);
        ivPhotoFrame = findViewById(R.id.iv_photo_frame);

        // 앨범 버튼
        btnAlbem.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            albumLauncher.launch(intent);
        });

        // 카메라 버튼
        btnCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = createImageFile();
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(intent);
            }
        });

        // 다음 페이지 (로딩 페이지)
        btnNext.setOnClickListener(v -> {
            Intent intent = new Intent(GeneticNoteActivity.this, GeneticLoadingActivity.class);

            // 이미지 경로 전달
            if (selectedImageUri != null) {
                intent.putExtra("image_uri", selectedImageUri.toString());
            }
            startActivity(intent);
        });

        // 이전 페이지
        btnBack.setOnClickListener(v -> {
            Intent intent = new Intent(GeneticNoteActivity.this, MainActivity.class);
            startActivity(intent);
        });

        // 다음 버튼 초기 설정
        btnNext.setEnabled(false);
    }

    // 이미지 저장 메서드
    private File createImageFile() {
        try {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File storageDir = getExternalFilesDir(null);
            File image = File.createTempFile("JPEG_" + timeStamp, ".jpg", storageDir);
            currentPhotoPath = image.getAbsolutePath();
            return image;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // 앨범 접근 및 이미지 로드 메서드
    private final ActivityResultLauncher<Intent> albumLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this)
                            .load(selectedImageUri)
                            .centerCrop()
                            .into(ivPhotoFrame);
                    btnNext.setEnabled(true);
                }
            });

    // 카메라 접근 및 이미지 로드 메서드
    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    File file = new File(currentPhotoPath);
                    selectedImageUri = Uri.fromFile(file);
                    Glide.with(this)
                            .load(selectedImageUri)
                            .centerCrop()
                            .into(ivPhotoFrame);
                    btnNext.setEnabled(true);
                }
            });
}
