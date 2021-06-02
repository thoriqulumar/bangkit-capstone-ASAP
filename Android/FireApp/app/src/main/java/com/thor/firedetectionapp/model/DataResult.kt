package com.thor.firedetectionapp.model

import androidx.versionedparcelable.ParcelField
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.util.*
import kotlin.collections.HashMap


data class DataResult (var class_name:String,
                       var date:String,
                       var imgUrl:String,
                       var latitude:String,
                       var longtitude:String,
                       var probability: Float?) {

    constructor() : this("", "", "","","",null) {

    }


}