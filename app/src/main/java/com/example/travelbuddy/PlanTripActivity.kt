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
import com.example.travelbuddy.helpers.setupBackButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PlanTripActivity : BaseActivity() {
    private var selectedStartDate: Long? = null
    private var selectedEndDate: Long? = null
    
    private lateinit var countryEditText: MaterialAutoCompleteTextView
    private lateinit var cityEditText: MaterialAutoCompleteTextView
    private lateinit var cityInputLayout: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plan_trip)

        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(this, "Musisz być zalogowany", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        countryEditText = findViewById(R.id.countryEditText)
        cityEditText = findViewById(R.id.cityEditText)
        cityInputLayout = findViewById(R.id.cityInputLayout)
        val dateRangeEditText = findViewById<TextInputEditText>(R.id.dateRangeEditText)

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
        findViewById<Button>(R.id.createTripButton).setOnClickListener {
            val country = countryEditText.text?.toString()?.trim() ?: ""
            val city = cityEditText.text?.toString()?.trim() ?: ""
            
            val (isValid, errorMessage) = TripFormValidator.validateTripForm(
                country, city, selectedStartDate, selectedEndDate
            )
            
            if (isValid) {
                saveTrip(country, city, selectedStartDate!!, selectedEndDate!!)
            } else {
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }
    
    private fun setupCountryDropdown() {
        countryEditText.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, 
            CountryCitiesData.getCountriesWithCities()))
        countryEditText.threshold = 1
        DropdownHelper.setupDropdownListeners(countryEditText) { updateCitiesForCountry(countryEditText.text.toString()) }
    }
    
    private fun setupCityDropdown() {
        cityInputLayout.isEnabled = false
        cityEditText.isEnabled = false
        cityInputLayout.hint = "Najpierw wybierz kraj"
        DropdownHelper.setupDropdownListeners(cityEditText, checkEnabled = true)
    }
    
    private fun updateCitiesForCountry(country: String) {
        val cities = CountryCitiesData.getCitiesForCountry(country)
        cityInputLayout.isEnabled = true
        cityEditText.isEnabled = true
        cityEditText.setText("", false)
        cityInputLayout.hint = if (cities.isNotEmpty()) "Wybierz miasto" else "Wpisz miasto"
        
        if (cities.isNotEmpty()) {
            cityEditText.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities))
            cityEditText.threshold = 1
            cityEditText.postDelayed({
                cityEditText.requestFocus()
                cityEditText.showDropDown()
            }, 100)
        } else {
            cityEditText.setAdapter(null)
        }
    }

    private fun saveTrip(country: String, city: String, startDate: Long, endDate: Long) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance().collection("trips").add(hashMapOf(
            "destination" to DestinationParser.createDestination(city, country),
            "country" to country,
            "city" to city,
            "startDate" to DateHelper.formatDate(startDate),
            "endDate" to DateHelper.formatDate(endDate),
            "startDateTimestamp" to startDate,
            "endDateTimestamp" to endDate,
            "budget" to 0,
            "userId" to user.uid,
            "createdAt" to com.google.firebase.Timestamp.now()
        )).addOnSuccessListener {
            Toast.makeText(this, "Podróż zapisana!", Toast.LENGTH_LONG).show()
            cityEditText.postDelayed({ finish() }, 1500)
        }.addOnFailureListener {
            Toast.makeText(this, "Błąd: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}
