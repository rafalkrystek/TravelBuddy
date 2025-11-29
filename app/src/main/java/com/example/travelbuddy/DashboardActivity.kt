package com.example.travelbuddy

import android.content.Intent
import android.os.Bundle
import androidx.cardview.widget.CardView

class DashboardActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        findViewById<CardView>(R.id.planTripCard).setOnClickListener {
            startActivity(Intent(this, PlanTripActivity::class.java))
        }
        findViewById<CardView>(R.id.yourTripsCard).setOnClickListener {
            startActivity(Intent(this, YourTripsActivity::class.java))
        }
    }
}
