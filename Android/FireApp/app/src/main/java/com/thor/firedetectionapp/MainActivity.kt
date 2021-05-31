package com.thor.firedetectionapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import com.thor.firedetectionapp.model.DataResult

class MainActivity : AppCompatActivity() {

    var dataList = arrayListOf<DataResult>()
    lateinit var adapter:DataAdapter
    lateinit var recyclerView: RecyclerView
    lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        recyclerView = findViewById(R.id.recycler_data)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.hasFixedSize()

        databaseReference = FirebaseDatabase.getInstance().getReference("Data")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (p0 in dataSnapshot.children) {

                        val data = p0.getValue(DataResult::class.java)
                        Log.d("GET DATA", "Hasil: "+ data!!.date)
                        dataList.add(data!!)
                    }
                    Log.d("GET DATA", "onDataChange: " + dataList.size)
                    adapter = DataAdapter(dataList, applicationContext)
                    recyclerView.adapter = adapter

                }


            }

            override fun onCancelled(p0: DatabaseError) {
                TODO("Not yet implemented")
            }

        })



    }



}