package com.petdoc.aiCheck.eye.model;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log; // Log 클래스 추가

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer; // ByteBuffer 클래스 추가
import java.nio.ByteOrder;  // ByteOrder 클래스 추가
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.tensorflow.lite.Interpreter;

public class EyeDiseasePredictor {

    // TensorFlow Lite 모델 실행을 위한 인터프리터 객체
    private Interpreter tflite;

    // 모델이 예측하는 질병 클래스의 수
    private static final int NUM_CLASSES = 10;

    // 모델 입력 이미지의 예상 크기 (높이와 너비 모두 224 픽셀)
    private static final int IMAGE_SIZE = 224;
    // 모델 입력 이미지의 채널 수 (RGB이므로 3)
    private static final int NUM_CHANNELS = 3;
    // float 데이터 타입은 4바이트
    private static final int FLOAT_TYPE_SIZE_BYTES = 4;

    /**
     * 생성자: AssetManager를 통해 모델 파일(`.tflite`)을 로드하고 TensorFlow Lite 인터프리터를 초기화합니다.
     *
     * @param assetManager 애셋(assets) 폴더에 접근하기 위한 AssetManager 객체
     * @param modelPath 애셋 폴더 내 모델 파일의 경로 (예: "eye-010-0.7412.tflite")
     * @throws IOException 모델 파일 로딩에 실패할 경우 발생
     */
    public EyeDiseasePredictor(AssetManager assetManager, String modelPath) throws IOException {
        // 인터프리터 설정 옵션 생성
        Interpreter.Options options = new Interpreter.Options();
        // XNNPACK은 TensorFlow Lite의 성능을 향상시키는 최적화 옵션입니다.
        // 특정 상황에서 호환성 문제가 있을 수 있으나, 일반적으로는 활성화하는 것이 좋습니다.
        // 현재는 false로 설정되어 있습니다.
        options.setUseXNNPACK(false);

        // 모델 파일을 메모리에 매핑하고, 설정된 옵션으로 인터프리터를 초기화합니다.
        tflite = new Interpreter(loadModelFile(assetManager, modelPath), options);
        Log.d("EyeDiseasePredictor", "TensorFlow Lite 모델 로드 및 인터프리터 초기화 완료: " + modelPath);

        // 모델의 입출력 텐서 정보 로그 출력 (디버깅에 유용)
        Log.d("EyeDiseasePredictor", "모델 입력 텐서 수: " + tflite.getInputTensorCount());
        Log.d("EyeDiseasePredictor", "모델 출력 텐서 수: " + tflite.getOutputTensorCount());
        if (tflite.getInputTensorCount() > 0) {
            Log.d("EyeDiseasePredictor", "입력 텐서 Shape: " + java.util.Arrays.toString(tflite.getInputTensor(0).shape()));
            Log.d("EyeDiseasePredictor", "입력 텐서 Type: " + tflite.getInputTensor(0).dataType());
        }
    }

