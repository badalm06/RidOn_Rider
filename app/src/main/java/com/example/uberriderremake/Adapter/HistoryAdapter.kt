package com.example.uberriderremake.Adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.uberriderremake.Model.History
import com.example.uberriderremake.R

class HistoryAdapter(private val historyList: List<History>) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTripDateTime: TextView = itemView.findViewById(R.id.tvTripDateTime)
        val tvStartAddress: TextView = itemView.findViewById(R.id.tvStartAddress)
        val tvEndAddress: TextView = itemView.findViewById(R.id.tvEndAddress)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvDistance: TextView = itemView.findViewById(R.id.tvDistance)
        val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val history = historyList[position]
        holder.tvTripDateTime.text = "Date & Time: ${history.tripStartTime}"
        holder.tvStartAddress.text = "Start Address: ${history.start_address}"
        holder.tvEndAddress.text = "End Address: ${history.end_address}"
        holder.tvDuration.text = "Trip Duration: ${history.duration}"
        holder.tvDistance.text = "Trip Distance: ${history.distanceText}"
        holder.tvPrice.text = "Fare: ₹${history.price}"
    }

    override fun getItemCount(): Int = historyList.size
}
