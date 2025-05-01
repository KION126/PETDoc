package com.petdoc.aicheck.utils;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;

public class ModelUtils {

    /**
     * Assets 폴더에서 샘플 이미지를 불러와 Bitmap으로 반환
     * @param assetManager AssetManager 객체
     * @param imageName 예: "img_sample_eye.jpg"
     * @return Bitmap 객체 (없으면 null)
     */
    public static Bitmap loadSampleImage(AssetManager assetManager, String imageName) {
        Bitmap bitmap = null;
        try (InputStream inputStream = assetManager.open(imageName)) {
            bitmap = BitmapFactory.decodeStream(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}
