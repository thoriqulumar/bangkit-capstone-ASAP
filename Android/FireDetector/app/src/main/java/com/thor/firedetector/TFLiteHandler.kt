package com.thor.firedetector

import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File

class TFLiteHandler {
    private var tflite: Interpreter? = null
    private var inputImageBuffer: TensorImage? = null
    private var outputProbabilityBuffer: TensorBuffer? = null
    private val NUM_THREADS = 4
    private val IMAGE_MEAN = floatArrayOf(128.0f, 128.0f, 128.0f)
    private val IMAGE_STD = floatArrayOf(128.0f, 128.0f, 128.0f)
    private var imageProcessor: ImageProcessor? = null

    private var imageSizeX = 0
    private var imageSizeY = 0


    fun TFLiteHandler(modelPath: String) {
        val file = File(modelPath)

        val options = Interpreter.Options()

        options.setNumThreads(NUM_THREADS)

        val delegate = NnApiDelegate()

        options.addDelegate(delegate)
        tflite = Interpreter(file, options)


         val imageTensorIndex = 0
         val imageShape = tflite!!.getInputTensor(imageTensorIndex).shape() // {1, height, width, 3}

         imageSizeY = imageShape[1]
         imageSizeX = imageShape[2]
         val imageDataType = tflite!!.getInputTensor(imageTensorIndex).dataType()

         val probabilityTensorIndex = 0
         val probabilityShape = tflite!!.getOutputTensor(probabilityTensorIndex).shape() // {1, NUM_CLASSES}

         val probabilityDataType = tflite!!.getOutputTensor(probabilityTensorIndex).dataType()

         inputImageBuffer = TensorImage(imageDataType)

        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType)


        imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(imageShape[1], imageShape[2], ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
        .add(NormalizeOp(IMAGE_MEAN, IMAGE_STD))
        .build()
    }




    private fun loadImage(bitmap: Bitmap): TensorImage {
        // Loads bitmap into a TensorImage.
        inputImageBuffer!!.load(bitmap)
        return imageProcessor!!.process(inputImageBuffer)
    }


    @Throws(Exception::class)
    private fun predict(bmp: Bitmap): FloatArray? {
        inputImageBuffer = loadImage(bmp)
        try {
            tflite!!.run(inputImageBuffer!!.buffer, outputProbabilityBuffer!!.buffer.rewind())
        } catch (e: Exception) {
            throw Exception("predict image fail! log:$e")
        }
        val results = outputProbabilityBuffer!!.floatArray
        val l = getArgMax(results)
        return floatArrayOf(l.toFloat(), results[l])
    }


    fun getArgMax(result: FloatArray): Int {
        var probability = 0f
        var r = 0
        for (i in result.indices) {
            if (probability < result[i]) {
                probability = result[i]
                r = i
            }
        }
        return r
    }
}