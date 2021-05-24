package com.thor.firedetector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var customTextureView: CustomTextureView
    lateinit var tvResult: TextView

    lateinit var allLabels: ArrayList<String>
    private var tfLiteClassificationUtil: TFLiteHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!checkPermission()){
            requestPermission()
        }

        setComponent()
        allLabels = Utils().generateLabelsFile(assets, "labels.txt")!!
        var modelPath = Utils().getFileFromAssets(this, "model_transfer.tflite").absolutePath
        tfLiteClassificationUtil = TFLiteHandler().TFLiteHandler(modelPath)


    }


    private fun setComponent(){
        customTextureView = findViewById(R.id.texture_view)
        tvResult = findViewById(R.id.result_text)
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ), 1
            )
        }
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

        } else {
            true
        }
    }

}