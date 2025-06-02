// ✅ PhotoInputActivity.java (최종 저장 및 업로드 액티비티)
package com.petdoc.login;

// 📌 기존 import 유지 + 추가된 유틸 클래스 import
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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
import com.petdoc.login.utils.PetInfoUtils;
import com.petdoc.main.MainActivity;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PhotoInputActivity extends AppCompatActivity {

    //  사진 등록 UI 요소
    private ImageView previewImage, placeholderIcon;
    private LinearLayout albumBtn, cameraBtn;
    private ImageButton btnNext, btnPrev;
    private TextView tvPetName, tvPetPhotoTitle;

    //  사진 및 Firebase 관련 변수
    private Uri selectedImageUri = null;
    private String currentPhotoPath;
    private String uid, petName;
    private DatabaseReference dbRef;
    private StorageReference storageRef;

    //  앨범 선택 런처
    private final ActivityResultLauncher<Intent> albumLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    showPreview(selectedImageUri);
                    enableNextButton();
                }
            });

    //  카메라 촬영 런처
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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pet_photo);

        //  뷰 연결
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

        //  Firebase 초기화
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        uid = user.getUid();
        dbRef = FirebaseDatabase.getInstance().getReference();
        storageRef = FirebaseStorage.getInstance().getReference();

        //  이름 가져오기
        petName = getIntent().getStringExtra("petName");

        //  상단 이름 표시
        if (petName != null && !petName.isEmpty()) {
            tvPetName.setText(petName);
            tvPetPhotoTitle.setText(petName + "의 사진을 올려주세요");
        } else {
            tvPetName.setText("멍멍이 이름");
            tvPetPhotoTitle.setText("반려견의 사진을 올려주세요");
        }

        // 이미지 상태 초기화
        previewImage.setVisibility(ImageView.INVISIBLE);
        placeholderIcon.setVisibility(ImageView.VISIBLE);

        //  이전으로 돌아가기
        btnPrev.setOnClickListener(v -> {
            Intent intent = new Intent(PhotoInputActivity.this, WeightInputActivity.class);
            intent.putExtras(getIntent());
            startActivity(intent);
            finish();
        });

        //  앨범에서 사진 선택
        albumBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            albumLauncher.launch(intent);
        });

        //  카메라 촬영
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

        //  최종 완료 → Dog 키 생성 + 이미지 업로드 + 전체 데이터 저장
        btnNext.setOnClickListener(v -> {
            if (selectedImageUri == null) return;

            dbRef.child("Users").child(uid).get().addOnSuccessListener(snapshot -> {
                int petCount = (int) snapshot.getChildrenCount();
                String newPetKey = "Dog" + (petCount + 1);
                uploadImageAndSaveInfo(newPetKey);
            });
        });

        //  나중에 등록하기 → Dog 키 생성 후 현재까지 입력된 데이터만 저장
        findViewById(R.id.imgRegisterLater).setOnClickListener(v -> {
            dbRef.child("Users").child(uid).get().addOnSuccessListener(snapshot -> {
                int petCount = (int) snapshot.getChildrenCount();
                String newPetKey = "Dog" + (petCount + 1);

                PetInfoUtils.updateCompletedDogInfo(getIntent().getExtras(), newPetKey);

                Intent intent = new Intent(PhotoInputActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            });
        });
    }

    //  이미지 업로드 후 전체 정보 저장
    private void uploadImageAndSaveInfo(String petKey) {
        if (selectedImageUri == null) return;

        String fileName = "profile.jpg";
        StorageReference imgRef = storageRef.child("pet_profiles/" + uid + "/" + petKey + "/" + fileName);

        imgRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> imgRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();

                    Bundle data = getIntent().getExtras();
                    if (data != null) data.putString("petImagePath", imageUrl);

                    PetInfoUtils.savePartialDogInfo(data, petKey);

                    Intent intent = new Intent(PhotoInputActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "사진 업로드 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    //  이미지 미리보기 표시
    private void showPreview(Uri uri) {
        if (uri != null) {
            Glide.with(this).load(uri).into(previewImage);
            previewImage.setVisibility(ImageView.VISIBLE);
            placeholderIcon.setVisibility(ImageView.INVISIBLE);
        }
    }

    //  다음 버튼 활성화
    private void enableNextButton() {
        btnNext.setEnabled(true);
        btnNext.setImageResource(R.drawable.ic_arrow_forward2);
    }

    //  이미지 파일 생성 (카메라용)
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
