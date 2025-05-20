package com.petdoc.login;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    private ImageView imgPhotoFrame;
    private LinearLayout albumBtn, cameraBtn;
    private ImageButton btnNext;

    private Uri selectedImageUri = null;
    private String currentPhotoPath;

    private String uid, petKey;
    private DatabaseReference dbRef;
    private StorageReference storageRef;

    private final ActivityResultLauncher<Intent> albumLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    Glide.with(this).load(selectedImageUri).into(imgPhotoFrame);
                    enableNextButton();
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    File file = new File(currentPhotoPath);
                    selectedImageUri = Uri.fromFile(file);
                    Glide.with(this).load(selectedImageUri).into(imgPhotoFrame);
                    enableNextButton();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pet_photo);

        imgPhotoFrame = findViewById(R.id.imgPhotoFrame);
        btnNext = findViewById(R.id.btnNext);
        albumBtn = findViewById(R.id.layoutAlbum);
        cameraBtn = findViewById(R.id.layoutCamera);

        btnNext.setEnabled(false);
        btnNext.setImageResource(R.drawable.ic_arrow_forward);

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
        if (petKey == null) {
            Toast.makeText(this, "반려견 정보 없음", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        albumBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            albumLauncher.launch(intent);
        });

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