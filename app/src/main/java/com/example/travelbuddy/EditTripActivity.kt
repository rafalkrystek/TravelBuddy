package com.example.travelbuddy

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class EditTripActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tripId: String
    private var selectedStartDate: Long? = null
    private var selectedEndDate: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_trip)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        tripId = intent.getStringExtra("trip_id") ?: ""

        if (tripId.isEmpty() || auth.currentUser == null) {
            Toast.makeText(this, "Błąd", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val destinationEditText = findViewById<MaterialAutoCompleteTextView>(R.id.destinationEditText)
        val dateRangeEditText = findViewById<TextInputEditText>(R.id.dateRangeEditText)
        val budgetSlider = findViewById<Slider>(R.id.budgetSlider)
        val budgetValueTextView = findViewById<TextView>(R.id.budgetValueTextView)
        val createTripButton = findViewById<Button>(R.id.createTripButton)

        val currentDestination = intent.getStringExtra("trip_destination") ?: ""
        val currentStartDate = intent.getStringExtra("trip_start_date") ?: ""
        val currentEndDate = intent.getStringExtra("trip_end_date") ?: ""
        val currentBudget = intent.getIntExtra("trip_budget", 0)

        destinationEditText.setText(currentDestination)
        dateRangeEditText.setText("$currentStartDate - $currentEndDate")
        budgetSlider.value = currentBudget.toFloat()
        budgetValueTextView.text = "$currentBudget zł"

        try {
            val df = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            df.parse(currentStartDate)?.let { selectedStartDate = it.time }
            df.parse(currentEndDate)?.let { selectedEndDate = it.time }
        } catch (e: Exception) {}

        ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, resources.getStringArray(R.array.countries))
            .also { destinationEditText.setAdapter(it) }
        destinationEditText.threshold = 1
        
        destinationEditText.setOnClickListener {
            destinationEditText.showDropDown()
        }
        
        destinationEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                destinationEditText.showDropDown()
            }
        }

        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        dateRangeEditText.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Wybierz daty")
                .setCalendarConstraints(CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.from(MaterialDatePicker.todayInUtcMilliseconds()))
                    .build())
                .build()
            picker.addOnPositiveButtonClickListener {
                selectedStartDate = it.first
                selectedEndDate = it.second
                dateRangeEditText.setText("${dateFormatter.format(Date(selectedStartDate!!))} - ${dateFormatter.format(Date(selectedEndDate!!))}")
            }
            picker.show(supportFragmentManager, "DATE_PICKER")
        }

        budgetSlider.addOnChangeListener { _, value, _ -> budgetValueTextView.text = "${value.toInt()} zł" }
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }

        createTripButton.text = "Zapisz zmiany"
        createTripButton.setOnClickListener {
            val destination = destinationEditText.text.toString().trim()
            val budget = budgetSlider.value.toInt()
            when {
                destination.isEmpty() -> Toast.makeText(this, "Wybierz miejsce", Toast.LENGTH_LONG).show()
                selectedStartDate == null || selectedEndDate == null -> Toast.makeText(this, "Wybierz daty", Toast.LENGTH_LONG).show()
                budget <= 0 -> Toast.makeText(this, "Wybierz budżet", Toast.LENGTH_LONG).show()
                else -> updateTrip(destination, selectedStartDate!!, selectedEndDate!!, budget)
            }
        }
    }

    private fun updateTrip(destination: String, startDate: Long, endDate: Long, budget: Int) {
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        db.collection("trips").document(tripId).update(hashMapOf(
            "destination" to destination,
            "startDate" to dateFormatter.format(Date(startDate)),
            "endDate" to dateFormatter.format(Date(endDate)),
            "startDateTimestamp" to startDate,
            "endDateTimestamp" to endDate,
            "budget" to budget,
            "updatedAt" to com.google.firebase.Timestamp.now()
        ) as Map<String, Any>).addOnSuccessListener {
            Toast.makeText(this, "Zaktualizowano", Toast.LENGTH_LONG).show()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ finish() }, 1500)
        }.addOnFailureListener {
            Toast.makeText(this, "Błąd: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}
