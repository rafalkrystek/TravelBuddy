package com.example.travelbuddy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        
        val user = FirebaseAuth.getInstance().currentUser
        val subtitleView = findViewById<TextView>(R.id.userNameTextView)
        if (user != null) {
            val displayName = user.displayName
            if (!displayName.isNullOrEmpty()) {
                subtitleView.text = "Cześć, $displayName!"
            }
        }
        
        findViewById<LinearLayout>(R.id.planTripQuick).setOnClickListener {
            startActivity(Intent(this, PlanTripActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.yourTripsQuick).setOnClickListener {
            startActivity(Intent(this, YourTripsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.settingsQuick).setOnClickListener {
            startActivity(Intent(this, YourTripsActivity::class.java))
        }
        findViewById<CardView>(R.id.planTripCard).setOnClickListener {
            startActivity(Intent(this, PlanTripActivity::class.java))
        }
        findViewById<CardView>(R.id.yourTripsCard).setOnClickListener {
            startActivity(Intent(this, YourTripsActivity::class.java))
        }
        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}
