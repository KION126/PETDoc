package com.petdoc.main.model;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.tensorflow.lite.Interpreter;

public class EyeDiseasePredictor {

    private Interpreter tflite;

    private static final int NUM_CLASSES = 10;

    public EyeDiseasePredictor(AssetManager assetManager, String modelPath) throws IOException {
        Interpreter.Options options = new Interpreter.Options();
        options.setUseXNNPACK(false);
        tflite = new Interpreter(loadModelFile(assetManager, modelPath));
    }

    private MappedByteBuffer loadModelFile(AssetManager assets, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    public float[] predict(float[] inputDataFlat) {
        // 입력을 4D 텐서 형태로 변환: [1, 224, 224, 3]
        float[][][][] inputData = new float[1][224][224][3];

        int index = 0;
        for (int i = 0; i < 224; i++) {
            for (int j = 0; j < 224; j++) {
                for (int k = 0; k < 3; k++) {
                    inputData[0][i][j][k] = inputDataFlat[index++];
                }
            }
        }

        float[][] outputData = new float[1][NUM_CLASSES];
        tflite.run(inputData, outputData);
        return outputData[0];
    }


    public void close() {
        if (tflite != null) {
            tflite.close();
        }
    }
}
