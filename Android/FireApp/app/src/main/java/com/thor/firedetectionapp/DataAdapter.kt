package com.thor.firedetectionapp

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.thor.firedetectionapp.model.DataResult

@Suppress("UNREACHABLE_CODE")
class DataAdapter(private var dataList: ArrayList<DataResult>,
                  private var context: Context) : RecyclerView.Adapter<DataAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_list, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val latitude = dataList[position].latitude
        val longitude = dataList[position].longitude
        Log.d("Adapter", "latitude: " + latitude)
        Log.d("Adapter", "longitude: " + longitude)
        val location = Utils().getSimpleAddress(latitude.toDouble(), longitude.toDouble(), context)

        holder.date.text = dataList[position].date
        holder.location.text = location

        holder.parent.setOnClickListener {
            val intent = Intent(context, DetailDataActivity::class.java)
            intent.putExtra("class_name", dataList[position].class_name)
            intent.putExtra("location", location)
            intent.putExtra("date", dataList[position].date)
            intent.putExtra("latitude", dataList[position].latitude)
            intent.putExtra("longitude", dataList[position].longitude)
            intent.putExtra("imgUrl", dataList[position].imgUrl)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return  dataList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var parent = itemView.findViewById<CardView>(R.id.parent_item)
        var date = itemView.findViewById<TextView>(R.id.tv_date)
        var location = itemView.findViewById<TextView>(R.id.tv_location)
    }
}