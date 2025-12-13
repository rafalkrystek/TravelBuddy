package com.example.travelbuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TripAdapter(
    private val trips: List<Trip>,
    private val onDeleteClick: (Trip) -> Unit,
    private val onEditClick: (Trip) -> Unit,
    private val onItemClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val destinationTextView: TextView = itemView.findViewById(R.id.destinationTextView)
        val dateRangeTextView: TextView = itemView.findViewById(R.id.dateRangeTextView)
        val editButton: ImageButton = itemView.findViewById(R.id.editButton)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TripViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false))

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        trips[position].let { trip ->
            holder.destinationTextView.text = trip.destination
            holder.dateRangeTextView.text = "${trip.startDate} - ${trip.endDate}"
            holder.itemView.setOnClickListener { onItemClick(trip) }
            holder.editButton.setOnClickListener { onEditClick(trip) }
            holder.deleteButton.setOnClickListener { onDeleteClick(trip) }
        }
    }

    override fun getItemCount() = trips.size
}
