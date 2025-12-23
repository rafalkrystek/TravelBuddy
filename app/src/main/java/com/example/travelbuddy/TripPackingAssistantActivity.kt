package com.example.travelbuddy

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.travelbuddy.helpers.DateHelper
import com.example.travelbuddy.helpers.DropdownHelper
import com.example.travelbuddy.helpers.getTripDocument
import com.example.travelbuddy.helpers.PackingListBuilder
import com.example.travelbuddy.helpers.setupBackButton
import com.example.travelbuddy.PackingListGenerator
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

class TripPackingAssistantActivity : BaseActivity() {
    private lateinit var tripId: String
    private lateinit var destination: String
    private var startDateTimestamp: Long = 0
    private var endDateTimestamp: Long = 0
    
    private lateinit var tabLayout: TabLayout
    private lateinit var assistantTabView: View
    private lateinit var myListTabView: View
    private lateinit var seasonSpinner: MaterialAutoCompleteTextView
    private lateinit var activitiesRecyclerView: RecyclerView
    private lateinit var genderRadioGroup: RadioGroup
    private lateinit var bagTypeRadioGroup: RadioGroup
    private lateinit var familyTripCheckBox: CheckBox
    private lateinit var childrenCountLayout: com.google.android.material.textfield.TextInputLayout
    private lateinit var childrenCountEditText: TextInputEditText
    private lateinit var generatePackingListButton: Button
    private lateinit var packingListRecyclerView: RecyclerView
    private lateinit var packingItemEditText: TextInputEditText
    private lateinit var addPackingItemButton: Button
    private lateinit var myListPackingListRecyclerView: RecyclerView
    private lateinit var myListPackingItemEditText: TextInputEditText
    private lateinit var myListAddPackingItemButton: Button
    private lateinit var packingAdapter: SimpleListAdapter
    private lateinit var myListPackingAdapter: SimpleListAdapter
    private lateinit var activitiesAdapter: ActivitiesCheckboxAdapter
    private val packingList = mutableListOf<String>()
    private val selectedActivities = mutableSetOf<String>()
    
    private val seasons = Constants.SEASONS
    private val activities = Constants.ACTIVITIES
    
