package com.thor.firedetectionapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class DetailDataActivity : AppCompatActivity() {

    lateinit var tvDate: TextView
    lateinit var tvLocation: TextView
    lateinit var tvLatitude: TextView
    lateinit var tvLongitude: TextView
    lateinit var btnMap: Button
    lateinit var img:ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_data)
        setComponent()
        val class_name = intent.getStringExtra("class_name")
        val location = intent.getStringExtra("location")
        val date = intent.getStringExtra("date")
        val latitude = intent.getStringExtra("latitude")
        val longitude = intent.getStringExtra("longitude")
        val imgUrl = intent.getStringExtra("imgUrl")

        tvDate.text = date
        tvLocation.text = location
        tvLatitude.text =latitude
        tvLongitude.text = longitude

        Glide.with(this)
                .load(imgUrl)
                .into(img)

        btnMap.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("latitude", latitude)
            intent.putExtra("longitude", longitude)
            startActivity(intent)
        }
    }


    private fun setComponent(){
        tvDate = findViewById(R.id.tvDate)
        tvLocation = findViewById(R.id.tvLocation)
        tvLatitude = findViewById(R.id.tvlatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        btnMap = findViewById(R.id.buttonMap)
        img = findViewById(R.id.imgDetect)
    }
}