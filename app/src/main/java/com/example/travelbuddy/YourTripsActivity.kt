package com.example.travelbuddy

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class YourTripsActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tripsRecyclerView: RecyclerView
    private lateinit var noTripsText: TextView
    private lateinit var tripAdapter: TripAdapter
    private val tripsList = mutableListOf<Trip>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_your_trips)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        noTripsText = findViewById(R.id.noTripsText)
        tripsRecyclerView = findViewById(R.id.tripsRecyclerView)

        tripAdapter = TripAdapter(
            tripsList,
            onDeleteClick = { showDeleteConfirmationDialog(it) },
            onEditClick = { startActivity(Intent(this, EditTripActivity::class.java).apply {
                putExtra("trip_id", it.id)
                putExtra("trip_destination", it.destination)
                putExtra("trip_start_date", it.startDate)
                putExtra("trip_end_date", it.endDate)
                putExtra("trip_budget", it.budget)
            })},
            onItemClick = { startActivity(Intent(this, TripDetailsActivity::class.java).apply {
                putExtra("trip_id", it.id)
                putExtra("trip_destination", it.destination)
                putExtra("trip_start_date", it.startDate)
                putExtra("trip_end_date", it.endDate)
                putExtra("trip_budget", it.budget)
            })}
        )
        tripsRecyclerView.layoutManager = LinearLayoutManager(this)
        tripsRecyclerView.adapter = tripAdapter

        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }
        findViewById<TextView>(R.id.addNewTripButton).setOnClickListener {
            startActivity(Intent(this, PlanTripActivity::class.java))
        }
        loadTrips()
    }

    override fun onResume() {
        super.onResume()
        loadTrips()
    }

    private fun loadTrips() {
        val user = auth.currentUser ?: run {
            noTripsText.visibility = TextView.VISIBLE
            noTripsText.text = "Musisz być zalogowany"
            tripsRecyclerView.visibility = android.view.View.GONE
            return
        }

        db.collection("trips")
            .whereEqualTo("userId", user.uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->
                tripsList.clear()
                docs.forEach { doc ->
                    val initialBudget = doc.getLong("budget")?.toInt() ?: 0
                    val remainingBudget = doc.getLong("remainingBudget")?.toInt()
                    val displayBudget = remainingBudget ?: initialBudget
                    
                    tripsList.add(Trip(
                        id = doc.id,
                        destination = doc.getString("destination") ?: "",
                        startDate = doc.getString("startDate") ?: "",
                        endDate = doc.getString("endDate") ?: "",
                        budget = initialBudget,
                        remainingBudget = remainingBudget,
                        userId = doc.getString("userId") ?: "",
                        createdAt = doc.getDate("createdAt") ?: java.util.Date(),
                        packingList = (doc.get("packingList") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        activities = (doc.get("activities") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    ))
                }
                tripAdapter.notifyDataSetChanged()
                noTripsText.visibility = if (tripsList.isEmpty()) TextView.VISIBLE else android.view.View.GONE
                tripsRecyclerView.visibility = if (tripsList.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
            }
            .addOnFailureListener {
                noTripsText.visibility = TextView.VISIBLE
                noTripsText.text = "Błąd: ${it.message}"
                tripsRecyclerView.visibility = android.view.View.GONE
            }
    }

    private fun showDeleteConfirmationDialog(trip: Trip) {
        AlertDialog.Builder(this)
            .setTitle("Usuń podróż")
            .setMessage("Usunąć podróż do ${trip.destination}?")
            .setPositiveButton("Usuń") { _, _ -> deleteTrip(trip) }
            .setNegativeButton("Anuluj", null)
            .show()
    }

    private fun deleteTrip(trip: Trip) {
        if (trip.id.isEmpty() || auth.currentUser?.uid != trip.userId) {
            Toast.makeText(this, "Brak uprawnień", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("trips").document(trip.id).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Usunięto", Toast.LENGTH_SHORT).show()
                loadTrips()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Błąd: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }
}
