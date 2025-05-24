package com.petdoc.login;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.petdoc.R;
import com.petdoc.main.MainActivity;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PhotoInputActivity extends AppCompatActivity {

    private ImageView imgPhotoFrame, placeholderIcon, previewImage;
    private LinearLayout albumBtn, cameraBtn;
    private ImageButton btnNext, btnPrev;
    private TextView tvPetName, tvPetPhotoTitle;

    private Uri selectedImageUri = null;
    private String currentPhotoPath;

    private String uid, petKey, petName;
    private DatabaseReference dbRef;
    private StorageReference storageRef;

    private final ActivityResultLauncher<Intent> albumLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    showPreview(selectedImageUri);
                    enableNextButton();
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    File file = new File(currentPhotoPath);
                    selectedImageUri = Uri.fromFile(file);
                    showPreview(selectedImageUri);
                    enableNextButton();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_photo);

        // 뷰 연결
        tvPetName = findViewById(R.id.tvPetName);
        tvPetPhotoTitle = findViewById(R.id.tvPetPhotoTitle);
        previewImage = findViewById(R.id.previewImage);
        placeholderIcon = findViewById(R.id.placeholderIcon);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        albumBtn = findViewById(R.id.albumButton);
        cameraBtn = findViewById(R.id.cameraButton);

        btnNext.setEnabled(false);
        btnNext.setImageResource(R.drawable.ic_arrow_forward);

        // 파라미터 받기
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        uid = user.getUid();
        dbRef = FirebaseDatabase.getInstance().getReference();
        storageRef = FirebaseStorage.getInstance().getReference();

        petKey = getIntent().getStringExtra("petKey");
        petName = getIntent().getStringExtra("petName");
        if (petKey == null) {
            Toast.makeText(this, "반려견 정보 없음", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 이름 표시
        if (petName != null && !petName.isEmpty()) {
            tvPetName.setText(petName);
            tvPetPhotoTitle.setText(petName + "의 사진을 올려주세요");
        } else {
            tvPetName.setText("멍멍이 이름");
            tvPetPhotoTitle.setText("반려견의 사진을 올려주세요");
        }

        // 처음에는 미리보기 invisible, 플레이스홀더 visible
        previewImage.setVisibility(ImageView.INVISIBLE);
        placeholderIcon.setVisibility(ImageView.VISIBLE);

        // 이전 버튼: WeightInputActivity로
        btnPrev.setOnClickListener(v -> {
            Intent intent = new Intent(PhotoInputActivity.this, WeightInputActivity.class);
            intent.putExtra("petKey", petKey);
            intent.putExtra("petName", petName);
            startActivity(intent);
            finish();
        });

        // 앨범에서 선택
        albumBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            albumLauncher.launch(intent);
        });

        // 카메라 촬영
        cameraBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = createImageFile();
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(intent);
            }
        });

        // 다음 버튼 → 사진 업로드 및 메인으로 이동
        btnNext.setOnClickListener(v -> {
            if (selectedImageUri == null) return;

            String fileName = "profile.jpg";
            StorageReference imgRef = storageRef.child("pet_profiles/" + uid + "/" + petKey + "/" + fileName);

            imgRef.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        dbRef.child("Users").child(uid).child(petKey)
                                .child("기본정보").child("이미지파일경로로")
                                .setValue(imageUrl)
                                .addOnSuccessListener(unused -> {
                                    Intent intent = new Intent(PhotoInputActivity.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                });
                    }))
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "사진 업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    // 이미지 미리보기 처리
    private void showPreview(Uri uri) {
        if (uri != null) {
            Glide.with(this).load(uri).into(previewImage);
            previewImage.setVisibility(ImageView.VISIBLE);
            placeholderIcon.setVisibility(ImageView.INVISIBLE);
        }
    }

    private void enableNextButton() {
        btnNext.setEnabled(true);
        btnNext.setImageResource(R.drawable.ic_arrow_forward2);
    }

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
}
