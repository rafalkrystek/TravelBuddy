package com.example.travelbuddy

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.google.firebase.firestore.FirebaseFirestore

class TripDetailsActivity : BaseActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var tripId: String
    private var destination: String = ""
    private var startDate: String = ""
    private var endDate: String = ""
    private var budget: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_details)
        db = FirebaseFirestore.getInstance()
        tripId = intent.getStringExtra("trip_id") ?: ""
        destination = intent.getStringExtra("trip_destination") ?: ""
        startDate = intent.getStringExtra("trip_start_date") ?: ""
        endDate = intent.getStringExtra("trip_end_date") ?: ""
        budget = intent.getIntExtra("trip_budget", 0)

        if (tripId.isEmpty()) {
            Toast.makeText(this, "Błąd: Brak ID podróży", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadTripData()
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }
        findViewById<TextView>(R.id.destinationTextView).text = destination
        findViewById<TextView>(R.id.dateRangeTextView).text = "$startDate - $endDate"

        findViewById<CardView>(R.id.weatherCard).setOnClickListener {
            startActivity(Intent(this, WeatherActivity::class.java).apply {
                putExtra("trip_id", tripId)
                putExtra("trip_destination", destination)
                putExtra("trip_start_date", startDate)
                putExtra("trip_end_date", endDate)
            })
        }
        findViewById<CardView>(R.id.packingAssistantCard).setOnClickListener {
            startActivity(Intent(this, TripPackingAssistantActivity::class.java).apply {
                putExtra("trip_id", tripId)
                putExtra("trip_destination", destination)
                putExtra("trip_start_date", startDate)
                putExtra("trip_end_date", endDate)
            })
        }
        findViewById<CardView>(R.id.activityPlannerCard).setOnClickListener {
            startActivity(Intent(this, ActivityPlannerActivity::class.java).apply {
                putExtra("trip_id", tripId)
                putExtra("trip_destination", destination)
                putExtra("trip_start_date", startDate)
                putExtra("trip_end_date", endDate)
            })
        }
        findViewById<CardView>(R.id.budgetCalculatorCard).setOnClickListener {
            startActivity(Intent(this, TripBudgetCalculatorActivity::class.java).apply {
                putExtra("trip_id", tripId)
                putExtra("trip_destination", destination)
                putExtra("trip_budget", budget)
            })
        }
        findViewById<CardView>(R.id.restaurantFinderCard).setOnClickListener {
            startActivity(Intent(this, RestaurantFinderActivity::class.java))
        }
        findViewById<CardView>(R.id.galleryCard).setOnClickListener {
            startActivity(Intent(this, TripGalleryActivity::class.java).apply {
                putExtra("trip_destination", destination)
            })
        }
    }

    override fun onResume() {
        super.onResume()
        loadTripData()
    }

    private fun loadTripData() {
        db.collection("trips").document(tripId).get().addOnSuccessListener { doc ->
            budget = (doc.getLong("budget") ?: 0).toInt()
            destination = doc.getString("destination") ?: destination
            startDate = doc.getString("startDate") ?: startDate
            endDate = doc.getString("endDate") ?: endDate
            findViewById<TextView>(R.id.destinationTextView).text = destination
            findViewById<TextView>(R.id.dateRangeTextView).text = "$startDate - $endDate"
        }
    }
}
