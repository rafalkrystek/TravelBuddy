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

class PlanTripActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
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

        if (auth.currentUser == null) {
            Toast.makeText(this, "Musisz być zalogowany", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        countryEditText = findViewById(R.id.countryEditText)
        cityEditText = findViewById(R.id.cityEditText)
        cityInputLayout = findViewById(R.id.cityInputLayout)
        val dateRangeEditText = findViewById<TextInputEditText>(R.id.dateRangeEditText)
        val budgetSlider = findViewById<Slider>(R.id.budgetSlider)
        val budgetValueTextView = findViewById<TextView>(R.id.budgetValueTextView)

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

        budgetValueTextView.text = "${budgetSlider.value.toInt()} zł"
        budgetSlider.addOnChangeListener { _, value, _ -> budgetValueTextView.text = "${value.toInt()} zł" }

        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }
        findViewById<Button>(R.id.createTripButton).setOnClickListener {
            selectedCountry = countryEditText.text.toString().trim()
            selectedCity = cityEditText.text.toString().trim()
            val budget = budgetSlider.value.toInt()
            
            when {
                selectedCountry.isEmpty() -> Toast.makeText(this, "Wybierz kraj", Toast.LENGTH_LONG).show()
                selectedCity.isEmpty() -> Toast.makeText(this, "Wybierz miasto", Toast.LENGTH_LONG).show()
                selectedStartDate == null || selectedEndDate == null -> Toast.makeText(this, "Wybierz daty", Toast.LENGTH_LONG).show()
                budget <= 0 -> Toast.makeText(this, "Wybierz budżet", Toast.LENGTH_LONG).show()
                else -> saveTrip(selectedCountry, selectedCity, selectedStartDate!!, selectedEndDate!!, budget)
            }
        }
    }
    
    private fun setupCountryDropdown() {
        // Używamy tylko krajów, które mają zdefiniowane miasta
        val countriesWithCities = CountryCitiesData.getCountriesWithCities()
        val countriesAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, countriesWithCities)
        countryEditText.setAdapter(countriesAdapter)
        countryEditText.threshold = 1
        
        // Pokazuj listę po kliknięciu
        countryEditText.setOnClickListener {
            countryEditText.showDropDown()
        }
        
        // Pokazuj listę po fokusie
        countryEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                countryEditText.showDropDown()
            }
        }
        
        // Gdy użytkownik wybierze kraj, zaktualizuj listę miast
        countryEditText.setOnItemClickListener { _, _, _, _ ->
            val country = countryEditText.text.toString()
            selectedCountry = country
            updateCitiesForCountry(country)
        }
    }
    
    private fun setupCityDropdown() {
        // Na początku pole miasta jest wyłączone
        cityInputLayout.isEnabled = false
        cityEditText.isEnabled = false
        cityInputLayout.hint = "Najpierw wybierz kraj"
        
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
            // Włącz pole miasta i zaktualizuj listę
            cityInputLayout.isEnabled = true
            cityEditText.isEnabled = true
            cityInputLayout.hint = "Wybierz miasto"
            
            // Wyczyść poprzednie miasto
            cityEditText.setText("", false)
            selectedCity = ""
            
            val citiesAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities)
            cityEditText.setAdapter(citiesAdapter)
            cityEditText.threshold = 1
            
            // Pokaż dropdown automatycznie
            cityEditText.postDelayed({
                cityEditText.requestFocus()
                cityEditText.showDropDown()
            }, 100)
        } else {
            // Brak miast dla tego kraju - pozwól na ręczne wpisanie
            cityInputLayout.isEnabled = true
            cityEditText.isEnabled = true
            cityInputLayout.hint = "Wpisz miasto"
            cityEditText.setText("", false)
            selectedCity = ""
            cityEditText.setAdapter(null)
        }
    }

    private fun saveTrip(country: String, city: String, startDate: Long, endDate: Long, budget: Int) {
        val user = auth.currentUser ?: return
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        
        // Tworzymy destination jako "Miasto, Kraj" dla kompatybilności
        // ale zapisujemy też osobno kraj i miasto
        val destination = "$city, $country"
        
        db.collection("trips").add(hashMapOf(
            "destination" to destination,
            "country" to country,
            "city" to city,
            "startDate" to dateFormatter.format(Date(startDate)),
            "endDate" to dateFormatter.format(Date(endDate)),
            "startDateTimestamp" to startDate,
            "endDateTimestamp" to endDate,
            "budget" to budget,
            "userId" to user.uid,
            "createdAt" to com.google.firebase.Timestamp.now()
        )).addOnSuccessListener {
            Toast.makeText(this, "Podróż zapisana!", Toast.LENGTH_LONG).show()
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ finish() }, 1500)
        }.addOnFailureListener {
            Toast.makeText(this, "Błąd: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}
