package com.thor.fireclassification;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileInputStream;

public class TFLiteHandler {
    private final Interpreter tflite;
    private TensorImage inputImageBuffer;
    private final TensorBuffer outputProbabilityBuffer;
    private static final int NUM_THREADS = 4;
    private static final float[] IMAGE_MEAN = new float[]{128.0f, 128.0f, 128.0f};
    private static final float[] IMAGE_STD = new float[]{128.0f, 128.0f, 128.0f};
    private final ImageProcessor imageProcessor;



    public TFLiteHandler(String modelPath) throws Exception {

        File file = new File(modelPath);
        if (!file.exists()) {
            throw new Exception("model file is not exists!");
        }

        try {
            Interpreter.Options options = new Interpreter.Options();

            options.setNumThreads(NUM_THREADS);

            NnApiDelegate delegate = new NnApiDelegate();

            options.addDelegate(delegate);
            tflite = new Interpreter(file, options);

            int imageTensorIndex = 0;
            int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape();
            DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();

            int probabilityTensorIndex = 0;
            int[] probabilityShape =
                    tflite.getOutputTensor(probabilityTensorIndex).shape();
            DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

            inputImageBuffer = new TensorImage(imageDataType);
            outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);

            imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeOp(imageShape[1], imageShape[2], ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                    .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("load model fail!");
        }
    }


    public float[] predictImage(String image_path) throws Exception {
        if (!new File(image_path).exists()) {
            throw new Exception("image file is not exists!");
        }
        FileInputStream fis = new FileInputStream(image_path);
        Bitmap bitmap = BitmapFactory.decodeStream(fis);
        float[] result = predictImage(bitmap);
        if (bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return result;
    }


    public float[] predictImage(Bitmap bitmap) throws Exception {
        return predict(bitmap);
    }



    private TensorImage loadImage(final Bitmap bitmap) {
        inputImageBuffer.load(bitmap);
        return imageProcessor.process(inputImageBuffer);
    }


    private float[] predict(Bitmap bmp) throws Exception {
        inputImageBuffer = loadImage(bmp);

        try {
            tflite.run(inputImageBuffer.getBuffer(), outputProbabilityBuffer.getBuffer().rewind());
        } catch (Exception e) {
            throw new Exception("predict image fail! log:" + e);
        }
        float[] results = outputProbabilityBuffer.getFloatArray();
        int l = getMaxResult(results);
        return new float[]{l, results[l]};
    }

    public static int getMaxResult(float[] result) {
        float probability = 0;
        int r = 0;
        for (int i = 0; i < result.length; i++) {
            if (probability < result[i]) {
                probability = result[i];
                r = i;
            }
        }
        return r;
    }
}
