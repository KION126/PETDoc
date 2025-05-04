package com.petdoc.aicheck.eye.model;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.tensorflow.lite.Interpreter;

public class EyeDiseasePredictor {

    // TensorflowLite 모델 실행을 위한 인터프리터 생성
    private Interpreter tflite;

    // 모델 클래스 수 (10개의 질병)
    private static final int NUM_CLASSES = 10;

    /**
     * 생성자: asset에서 모델 파일(eye-010-0.7412.tflite) 로드 및 인터프리터 초기화
     *
     * @param assetManager 애셋 접근을 위한 AssetManager
     * @param modelPath 모델 파일 경로 (.tflite)
     * @throws IOException 모델 파일 로딩 실패 시 예외 발생
     */
    public EyeDiseasePredictor(AssetManager assetManager, String modelPath) throws IOException {
        // 인터프리터 옵션 설정 (XNNPACK 사용 x)
        Interpreter.Options options = new Interpreter.Options();
        options.setUseXNNPACK(false);

        // 모델 파일 로드 및 인터프리터 초기화
        tflite = new Interpreter(loadModelFile(assetManager, modelPath));
    }

    /**
     * 메모리 로드을 위한 MappedByteBuffer 변환
     *
     * @param assets AssetManager 인스턴스
     * @param modelPath 모델 파일 경로
     * @return 메모리에 매핑된 모델 파일
     * @throws IOException 파일 로딩 실패 시 예외 발생
     */
    private MappedByteBuffer loadModelFile(AssetManager assets, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        // 파일을 메모리에 매핑 -> 읽기 전용으로 반환
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * 입력 이미지에 따른 예측 결과 (클래스 단위로 반환)
     *
     * @param inputDataFlat [1, 224, 224, 3] 형태로 변환 가능한 1차원 배열
     * @return 클래스별 예측 확률
     */
    public float[] predict(float[] inputDataFlat) {
        // 1차원 입력을 4D 텐서 형태로 변환: [1, 224, 224, 3]
        float[][][][] inputData = new float[1][224][224][3];

        int index = 0;
        for (int i = 0; i < 224; i++) {
            for (int j = 0; j < 224; j++) {
                for (int k = 0; k < 3; k++) {
                    inputData[0][i][j][k] = inputDataFlat[index++];
                }
            }
        }

        // 출력값을 담을 배열 생성
        float[][] outputData = new float[1][NUM_CLASSES];

        // 예측 모델 실행
        tflite.run(inputData, outputData);

        return outputData[0];
    }

    /**
     * 인터프리터 리소스 해제
     */
    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}
