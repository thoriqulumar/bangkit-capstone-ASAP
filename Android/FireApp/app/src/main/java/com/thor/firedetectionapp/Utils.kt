package com.thor.firedetectionapp

import android.content.Context
import android.location.Geocoder
import java.io.IOException
import java.util.*

class Utils {

    var addresses:kotlin.collections.MutableList<android.location.Address?>? = null



    fun getSimpleAddress(latitude: Double, longitude: Double, context: Context) :  String{
        val geocoder = Geocoder(context, Locale.getDefault())

        try {
          addresses = geocoder.getFromLocation(latitude, longitude, 1)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val address: String = addresses!!.get(0)!!.getAddressLine(0)
        val state: String = addresses!!.get(0)!!.getAdminArea()
        val country: String = addresses!!.get(0)!!.getCountryName()
        val postalCode: String = addresses!!.get(0)!!.getPostalCode()


        val text = address.replace(state, "").replace("$postalCode,", "").replace(country, "")

        return text
    }
}