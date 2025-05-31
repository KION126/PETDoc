package com.petdoc.aiCheck.skin.model;

import android.content.res.AssetManager;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SkinDiseasePredictor {

//    private Interpreter interpreter;
//
//    public SkinDiseasePredictor(AssetManager assetManager, String modelPath) throws IOException {
//        MappedByteBuffer modelBuffer = loadModelFile(assetManager, modelPath);
//        interpreter = new Interpreter(modelBuffer);
//    }
//
//    public float[] predict(ByteBuffer inputBuffer) {
//        float[][] output = new float[1][6]; // 예측 클래스 수 (6가지 증상)
//        interpreter.run(inputBuffer, output);
//        return output[0];
//    }
//
//    private MappedByteBuffer loadModelFile(AssetManager assetManager, String path) throws IOException {
//        try (FileInputStream inputStream = new FileInputStream(assetManager.openFd(path).getFileDescriptor())) {
//            FileChannel fileChannel = inputStream.getChannel();
//            long startOffset = assetManager.openFd(path).getStartOffset();
//            long declaredLength = assetManager.openFd(path).getDeclaredLength();
//            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
//        }
//    }
//
//    public void close() {
//        if (interpreter != null) interpreter.close();
//    }
}
