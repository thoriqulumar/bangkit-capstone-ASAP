package com.thor.firedetectionapp.model


import com.google.gson.annotations.SerializedName

data class DataItem(
    @SerializedName("class_name")
    val className: String,
    @SerializedName("date")
    val date: String,
    @SerializedName("imgUrl")
    val imgUrl: String,
    @SerializedName("latitude")
    val latitude: String,
    @SerializedName("longtitude")
    val longtitude: String,
    @SerializedName("probability")
    val probability: Double



)