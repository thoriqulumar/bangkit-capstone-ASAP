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
import com.thor.firedetectionapp.model.DataItem
import com.thor.firedetectionapp.model.DataResult
import com.thor.firedetectionapp.service.ApiClient
import com.thor.firedetectionapp.service.GetData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    var dataList = arrayListOf<DataItem>()
    var dataList2 = arrayListOf<DataResult>()
    lateinit var adapter:DataAdapter
    lateinit var adapter2:DataAdapter2
    lateinit var recyclerView: RecyclerView
    lateinit var databaseReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        recyclerView = findViewById(R.id.recycler_data)
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = true
        recyclerView.layoutManager = layoutManager
        recyclerView.hasFixedSize()


        getDataRetrofit()
        /*databaseReference = FirebaseDatabase.getInstance().getReference("Data")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (p0 in dataSnapshot.children) {
                        val data = p0.getValue(DataResult::class.java)
                        Log.d("GET DATA", "Hasil: "+ data!!.date)
                        dataList2.add(data!!)
                    }
                    Log.d("GET DATA", "onDataChange: " + dataList2.size)
                    adapter2 = DataAdapter2(dataList2, applicationContext)
                    recyclerView.adapter = adapter2

                }


            }

            override fun onCancelled(p0: DatabaseError) {

            }

        })*/



    }

    private fun getDataRetrofit(){
        val service: GetData = ApiClient().getRetrofitInstance()!!.create(GetData::class.java)
        val call: Call<ArrayList<DataItem>> = service.data
        call.enqueue(object : Callback<ArrayList<DataItem>>{
            override fun onResponse(call: Call<ArrayList<DataItem>>, response: Response<ArrayList<DataItem>>) {
                dataList = response.body()!!
                adapter = DataAdapter(dataList, applicationContext)
                recyclerView.adapter = adapter
            }

            override fun onFailure(call: Call<ArrayList<DataItem>>, t: Throwable) {

            }
        })

    }



}