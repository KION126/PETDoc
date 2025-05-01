package com.petdoc.aicheck.utils;

import android.graphics.Bitmap;

public class ImageUtils {

    /**
     * Bitmap을 모델 입력에 맞는 크기로 전처리하고 정규화된 float 배열로 변환
     * @param bitmap 원본 이미지
     * @param size 모델 입력 크기 (예: 224)
     * @return float[] (224 * 224 * 3)의 1차원 배열
     */
    public static float[] preprocess(Bitmap bitmap, int size) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);
        float[] inputData = new float[size * size * 3];

        int index = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int pixel = resizedBitmap.getPixel(i, j);
                inputData[index++] = ((pixel >> 16) & 0xFF) / 255.0f; // R
                inputData[index++] = ((pixel >> 8) & 0xFF) / 255.0f;  // G
                inputData[index++] = (pixel & 0xFF) / 255.0f;         // B
            }
        }

        return inputData;
    }
}