    // Dane pogodowe
    private var tripCountry: String = ""
    private var tripCity: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_packing_assistant)


        tripId = intent.getStringExtra("trip_id") ?: ""
        destination = intent.getStringExtra("trip_destination") ?: ""
        val startDate = intent.getStringExtra("trip_start_date") ?: ""
        val endDate = intent.getStringExtra("trip_end_date") ?: ""

        if (tripId.isEmpty()) {
            Toast.makeText(this, "Błąd: Brak ID podróży", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val destinationTextView = findViewById<TextView>(R.id.destinationTextView)
        val dateRangeTextView = findViewById<TextView>(R.id.dateRangeTextView)
        tabLayout = findViewById(R.id.tabLayout)
        assistantTabView = findViewById(R.id.assistantTabView)
        myListTabView = findViewById(R.id.myListTabView)
        seasonSpinner = findViewById(R.id.seasonSpinner)
        activitiesRecyclerView = findViewById(R.id.activitiesRecyclerView)
        genderRadioGroup = findViewById(R.id.genderRadioGroup)
        bagTypeRadioGroup = findViewById(R.id.bagTypeRadioGroup)
        familyTripCheckBox = findViewById(R.id.familyTripCheckBox)
        childrenCountLayout = findViewById(R.id.childrenCountLayout)
        childrenCountEditText = findViewById(R.id.childrenCountEditText)
        generatePackingListButton = findViewById(R.id.generatePackingListButton)
        packingListRecyclerView = findViewById(R.id.packingListRecyclerView)
        packingItemEditText = findViewById(R.id.packingItemEditText)
        addPackingItemButton = findViewById(R.id.addPackingItemButton)
        myListPackingListRecyclerView = findViewById(R.id.myListPackingListRecyclerView)
        myListPackingItemEditText = findViewById(R.id.myListPackingItemEditText)
        myListAddPackingItemButton = findViewById(R.id.myListAddPackingItemButton)

        destinationTextView.text = destination
        dateRangeTextView.text = "$startDate - $endDate"
        
        DateHelper.parseDate(startDate)?.let { startDateTimestamp = it.time }
        DateHelper.parseDate(endDate)?.let { endDateTimestamp = it.time }
        
        setupBackButton()
        setupPackingAssistant()
        activitiesAdapter = ActivitiesCheckboxAdapter(
            activities = activities,
            selectedActivities = selectedActivities,
            maxSelections = 10,
            onSelectionChanged = { selected ->
                saveTripData()
            }
        )
        activitiesRecyclerView.layoutManager = LinearLayoutManager(this)
        activitiesRecyclerView.adapter = activitiesAdapter

        packingAdapter = SimpleListAdapter(packingList) { item ->
            packingList.remove(item)
            packingAdapter.notifyDataSetChanged()
            myListPackingAdapter.notifyDataSetChanged()
            saveTripData()
        }
        packingListRecyclerView.layoutManager = LinearLayoutManager(this)
        packingListRecyclerView.adapter = packingAdapter

        myListPackingAdapter = SimpleListAdapter(packingList) { item ->
            packingList.remove(item)
            packingAdapter.notifyDataSetChanged()
            myListPackingAdapter.notifyDataSetChanged()
            saveTripData()
        }
        myListPackingListRecyclerView.layoutManager = LinearLayoutManager(this)
        myListPackingListRecyclerView.adapter = myListPackingAdapter

        addPackingItemButton.setOnClickListener {
            val item = packingItemEditText.text?.toString()?.trim() ?: ""
            if (item.isNotEmpty()) {
                packingList.add(item)
                packingAdapter.notifyItemInserted(packingList.size - 1)
                myListPackingAdapter.notifyItemInserted(packingList.size - 1)
                packingItemEditText.text?.clear()
                saveTripData()
            }
        }

        myListAddPackingItemButton.setOnClickListener {
            val item = myListPackingItemEditText.text?.toString()?.trim() ?: ""
            if (item.isNotEmpty()) {
                packingList.add(item)
                packingAdapter.notifyItemInserted(packingList.size - 1)
                myListPackingAdapter.notifyItemInserted(packingList.size - 1)
                myListPackingItemEditText.text?.clear()
                saveTripData()
            }
        }

        familyTripCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                childrenCountLayout.visibility = android.view.View.VISIBLE
            } else {
                childrenCountLayout.visibility = android.view.View.GONE
                childrenCountEditText.text?.clear()
                saveTripData()
            }
        }

        bagTypeRadioGroup.setOnCheckedChangeListener { _, _ ->
            saveTripData()
        }

        generatePackingListButton.setOnClickListener {
            generatePackingList()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        assistantTabView.visibility = View.VISIBLE
                        myListTabView.visibility = View.GONE
                    }
                    1 -> {
                        assistantTabView.visibility = View.GONE
                        myListTabView.visibility = View.VISIBLE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        loadTripData()
    }

    private fun setupPackingAssistant() {
        detectSeason().takeIf { it.isNotEmpty() }?.let { seasonSpinner.setText(it, false) }
        seasonSpinner.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, seasons))
        seasonSpinner.threshold = 1
        DropdownHelper.setupDropdownListeners(seasonSpinner)
    }

    private fun detectSeason(): String {
        if (startDateTimestamp == 0L) return ""
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDateTimestamp
        val month = calendar.get(Calendar.MONTH) + 1
        return when (month) {
            in 3..5 -> "Wiosna"
            in 6..8 -> "Lato"
            in 9..11 -> "Jesień"
            else -> "Zima"
        }
    }

    private fun generatePackingList() {
        val gender = if (findViewById<RadioButton>(R.id.maleRadioButton).isChecked) "Mężczyzna" else "Kobieta"
        val isBackpack = findViewById<RadioButton>(R.id.backpackRadioButton).isChecked
        val isFamilyTrip = familyTripCheckBox.isChecked
        val childrenCount = if (isFamilyTrip) {
            childrenCountEditText.text?.toString()?.trim()?.toIntOrNull() ?: 0
        } else {
            0
        }
        
        if (selectedActivities.isEmpty()) {
            Toast.makeText(this, "Wybierz przynajmniej jedną aktywność", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isFamilyTrip && childrenCount <= 0) {
            Toast.makeText(this, "Podaj liczbę dzieci", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Pokaż komunikat ładowania
        generatePackingListButton.isEnabled = false
        generatePackingListButton.text = "Sprawdzam pogodę..."
        
        // Pobierz dane pogodowe przed wygenerowaniem listy
        fetchWeatherAndGenerateList(gender, isBackpack, isFamilyTrip, childrenCount)
    }
    
    /**
     * Pobiera prognozę pogody dla miasta docelowego, a następnie generuje listę pakowania
     * na podstawie rzeczywistej temperatury (nie tylko pory roku).
     */
    private fun fetchWeatherAndGenerateList(
        gender: String,
        isBackpack: Boolean,
        isFamilyTrip: Boolean,
        childrenCount: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiKey = BuildConfig.OPENWEATHER_API_KEY
                
                // Pobierz miasto z podróży
                val cityToCheck = if (tripCity.isNotEmpty()) tripCity else destination
                
                Log.d("TripPackingAssistant", "Checking weather for: $cityToCheck")
                
                // Pobierz prognozę pogody
                val url = URL("https://api.openweathermap.org/data/2.5/forecast?q=$cityToCheck&appid=$apiKey&units=metric&lang=pl")
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val responseCode = connection.responseCode
                
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val list = json.getJSONArray("list")
                    
                    // Oblicz średnią temperaturę z prognozy
                    var tempSum = 0.0
                    var tempCount = 0
                    var minTemp = Double.MAX_VALUE
                    var maxTemp = Double.MIN_VALUE
                    
                    for (i in 0 until minOf(list.length(), 40)) { // 5 dni prognozy
                        val item = list.getJSONObject(i)
                        val main = item.getJSONObject("main")
                        val temp = main.getDouble("temp")
                        tempSum += temp
                        tempCount++
                        if (temp < minTemp) minTemp = temp
                        if (temp > maxTemp) maxTemp = temp
                    }
                    
                    val avgTemp = if (tempCount > 0) tempSum / tempCount else 20.0
                    
                    Log.d("TripPackingAssistant", "Weather fetched: avg=$avgTemp°C, min=$minTemp°C, max=$maxTemp°C")
                    
                    withContext(Dispatchers.Main) {
                        // Określ klimat na podstawie rzeczywistej temperatury
                        val climateType = PackingListGenerator.determineClimateType(avgTemp, minTemp, maxTemp)
                        
                        // Zaktualizuj sezon w UI na podstawie rzeczywistej pogody
                        val weatherBasedSeason = getSeasonFromClimate(climateType)
                        seasonSpinner.setText(weatherBasedSeason, false)
                        
                        // Generuj listę z informacją o pogodzie
                        generatePackingListWithWeather(
                            climateType, avgTemp, minTemp, maxTemp,
                            gender, isBackpack, isFamilyTrip, childrenCount, cityToCheck
                        )
                    }
                } else {
                    Log.w("TripPackingAssistant", "Weather API error: $responseCode, using fallback")
                    withContext(Dispatchers.Main) {
                        // Fallback - użyj pory roku z kalendarza
                        val fallbackSeason = detectSeason()
                        generatePackingListFallback(fallbackSeason, gender, isBackpack, isFamilyTrip, childrenCount)
                    }
                }
            } catch (e: Exception) {
                Log.e("TripPackingAssistant", "Error fetching weather", e)
                withContext(Dispatchers.Main) {
                    // Fallback - użyj pory roku z kalendarza
                    val fallbackSeason = detectSeason()
                    generatePackingListFallback(fallbackSeason, gender, isBackpack, isFamilyTrip, childrenCount)
                }
            }
        }
    }
    
    private fun getSeasonFromClimate(climateType: String): String {
        return when (climateType) {
            "BARDZO_GORĄCO", "GORĄCO" -> "Lato"
            "CIEPŁO" -> "Wiosna"
            "CHŁODNO" -> "Jesień"
            "ZIMNO", "BARDZO_ZIMNO" -> "Zima"
            else -> "Lato"
        }
    }
    
    /**
     * Generuje listę pakowania na podstawie RZECZYWISTYCH danych pogodowych
     */
    private fun generatePackingListWithWeather(
        climateType: String,
        avgTemp: Double,
        minTemp: Double,
        maxTemp: Double,
        gender: String,
        isBackpack: Boolean,
        isFamilyTrip: Boolean,
        childrenCount: Int,
        cityName: String
    ) {
        val tripDays = if (startDateTimestamp > 0 && endDateTimestamp > 0) {
            DateHelper.calculateDays(startDateTimestamp, endDateTimestamp)
        } else {
            7
        }
        
        val packingDays = if (isBackpack) minOf(4, tripDays) else tripDays
        
        packingList.clear()
        
        packingList.add("🌡️ POGODA W $cityName: ${avgTemp.toInt()}°C (${minTemp.toInt()}°-${maxTemp.toInt()}°C)")
        packingList.add("📋 Typ klimatu: ${PackingListGenerator.getClimateDescription(climateType)}")
        packingList.add("---")
        
        if (isBackpack && tripDays > 4) {
            packingList.add("⚠️ UWAGA: Pakowanie na $packingDays dni - będziesz musiał prać ubrania podczas podróży")
        }
        
        PackingListBuilder.addBasicItems(packingList)
        PackingListBuilder.addUnderwear(packingList, if (isBackpack) packingDays + 1 else tripDays + 2)
        PackingListBuilder.addClothesForClimate(packingList, climateType, avgTemp, isBackpack, tripDays, packingDays, gender)
        PackingListBuilder.addActivityItems(packingList, selectedActivities)
        PackingListBuilder.addToiletries(packingList)
        
        if (gender == "Kobieta") {
            PackingListBuilder.addWomenItems(packingList, isBackpack, packingDays, tripDays)
        }
        
        if (isFamilyTrip && childrenCount > 0) {
            PackingListBuilder.addChildrenItems(packingList, childrenCount, isBackpack, tripDays, climateType)
        }
        
                if (climateType == "BARDZO_GORĄCO") {
                    packingList.add("💡 TIP: Przy ${avgTemp.toInt()}°C pij dużo wody i unikaj słońca w południe")
                }
                
        packingList.removeAll { it.isEmpty() || it.isBlank() }
        
        // Aktualizuj UI
        packingAdapter.notifyDataSetChanged()
        myListPackingAdapter.notifyDataSetChanged()
        saveTripData()
        
        generatePackingListButton.isEnabled = true
        generatePackingListButton.text = "Generuj listę"
        
        Toast.makeText(this, "Lista wygenerowana na podstawie pogody w $cityName!", Toast.LENGTH_LONG).show()
    }
    
    
    
    /**
     * Fallback - generuje listę gdy nie można pobrać danych pogodowych
     */
    private fun generatePackingListFallback(
        season: String,
        gender: String,
        isBackpack: Boolean,
        isFamilyTrip: Boolean,
        childrenCount: Int
    ) {
        generatePackingListButton.isEnabled = true
        generatePackingListButton.text = "Generuj listę"
        
        val tripDays = if (startDateTimestamp > 0 && endDateTimestamp > 0) {
            DateHelper.calculateDays(startDateTimestamp, endDateTimestamp)
        } else {
            7
        }
        
        val packingDays = if (isBackpack) minOf(4, tripDays) else tripDays
        
        packingList.clear()
        packingList.add("⚠️ Nie udało się sprawdzić pogody - lista na podstawie pory roku")
        
        if (isBackpack && tripDays > 4) {
            packingList.add("⚠️ UWAGA: Pakowanie na $packingDays dni - będziesz musiał prać ubrania podczas podróży")
        }
        
        val climateType = PackingListGenerator.mapSeasonToClimate(season)
        
        PackingListBuilder.addBasicItems(packingList)
        PackingListBuilder.addUnderwear(packingList, if (isBackpack) packingDays + 1 else tripDays + 2)
        PackingListBuilder.addClothesForClimate(packingList, climateType, 20.0, isBackpack, tripDays, packingDays, gender)
        PackingListBuilder.addActivityItems(packingList, selectedActivities)
        PackingListBuilder.addToiletries(packingList)
        
        if (gender == "Kobieta") {
            PackingListBuilder.addWomenItems(packingList, isBackpack, packingDays, tripDays)
        }
        
        if (isFamilyTrip && childrenCount > 0) {
            PackingListBuilder.addChildrenItems(packingList, childrenCount, isBackpack, tripDays, climateType)
        }
        
        packingList.removeAll { it.isEmpty() || it.isBlank() }
        
        packingAdapter.notifyDataSetChanged()
        myListPackingAdapter.notifyDataSetChanged()
        saveTripData()
        
        Toast.makeText(this, "Lista wygenerowana (bez danych pogodowych)", Toast.LENGTH_SHORT).show()
    }

    private fun saveTripData() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) return

        val packingData = hashMapOf(
            "packingList" to packingList,
            "packingSeason" to (seasonSpinner.text?.toString() ?: ""),
            "packingActivities" to selectedActivities.toList(),
            "packingGender" to (if (findViewById<RadioButton>(R.id.maleRadioButton).isChecked) "Mężczyzna" else "Kobieta"),
            "packingBagType" to (if (findViewById<RadioButton>(R.id.backpackRadioButton).isChecked) "Plecak" else "Walizka"),
            "packingFamilyTrip" to familyTripCheckBox.isChecked,
            "packingChildrenCount" to (if (familyTripCheckBox.isChecked) {
                childrenCountEditText.text?.toString()?.trim()?.toIntOrNull() ?: 0
            } else {
                0
            }),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        FirebaseFirestore.getInstance().getTripDocument(tripId).update(packingData as Map<String, Any>)
            .addOnSuccessListener {
                Log.d("TripPackingAssistantActivity", "Packing data saved successfully. List size: ${packingList.size}")
            }
            .addOnFailureListener { e ->
                Log.e("TripPackingAssistantActivity", "Error saving packing data", e)
                Toast.makeText(this, "Błąd zapisywania listy", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTripData() {
        FirebaseFirestore.getInstance().getTripDocument(tripId).get()
            .addOnSuccessListener { document ->
                // Pobierz dane o kraju i mieście
                tripCountry = document.getString("country") ?: ""
                tripCity = document.getString("city") ?: ""
                
                // Jeśli nie ma osobnych pól, spróbuj wyciągnąć z destination
                if (tripCountry.isEmpty() && destination.contains(",")) {
                    val parts = destination.split(",").map { it.trim() }
                    if (parts.size >= 2) {
                        tripCity = parts[0]
                        tripCountry = parts[1]
                    }
                }
                
                Log.d("TripPackingAssistant", "Loaded trip: country=$tripCountry, city=$tripCity")
                
                val savedPackingList = (document.get("packingList") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val savedSeason = document.getString("packingSeason") ?: ""
                val savedActivities = (document.get("packingActivities") as? List<*>)?.mapNotNull { it as? String }?.toSet() ?: emptySet()
                val savedGender = document.getString("packingGender") ?: "Mężczyzna"
                val savedBagType = document.getString("packingBagType") ?: "Walizka"
                val savedFamilyTrip = document.getBoolean("packingFamilyTrip") ?: false
                val savedChildrenCount = (document.get("packingChildrenCount") as? Long)?.toInt() ?: 0
                
                if (savedPackingList.isNotEmpty()) {
                    packingList.clear()
                    packingList.addAll(savedPackingList)
                    packingAdapter.notifyDataSetChanged()
                    myListPackingAdapter.notifyDataSetChanged()
                }
                
                if (savedSeason.isNotEmpty()) {
                    seasonSpinner.setText(savedSeason, false)
                }
                
                if (savedActivities.isNotEmpty()) {
                    selectedActivities.clear()
                    selectedActivities.addAll(savedActivities)
                    activitiesAdapter.notifyDataSetChanged()
                }
                
                if (savedGender == "Kobieta") {
                    findViewById<RadioButton>(R.id.femaleRadioButton).isChecked = true
                }
                
                if (savedBagType == "Plecak") {
                    findViewById<RadioButton>(R.id.backpackRadioButton).isChecked = true
                } else {
                    findViewById<RadioButton>(R.id.suitcaseRadioButton).isChecked = true
                }
                
                familyTripCheckBox.isChecked = savedFamilyTrip
                if (savedFamilyTrip) {
                    childrenCountLayout.visibility = android.view.View.VISIBLE
                    if (savedChildrenCount > 0) {
                        childrenCountEditText.setText(savedChildrenCount.toString())
                    }
                } else {
                    childrenCountLayout.visibility = android.view.View.GONE
                }
            }
            .addOnFailureListener { e ->
                Log.e("TripPackingAssistantActivity", "Error loading trip data", e)
            }
    }
}

