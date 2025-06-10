package com.petdoc.aiCheck.utils;

import android.graphics.Bitmap;
import android.util.Log; // Log 추가

public class ImageUtils {

    /**
     * Bitmap을 모델 입력에 맞는 크기로 전처리하고 정규화된 float 배열로 변환합니다.
     *
     * @param bitmap 원본 이미지 (null일 수 있음)
     * @param size 모델 입력 크기 (예: 224x224)
     * @return 정규화된 1차원 float 배열 (크기: size * size * 3). 입력 bitmap이 유효하지 않으면 null 반환.
     */
    public static float[] preprocess(Bitmap bitmap, int size) {
        // 입력 비트맵이 null이면 처리할 수 없으므로 null 반환
        if (bitmap == null) {
            Log.e("ImageUtils", "preprocess: 입력 Bitmap이 null입니다. 이미지 전처리를 수행할 수 없습니다.");
            return null;
        }

        // 비트맵을 모델 입력 크기(size x size)로 리사이즈
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);

        // 리사이즈된 비트맵이 null이면 (예: 메모리 부족 등) null 반환
        if (resizedBitmap == null) {
            Log.e("ImageUtils", "preprocess: resizedBitmap이 null입니다. 비트맵 리사이징에 실패했습니다.");
            return null;
        }

        // 모델 입력에 필요한 1차원 float 배열 초기화 (RGB 채널 포함)
        float[] inputData = new float[size * size * 3];
        int index = 0;

        // 픽셀 데이터를 [Height, Width, Channels] 순서로 배열에 저장
        // Bitmap.getPixel(x, y)는 (col, row) 또는 (width, height) 개념
        // 모델은 일반적으로 [height, width, channel] 순서를 기대합니다.
        for (int y = 0; y < size; y++) { // 높이 (row) 순회
            for (int x = 0; x < size; x++) { // 너비 (column) 순회
                int pixel = resizedBitmap.getPixel(x, y);

                // 각 픽셀의 R, G, B 값을 추출하고 0-255 범위를 0-1 범위로 정규화
                // (pixel >> 16) & 0xFF : Red 성분
                // (pixel >> 8) & 0xFF  : Green 성분
                // (pixel & 0xFF)       : Blue 성분
                inputData[index++] = ((pixel >> 16) & 0xFF) / 255.0f; // R (Red)
                inputData[index++] = ((pixel >> 8) & 0xFF) / 255.0f;  // G (Green)
                inputData[index++] = (pixel & 0xFF) / 255.0f;         // B (Blue)
            }
        }
        // 리사이즈된 비트맵 사용 후 메모리 해제
        if (resizedBitmap != bitmap) { // 원본과 다르면 해제
            resizedBitmap.recycle();
        }

        return inputData;
    }
}