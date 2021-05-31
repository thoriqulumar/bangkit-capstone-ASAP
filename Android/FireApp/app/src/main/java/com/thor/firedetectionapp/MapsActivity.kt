package com.thor.firedetectionapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class MapsActivity : AppCompatActivity() {

    private var supportMapFragment: SupportMapFragment? = null
    //private val LAT = -7.0652012
    //private val LONG =  110.4094606
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        supportMapFragment = supportFragmentManager.findFragmentById(R.id.mapFragmentUser) as SupportMapFragment?

        val latitude = intent.getStringExtra("latitude")
        val longitude = intent.getStringExtra("longitude")

        setMap(latitude.toDouble(), longitude.toDouble())
        //setMap(LAT, LONG)
    }

    private fun setMap(latitude: Double, longitude: Double){
        supportMapFragment!!.getMapAsync { p0 ->
            val latLng = LatLng(latitude, longitude)

            val options = MarkerOptions().position(latLng)

            p0!!.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
            p0.addMarker(options)
        }


    }
}