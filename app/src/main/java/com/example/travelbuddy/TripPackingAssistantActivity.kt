package com.example.travelbuddy

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class TripPackingAssistantActivity : BaseActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_packing_assistant)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tripId = intent.getStringExtra("trip_id") ?: ""
        destination = intent.getStringExtra("trip_destination") ?: ""
        val startDate = intent.getStringExtra("trip_start_date") ?: ""
        val endDate = intent.getStringExtra("trip_end_date") ?: ""

        if (tripId.isEmpty()) {
            Toast.makeText(this, "Błąd: Brak ID podróży", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val backButton = findViewById<ImageButton>(R.id.backButton)
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
        
        try {
            val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val startDateParsed = dateFormatter.parse(startDate)
            val endDateParsed = dateFormatter.parse(endDate)
            if (startDateParsed != null && endDateParsed != null) {
                startDateTimestamp = startDateParsed.time
                endDateTimestamp = endDateParsed.time
            }
        } catch (e: Exception) {
            Log.e("TripPackingAssistantActivity", "Error parsing dates", e)
        }

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

        backButton.setOnClickListener {
            finish()
        }

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
        val season = detectSeason()
        if (season.isNotEmpty()) {
            seasonSpinner.setText(season, false)
        }
        
        val seasonAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, seasons)
        seasonSpinner.setAdapter(seasonAdapter)
        seasonSpinner.threshold = 1
        seasonSpinner.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) seasonSpinner.showDropDown()
        }
        seasonSpinner.setOnClickListener { seasonSpinner.showDropDown() }
        
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
        val season = seasonSpinner.text?.toString()?.trim() ?: ""
        val gender = if (findViewById<RadioButton>(R.id.maleRadioButton).isChecked) "Mężczyzna" else "Kobieta"
        val isBackpack = findViewById<RadioButton>(R.id.backpackRadioButton).isChecked
        val isFamilyTrip = familyTripCheckBox.isChecked
        val childrenCount = if (isFamilyTrip) {
            childrenCountEditText.text?.toString()?.trim()?.toIntOrNull() ?: 0
        } else {
            0
        }
        
        if (season.isEmpty() || selectedActivities.isEmpty()) {
            Toast.makeText(this, "Wypełnij wszystkie pola", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isFamilyTrip && childrenCount <= 0) {
            Toast.makeText(this, "Podaj liczbę dzieci", Toast.LENGTH_SHORT).show()
            return
        }

        val tripDays = if (startDateTimestamp > 0 && endDateTimestamp > 0) {
            ((endDateTimestamp - startDateTimestamp) / (1000 * 60 * 60 * 24)).toInt() + 1
        } else {
            7
        }
        
        val packingDays = if (isBackpack) {
            minOf(4, tripDays)
        } else {
            tripDays
        }
        
        packingList.clear()
        if (isBackpack && tripDays > 4) {
            packingList.add("⚠️ UWAGA: Pakowanie na $packingDays dni - będziesz musiał prać ubrania podczas podróży")
        }
        
        packingList.add("Paszport")
        packingList.add("Dowód osobisty")
        packingList.add("Bilety")
        packingList.add("Rezerwacje")
        packingList.add("Gotówka")
        packingList.add("Karty płatnicze")
        packingList.add("Telefon")
        packingList.add("Ładowarka do telefonu")
        packingList.add("Powerbank")
        packingList.add("Apteczka pierwszej pomocy")
        packingList.add("Leki osobiste")
        val underwearCount = if (isBackpack) {
            packingDays + 1 
        } else {
            tripDays + 2 
        }
        packingList.add("Majtki x$underwearCount")
        packingList.add("Skarpetki x$underwearCount")
        
        when (season) {
            "Lato" -> {
                val tshirtCount = if (isBackpack) packingDays else tripDays
                val shortsCount = if (isBackpack) {
                    minOf(3, packingDays)
                } else {
                    (tripDays / 2 + 1).coerceAtLeast(2)
                }
                packingList.add("Koszulki x$tshirtCount")
                packingList.add("Krótkie spodenki x$shortsCount")
                if (!isBackpack) {
                    packingList.add("Spodnie długie x1")
                }
                packingList.add("Buty sportowe x1")
                packingList.add("Sandały/klapki x1")
                packingList.add("Okulary przeciwsłoneczne x1")
                packingList.add("Krem z filtrem SPF 50 x1")
                packingList.add("Kapelusz/słomkowy x1")
            }
            "Zima" -> {
                val tshirtCount = if (isBackpack) packingDays else tripDays
                val sweaterCount = if (isBackpack) {
                    minOf(2, packingDays)
                } else {
                    (tripDays / 3 + 1).coerceAtLeast(2)
                }
                val pantsCount = if (isBackpack) {
                    minOf(2, packingDays)
                } else {
                    (tripDays / 2 + 1).coerceAtLeast(2)
                }
                packingList.add("Koszulki x$tshirtCount")
                packingList.add("Swetry/bluzy x$sweaterCount")
                packingList.add("Spodnie długie x$pantsCount")
                packingList.add("Kurtka zimowa x1")
                packingList.add("Czapka x1")
                packingList.add("Rękawiczki x1")
                packingList.add("Szalik x1")
                packingList.add("Buty zimowe x1")
                if (!isBackpack) {
                    packingList.add("Buty na zmianę x1")
                }
            }
            "Wiosna", "Jesień" -> {
                val tshirtCount = if (isBackpack) packingDays else tripDays
                val longSleeveCount = if (isBackpack) {
                    minOf(2, packingDays)
                } else {
                    (tripDays / 3 + 1).coerceAtLeast(2)
                }
                val pantsCount = if (isBackpack) {
                    minOf(2, packingDays)
                } else {
                    (tripDays / 2 + 1).coerceAtLeast(2)
                }
                packingList.add("Koszulki x$tshirtCount")
                packingList.add("Koszule długi rękaw x$longSleeveCount")
                packingList.add("Spodnie długie x$pantsCount")
                if (!isBackpack) {
                    packingList.add("Spodnie krótkie x1")
                }
                packingList.add("Lekka kurtka x1")
                packingList.add("Sweter/bluza x1")
                packingList.add("Buty sportowe x1")
                if (!isBackpack) {
                    packingList.add("Buty na zmianę x1")
                }
                packingList.add("Parasol x1")
            }
        }
        
        if (selectedActivities.contains("Plażowanie") || selectedActivities.contains("Sporty wodne")) {
            packingList.add("Strój kąpielowy x1")
            packingList.add("Ręcznik plażowy x1")
            packingList.add("Okulary przeciwsłoneczne x1")
        }
        if (selectedActivities.contains("Góry i trekking")) {
            packingList.add("Buty trekkingowe x1")
            packingList.add("Plecak trekkingowy x1")
            packingList.add("Butelka na wodę x1")
        }
        if (selectedActivities.contains("Nurkowanie")) {
            packingList.add("Maska do nurkowania x1")
            packingList.add("Płetwy x1")
            packingList.add("Fajka do nurkowania x1")
        }
        
        packingList.add("Szczoteczka do zębów x1")
        packingList.add("Pasta do zębów x1")
        packingList.add("Dezodorant x1")
        packingList.add("Szampon x1")
        packingList.add("Żel pod prysznic x1")
        packingList.add("Ręcznik x1")
        
        if (gender == "Kobieta") {
            val braCount = if (isBackpack) {
                minOf(2, packingDays)
            } else {
                (tripDays / 2 + 1).coerceAtLeast(2)
            }
            packingList.add("Kosmetyczka x1")
            packingList.add("Odżywka do włosów x1")
            packingList.add("Krem do twarzy x1")
            packingList.add("Szminka x1")
            packingList.add("Staniki x$braCount")
        }
        
        if (isFamilyTrip && childrenCount > 0) {
            for (i in 1..childrenCount) {
                val childPackingDays = if (isBackpack) minOf(4, tripDays) else tripDays
                val childUnderwear = if (isBackpack) {
                    childPackingDays + 1
                } else {
                    tripDays + 2
                }
                val childTshirtCount = if (isBackpack) childPackingDays else tripDays
                packingList.add("Majtki dla dziecka $i x$childUnderwear")
                packingList.add("Skarpetki dla dziecka $i x$childUnderwear")
                packingList.add("Koszulki dla dziecka $i x$childTshirtCount")
                val childPantsCount = if (isBackpack) {
                    minOf(2, childPackingDays)
                } else {
                    (tripDays / 2 + 1).coerceAtLeast(2)
                }
                packingList.add("Spodnie dla dziecka $i x$childPantsCount")
                packingList.add("Buty dla dziecka $i x1")
                if (!isBackpack) {
                    packingList.add("Buty na zmianę dla dziecka $i x1")
                }
            }
            packingList.add("Zabawki dla dzieci")
            packingList.add("Pieluchy (jeśli potrzebne)")
            packingList.add("Chusteczki nawilżane x2")
            packingList.add("Krem z filtrem dla dzieci x1")
            packingList.add("Apteczka dla dzieci x1")
        }
        
        packingAdapter.notifyDataSetChanged()
        myListPackingAdapter.notifyDataSetChanged()
        saveTripData()
        Toast.makeText(this, "Lista wygenerowana!", Toast.LENGTH_SHORT).show()
    }

    private fun saveTripData() {
        val user = auth.currentUser
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

        db.collection("trips")
            .document(tripId)
            .update(packingData as Map<String, Any>)
            .addOnSuccessListener {
                Log.d("TripPackingAssistantActivity", "Packing data saved successfully. List size: ${packingList.size}")
            }
            .addOnFailureListener { e ->
                Log.e("TripPackingAssistantActivity", "Error saving packing data", e)
                Toast.makeText(this, "Błąd zapisywania listy", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadTripData() {
        db.collection("trips")
            .document(tripId)
            .get()
            .addOnSuccessListener { document ->
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

