package com.thor.firedetectionapp.model

import androidx.versionedparcelable.ParcelField
import com.google.firebase.database.Exclude
import com.google.firebase.database.IgnoreExtraProperties
import java.util.*
import kotlin.collections.HashMap


data class DataResult (var class_name:String,
                  var probability: Float?,
                  var date:String,
                  var latitude:String,
                  var longitude:String,
                  var imgUrl:String) {

    constructor() : this("", null, "","","","") {

    }


}