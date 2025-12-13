package com.example.travelbuddy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.travelbuddy.helpers.getTripDocument
import com.example.travelbuddy.helpers.putTripExtras
import com.example.travelbuddy.helpers.setupBackButton
import com.google.firebase.firestore.FirebaseFirestore

class TripDetailsActivity : BaseActivity() {
    private lateinit var tripId: String
    private var destination: String = ""
    private var startDate: String = ""
    private var endDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_details)
        tripId = intent.getStringExtra("trip_id") ?: ""
        destination = intent.getStringExtra("trip_destination") ?: ""
        startDate = intent.getStringExtra("trip_start_date") ?: ""
        endDate = intent.getStringExtra("trip_end_date") ?: ""

        if (tripId.isEmpty()) {
            Toast.makeText(this, "Błąd: Brak ID podróży", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadTripData()
        setupBackButton()
        findViewById<TextView>(R.id.destinationTextView).text = destination
        findViewById<TextView>(R.id.dateRangeTextView).text = "$startDate - $endDate"

        mapOf(
            R.id.weatherQuick to ::openWeather,
            R.id.weatherCard to ::openWeather,
            R.id.packingQuick to ::openPacking,
            R.id.packingAssistantCard to ::openPacking,
            R.id.plannerQuick to ::openPlanner,
            R.id.activityPlannerCard to ::openPlanner
        ).forEach { (id, action) -> findViewById<View>(id).setOnClickListener { action() } }
    }
    
    private fun openWeather() {
        startActivity(Intent(this, WeatherActivity::class.java).putTripExtras(tripId, destination, startDate, endDate))
    }
    
    private fun openPacking() {
        startActivity(Intent(this, TripPackingAssistantActivity::class.java).putTripExtras(tripId, destination, startDate, endDate))
    }
    
    private fun openPlanner() {
        startActivity(Intent(this, ActivityPlannerActivity::class.java).putTripExtras(tripId, destination, startDate, endDate))
    }

    override fun onResume() {
        super.onResume()
        loadTripData()
    }

    private fun loadTripData() {
        FirebaseFirestore.getInstance().getTripDocument(tripId).get().addOnSuccessListener { doc ->
            destination = doc.getString("destination") ?: destination
            startDate = doc.getString("startDate") ?: startDate
            endDate = doc.getString("endDate") ?: endDate
            findViewById<TextView>(R.id.destinationTextView).text = destination
            findViewById<TextView>(R.id.dateRangeTextView).text = "$startDate - $endDate"
        }
    }
}
