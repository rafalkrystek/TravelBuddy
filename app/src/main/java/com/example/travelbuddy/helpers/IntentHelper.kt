package com.example.travelbuddy.helpers

import android.content.Intent

fun Intent.putTripExtras(tripId: String, destination: String, startDate: String, endDate: String): Intent {
    putExtra("trip_id", tripId)
    putExtra("trip_destination", destination)
    putExtra("trip_start_date", startDate)
    putExtra("trip_end_date", endDate)
    return this
}