    /**
     * 애셋(assets) 폴더에 있는 모델 파일을 메모리에 매핑하여 `MappedByteBuffer`로 반환합니다.
     * 이는 모델 파일을 효율적으로 메모리에 로드하는 표준 방식입니다.
     *
     * @param assets AssetManager 인스턴스
     * @param modelPath 모델 파일 경로
     * @return 메모리에 매핑된 모델 파일 데이터 (`MappedByteBuffer`)
     * @throws IOException 파일 로딩 실패 시 예외 발생
     */
    private MappedByteBuffer loadModelFile(AssetManager assets, String modelPath) throws IOException {
        // AssetManager를 사용하여 모델 파일의 파일 디스크립터(FileDescriptor)를 얻습니다.
        AssetFileDescriptor fileDescriptor = assets.openFd(modelPath);
        // 파일 디스크립터로부터 파일 입력 스트림을 생성합니다.
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        // 파일 스트림으로부터 파일 채널을 얻습니다. 파일 채널은 메모리 매핑에 사용됩니다.
        FileChannel fileChannel = inputStream.getChannel();
        // 파일 디스크립터에서 모델 파일의 시작 오프셋(offset)과 선언된 길이(length)를 가져옵니다.
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        // 파일의 특정 부분을 메모리에 매핑하여 읽기 전용(`READ_ONLY`) 버퍼로 반환합니다.
        // 이 방식은 모델 파일에 더 빠르게 접근할 수 있도록 도와줍니다.
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * 전처리된 1차원 이미지 데이터 배열을 입력받아 모델 예측을 수행하고 클래스별 확률을 반환합니다.
     * 이 메서드는 `ByteBuffer`를 사용하여 모델에 데이터를 전달하도록 변경되었습니다.
     *
     * @param inputDataFlat `ImageUtils.preprocess`를 통해 전처리된 1차원 `float` 배열 (크기: IMAGE_SIZE * IMAGE_SIZE * NUM_CHANNELS)
     * @return 각 질병 클래스에 대한 예측 확률을 담은 1차원 `float` 배열
     */
    public float[] predict(float[] inputDataFlat) {
        // 입력 배열의 크기가 모델이 기대하는 크기와 일치하는지 확인합니다.
        // IMAGE_SIZE * IMAGE_SIZE * NUM_CHANNELS = 224 * 224 * 3 = 150528
        if (inputDataFlat == null || inputDataFlat.length != IMAGE_SIZE * IMAGE_SIZE * NUM_CHANNELS) {
            Log.e("EyeDiseasePredictor", "입력 데이터 배열의 크기가 올바르지 않습니다. 예상: " +
                    (IMAGE_SIZE * IMAGE_SIZE * NUM_CHANNELS) + ", 실제: " +
                    (inputDataFlat != null ? inputDataFlat.length : "null"));
            // 잘못된 입력에 대해 null 또는 기본값을 반환하여 크래시 방지
            return new float[NUM_CLASSES]; // 0으로 채워진 배열 반환 또는 예외 발생
        }

        // 모델 입력에 사용할 ByteBuffer를 생성합니다.
        // 크기: 이미지 크기 * 이미지 크기 * 채널 수 * float의 바이트 크기
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(
                IMAGE_SIZE * IMAGE_SIZE * NUM_CHANNELS * FLOAT_TYPE_SIZE_BYTES);
        // 바이트 순서(endianness)를 모델과 동일하게 설정합니다. (일반적으로 Native Order)
        inputBuffer.order(ByteOrder.nativeOrder());

        // 1차원 float 배열의 데이터를 ByteBuffer에 복사합니다.
        // ImageUtils.preprocess에서 픽셀 데이터를 올바른 [Height, Width, Channels] 순서로
        // 1차원 배열에 채웠다고 가정합니다.
        for (float pixelValue : inputDataFlat) {
            inputBuffer.putFloat(pixelValue);
        }
        // 버퍼의 위치를 시작으로 재설정 (읽기 준비)
        inputBuffer.rewind();

        // 모델의 출력값을 담을 배열을 생성합니다.
        // [1, NUM_CLASSES] 형태로, 1은 배치 크기, NUM_CLASSES는 예측 클래스(질병)의 수입니다.
        float[][] outputData = new float[1][NUM_CLASSES];

        // TensorFlow Lite 인터프리터를 실행하여 예측을 수행합니다.
        // inputBuffer는 입력 텐서(ByteBuffer), outputData는 결과를 받을 출력 텐서(float[][])입니다.
        try {
            tflite.run(inputBuffer, outputData);
            Log.d("EyeDiseasePredictor", "모델 예측 실행 완료.");
        } catch (IllegalArgumentException e) {
            // "num_input_elements != num_output_elements"와 같은 내부 에러를 처리
            Log.e("EyeDiseasePredictor", "TensorFlow Lite 예측 중 오류 발생: " + e.getMessage(), e);
            return new float[NUM_CLASSES]; // 오류 발생 시 0으로 채워진 배열 반환
        } catch (Exception e) {
            Log.e("EyeDiseasePredictor", "모델 예측 중 알 수 없는 오류 발생: " + e.getMessage(), e);
            return new float[NUM_CLASSES]; // 오류 발생 시 0으로 채워진 배열 반환
        }


        // 첫 번째 배치(즉, 현재 이미지)에 대한 예측 결과(각 클래스별 확률)를 반환합니다.
        return outputData[0];
    }

    /**
     * TensorFlow Lite 인터프리터가 사용하는 모든 리소스를 해제합니다.
     * 액티비티 또는 애플리케이션 종료 시 반드시 호출하여 메모리 누수를 방지해야 합니다.
     */
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null; // null로 설정하여 중복 해제 방지 및 가비지 컬렉션 도움
            Log.d("EyeDiseasePredictor", "TensorFlow Lite 인터프리터 리소스 해제 완료.");
        }
    }
}