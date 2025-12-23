package com.example.travelbuddy

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.travelbuddy.helpers.DateHelper
import com.example.travelbuddy.helpers.DropdownHelper
import com.example.travelbuddy.helpers.getTripDocument
import com.example.travelbuddy.helpers.setupBackButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditTripActivity : BaseActivity() {
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
        tripId = intent.getStringExtra("trip_id") ?: ""

        if (tripId.isEmpty() || FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(this, "Błąd", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        countryEditText = findViewById(R.id.countryEditText)
        cityEditText = findViewById(R.id.cityEditText)
        cityInputLayout = findViewById(R.id.cityInputLayout)
        val dateRangeEditText = findViewById<TextInputEditText>(R.id.dateRangeEditText)
        val createTripButton = findViewById<Button>(R.id.createTripButton)

        val currentDestination = intent.getStringExtra("trip_destination") ?: ""
        val currentStartDate = intent.getStringExtra("trip_start_date") ?: ""
        val currentEndDate = intent.getStringExtra("trip_end_date") ?: ""

        val (city, country) = DestinationParser.parseDestination(currentDestination)
        selectedCity = city
        selectedCountry = country
        
        dateRangeEditText.setText("$currentStartDate - $currentEndDate")

        DateHelper.parseDate(currentStartDate)?.let { selectedStartDate = it.time }
        DateHelper.parseDate(currentEndDate)?.let { selectedEndDate = it.time }

        setupCountryDropdown()
        setupCityDropdown()

        dateRangeEditText.setOnClickListener {
            MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Wybierz daty")
                .setCalendarConstraints(CalendarConstraints.Builder()
                    .setValidator(DateValidatorPointForward.from(MaterialDatePicker.todayInUtcMilliseconds()))
                    .build())
                .build()
                .apply {
                    addOnPositiveButtonClickListener {
                        selectedStartDate = it.first
                        selectedEndDate = it.second
                        dateRangeEditText.setText("${DateHelper.formatDate(it.first)} - ${DateHelper.formatDate(it.second)}")
                    }
                    show(supportFragmentManager, "DATE_PICKER")
                }
        }

        setupBackButton()
        createTripButton.text = "Zapisz zmiany"
        createTripButton.setOnClickListener {
            val country = countryEditText.text?.toString()?.trim() ?: ""
            val city = cityEditText.text?.toString()?.trim() ?: ""
            
            val (isValid, errorMessage) = TripFormValidator.validateTripForm(
                country, city, selectedStartDate, selectedEndDate
            )
            
            if (isValid) {
                updateTrip(country, city, selectedStartDate!!, selectedEndDate!!)
            } else {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun setupCountryDropdown() {
        countryEditText.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
            CountryCitiesData.getCountriesWithCities()))
        countryEditText.threshold = 1
        
        if (selectedCountry.isNotEmpty()) {
            countryEditText.setText(selectedCountry, false)
            updateCitiesForCountry(selectedCountry)
        }
        
        DropdownHelper.setupDropdownListeners(countryEditText) {
            selectedCountry = countryEditText.text.toString()
            updateCitiesForCountry(selectedCountry)
        }
    }
    
    private fun setupCityDropdown() {
        DropdownHelper.setupDropdownListeners(cityEditText, checkEnabled = true) {
            selectedCity = cityEditText.text.toString()
        }
    }
    
    private fun updateCitiesForCountry(country: String) {
        val cities = CountryCitiesData.getCitiesForCountry(country)
            cityInputLayout.isEnabled = true
            cityEditText.isEnabled = true
        cityInputLayout.hint = if (cities.isNotEmpty()) "Wybierz miasto" else "Wpisz miasto"
            
        if (cities.isNotEmpty()) {
            cityEditText.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities))
            cityEditText.threshold = 1
            if (selectedCity.isNotEmpty() && cities.contains(selectedCity)) {
                cityEditText.setText(selectedCity, false)
            } else {
                cityEditText.setText("", false)
                selectedCity = ""
            }
        } else {
            cityEditText.setAdapter(null)
            if (selectedCity.isNotEmpty()) cityEditText.setText(selectedCity, false)
        }
    }

    private fun updateTrip(country: String, city: String, startDate: Long, endDate: Long) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Błąd: Nie jesteś zalogowany", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Sprawdź ownership przed aktualizacją (dodatkowa warstwa bezpieczeństwa)
        FirebaseFirestore.getInstance().getTripDocument(tripId).get()
            .addOnSuccessListener { document ->
                val tripUserId = document.getString("userId") ?: ""
                if (tripUserId != user.uid) {
                    Toast.makeText(this, "Brak uprawnień", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                
                // Wykonaj aktualizację tylko jeśli użytkownik jest właścicielem
                FirebaseFirestore.getInstance().getTripDocument(tripId).update(hashMapOf(
            "destination" to DestinationParser.createDestination(city, country),
            "country" to country,
            "city" to city,
            "startDate" to DateHelper.formatDate(startDate),
            "endDate" to DateHelper.formatDate(endDate),
            "startDateTimestamp" to startDate,
            "endDateTimestamp" to endDate,
                    "budget" to 0,
            "updatedAt" to com.google.firebase.Timestamp.now()
        ) as Map<String, Any>).addOnSuccessListener {
            Toast.makeText(this, "Zaktualizowano", Toast.LENGTH_LONG).show()
                    cityEditText.postDelayed({ finish() }, 1500)
        }.addOnFailureListener {
                    Toast.makeText(this, "Błąd: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener {
            Toast.makeText(this, "Błąd: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}
