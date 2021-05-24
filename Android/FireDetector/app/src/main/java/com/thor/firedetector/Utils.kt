package com.thor.firedetector

import android.content.Context
import android.content.res.AssetManager
import android.util.Size
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Long
import java.util.*

@Suppress("UNREACHABLE_CODE")
class Utils {

     fun generateLabelsFile(assetManager: AssetManager, fileName: String?): ArrayList<String>? {
        val list = ArrayList<String>()
        val reader: BufferedReader?

        try {
            reader = BufferedReader(
                    InputStreamReader(assetManager.open(fileName!!)))
            var line: String
            while (reader.readLine().also {
                        line = it
            } != null) {
                list.add(line)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return list
    }


    fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int): Size? {
        val desiredSize = Size(width, height)

        // Collect the supported resolutions that are at least as big as the preview Surface
        var exactSizeFound = false
        val desiredAspectRatio = width * 1.0f / height //in landscape perspective
        var bestAspectRatio = 0f
        val bigEnough: MutableList<Size> = ArrayList()
        for (option in choices) {
            if (option == desiredSize) {
                // Set the size but don't return yet so that remaining sizes will still be logged.
                exactSizeFound = true
                break
            }
            val aspectRatio = option.width * 1.0f / option.height
            if (aspectRatio > desiredAspectRatio) continue  //smaller than screen
            //try to find the best aspect ratio which fits in screen
            if (aspectRatio > bestAspectRatio) {
                if (option.height >= height && option.width >= width) {
                    bigEnough.clear()
                    bigEnough.add(option)
                    bestAspectRatio = aspectRatio
                }
            } else if (aspectRatio == bestAspectRatio) {
                if (option.height >= height && option.width >= width) {
                    bigEnough.add(option)
                }
            }
        }
        if (exactSizeFound) {
            return desiredSize
        }
        return if (bigEnough.size > 0) {
            Collections.min(bigEnough) { p0, p1 ->
                Long.signum(
                        p0!!.width.toLong() * p0.height - p1!!.width.toLong() * p1.height)
            }
        } else {
            choices[0]
        }
    }

    @Throws(IOException::class)
    fun getFileFromAssets(context: Context, fileName: String): File = File(context.cacheDir, fileName)
            .also {
                if (!it.exists()) {
                    it.outputStream().use { cache ->
                        context.assets.open(fileName).use { inputStream ->
                            inputStream.copyTo(cache)
                        }
                    }
                }
            }
}