package com.example.travelbuddy

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class RestaurantAdapter(
    private val restaurants: List<RestaurantFinderActivity.Restaurant>,
    private val favoritePlaceIds: Set<String>,
    private val onItemClick: (RestaurantFinderActivity.Restaurant) -> Unit,
    private val onFavoriteClick: (RestaurantFinderActivity.Restaurant, Boolean) -> Unit
) : RecyclerView.Adapter<RestaurantAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.restaurantNameTextView)
        val addressTextView: TextView = view.findViewById(R.id.restaurantAddressTextView)
        val ratingTextView: TextView = view.findViewById(R.id.restaurantRatingTextView)
        val favoriteButton: ImageButton = view.findViewById(R.id.favoriteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_restaurant, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val restaurant = restaurants[position]
        holder.nameTextView.text = restaurant.name
        holder.addressTextView.text = restaurant.address
        holder.ratingTextView.text = "⭐ ${String.format(Locale.getDefault(), "%.1f", restaurant.rating)}"
        
        val isFavorite = favoritePlaceIds.contains(restaurant.placeId)
        if (isFavorite) {
            holder.favoriteButton.setImageResource(android.R.drawable.btn_star_big_on)
        } else {
            holder.favoriteButton.setImageResource(android.R.drawable.btn_star_big_off)
        }
        
        holder.itemView.setOnClickListener {
            onItemClick(restaurant)
        }
        
        holder.favoriteButton.setOnClickListener {
            val newFavoriteState = !isFavorite
            onFavoriteClick(restaurant, newFavoriteState)
        }
    }

    override fun getItemCount() = restaurants.size
}

