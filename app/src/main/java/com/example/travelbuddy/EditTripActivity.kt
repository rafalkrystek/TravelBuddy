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
import com.google.android.material.textfield.TextInputLayout
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
    
    private lateinit var countryEditText: MaterialAutoCompleteTextView
    private lateinit var cityEditText: MaterialAutoCompleteTextView
    private lateinit var cityInputLayout: TextInputLayout
    
    private var selectedCountry: String = ""
    private var selectedCity: String = ""

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

        countryEditText = findViewById(R.id.countryEditText)
        cityEditText = findViewById(R.id.cityEditText)
        cityInputLayout = findViewById(R.id.cityInputLayout)
        val dateRangeEditText = findViewById<TextInputEditText>(R.id.dateRangeEditText)
        val budgetSlider = findViewById<Slider>(R.id.budgetSlider)
        val budgetValueTextView = findViewById<TextView>(R.id.budgetValueTextView)
        val createTripButton = findViewById<Button>(R.id.createTripButton)

        val currentDestination = intent.getStringExtra("trip_destination") ?: ""
        val currentStartDate = intent.getStringExtra("trip_start_date") ?: ""
        val currentEndDate = intent.getStringExtra("trip_end_date") ?: ""
        val currentBudget = intent.getIntExtra("trip_budget", 0)

        // Parsuj destination - może być w formacie "Miasto, Kraj" lub sam kraj
        parseDestination(currentDestination)
        
        dateRangeEditText.setText("$currentStartDate - $currentEndDate")
        budgetSlider.value = currentBudget.toFloat()
        budgetValueTextView.text = "$currentBudget zł"

        try {
            val df = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            df.parse(currentStartDate)?.let { selectedStartDate = it.time }
            df.parse(currentEndDate)?.let { selectedEndDate = it.time }
        } catch (e: Exception) {}

        setupCountryDropdown()
        setupCityDropdown()

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
            selectedCountry = countryEditText.text.toString().trim()
            selectedCity = cityEditText.text.toString().trim()
            val budget = budgetSlider.value.toInt()
            
            when {
                selectedCountry.isEmpty() -> Toast.makeText(this, "Wybierz kraj", Toast.LENGTH_LONG).show()
                selectedCity.isEmpty() -> Toast.makeText(this, "Wybierz miasto", Toast.LENGTH_LONG).show()
                selectedStartDate == null || selectedEndDate == null -> Toast.makeText(this, "Wybierz daty", Toast.LENGTH_LONG).show()
                budget <= 0 -> Toast.makeText(this, "Wybierz budżet", Toast.LENGTH_LONG).show()
                else -> updateTrip(selectedCountry, selectedCity, selectedStartDate!!, selectedEndDate!!, budget)
            }
        }
    }
    
    private fun parseDestination(destination: String) {
        if (destination.contains(",")) {
            val parts = destination.split(",").map { it.trim() }
            if (parts.size >= 2) {
                selectedCity = parts[0]
                selectedCountry = parts[1]
            }
        } else {
            // Stary format - destination to sam kraj
            selectedCountry = destination
            selectedCity = ""
        }
    }
    
    private fun setupCountryDropdown() {
        val countriesWithCities = CountryCitiesData.getCountriesWithCities()
        val countriesAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, countriesWithCities)
        countryEditText.setAdapter(countriesAdapter)
        countryEditText.threshold = 1
        
        // Ustaw aktualny kraj
        if (selectedCountry.isNotEmpty()) {
            countryEditText.setText(selectedCountry, false)
            updateCitiesForCountry(selectedCountry)
        }
        
        countryEditText.setOnClickListener {
            countryEditText.showDropDown()
        }
        
        countryEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                countryEditText.showDropDown()
            }
        }
        
        countryEditText.setOnItemClickListener { _, _, _, _ ->
            val country = countryEditText.text.toString()
            selectedCountry = country
            updateCitiesForCountry(country)
        }
    }
    
    private fun setupCityDropdown() {
        cityEditText.setOnClickListener {
            if (cityEditText.isEnabled) {
                cityEditText.showDropDown()
            }
        }
        
        cityEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && cityEditText.isEnabled) {
                cityEditText.showDropDown()
            }
        }
        
        cityEditText.setOnItemClickListener { _, _, _, _ ->
            selectedCity = cityEditText.text.toString()
        }
    }
    
    private fun updateCitiesForCountry(country: String) {
        val cities = CountryCitiesData.getCitiesForCountry(country)
        
        if (cities.isNotEmpty()) {
            cityInputLayout.isEnabled = true
            cityEditText.isEnabled = true
            cityInputLayout.hint = "Wybierz miasto"
            
            val citiesAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities)
            cityEditText.setAdapter(citiesAdapter)
            cityEditText.threshold = 1
            
            // Ustaw aktualne miasto jeśli jest na liście
            if (selectedCity.isNotEmpty() && cities.contains(selectedCity)) {
                cityEditText.setText(selectedCity, false)
            } else {
                cityEditText.setText("", false)
                selectedCity = ""
            }
        } else {
            cityInputLayout.isEnabled = true
            cityEditText.isEnabled = true
            cityInputLayout.hint = "Wpisz miasto"
            cityEditText.setAdapter(null)
            
            if (selectedCity.isNotEmpty()) {
                cityEditText.setText(selectedCity, false)
            }
        }
    }

    private fun updateTrip(country: String, city: String, startDate: Long, endDate: Long, budget: Int) {
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val destination = "$city, $country"
        
        db.collection("trips").document(tripId).update(hashMapOf(
            "destination" to destination,
            "country" to country,
            "city" to city,
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
