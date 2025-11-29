package com.example.travelbuddy

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ActivityPlannerActivity : BaseActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tripId: String
    private lateinit var destination: String
    private var startDateTimestamp: Long = 0
    private var endDateTimestamp: Long = 0
    
    private lateinit var userPreferencesEditText: TextInputEditText
    private lateinit var generatePlanButton: Button
    private lateinit var planLoadingTextView: TextView
    private lateinit var planScrollView: NestedScrollView
    private lateinit var generatedPlanTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activity_planner)

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
        userPreferencesEditText = findViewById(R.id.userPreferencesEditText)
        generatePlanButton = findViewById(R.id.generatePlanButton)
        planLoadingTextView = findViewById(R.id.planLoadingTextView)
        planScrollView = findViewById(R.id.planScrollView)
        generatedPlanTextView = findViewById(R.id.generatedPlanTextView)

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
            Log.e("ActivityPlannerActivity", "Error parsing dates", e)
        }

        backButton.setOnClickListener {
            finish()
        }

        generatePlanButton.setOnClickListener {
            generateTravelPlan()
        }

        loadTripData()
    }

    private fun generateTravelPlan() {
        val userPreferences = userPreferencesEditText.text?.toString()?.trim() ?: ""
        
        val daysDiff = if (startDateTimestamp > 0 && endDateTimestamp > 0) {
            ((endDateTimestamp - startDateTimestamp) / (1000 * 60 * 60 * 24)).toInt() + 1
        } else {
            7
        }
        
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val startDateFormatted = if (startDateTimestamp > 0) {
            dateFormatter.format(Date(startDateTimestamp))
        } else {
            ""
        }
        val endDateFormatted = if (endDateTimestamp > 0) {
            dateFormatter.format(Date(endDateTimestamp))
        } else {
            ""
        }
        
        db.collection("trips")
            .document(tripId)
            .get()
            .addOnSuccessListener { document ->
                val weatherCity = document.getString("weatherCity") ?: ""
                val weatherInfo = document.getString("weatherInfo") ?: ""
                
                planLoadingTextView.visibility = View.VISIBLE
                                planScrollView.visibility = View.GONE
                generatePlanButton.isEnabled = false
                
                CoroutineScope(Dispatchers.IO).launch {
                    val apiKey = BuildConfig.GEMINI_API_KEY
                    try {
                        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY") {
                            withContext(Dispatchers.Main) {
                                planLoadingTextView.visibility = View.GONE
                                planScrollView.visibility = View.VISIBLE
                                generatedPlanTextView.text = "Błąd: Brak klucza API Gemini.\n\nDodaj GEMINI_API_KEY do pliku local.properties:\nGEMINI_API_KEY=twój_klucz\n\nKlucz możesz uzyskać w Google AI Studio:\nhttps://aistudio.google.com/app/apikey"
                                generatePlanButton.isEnabled = true
                            }
                            return@launch
                        }
                        
                        var prompt = "Jesteś asystentem planowania podróży. Stwórz szczegółowy plan podróży z podziałem na dni.\n\n"
                        prompt += "Informacje o podróży:\n"
                        prompt += "- Miasto docelowe: $destination\n"
                        if (weatherCity.isNotEmpty()) {
                            prompt += "- Miasto pogodowe: $weatherCity\n"
                        }
                        if (weatherInfo.isNotEmpty()) {
                            prompt += "- Pogoda: $weatherInfo\n"
                        }
                        prompt += "- Data rozpoczęcia: $startDateFormatted\n"
                        prompt += "- Data zakończenia: $endDateFormatted\n"
                        prompt += "- Liczba dni: $daysDiff\n"
                        if (userPreferences.isNotEmpty()) {
                            prompt += "- Preferencje użytkownika: $userPreferences\n"
                        }
                        prompt += "\nStwórz szczegółowy plan podróży z podziałem na dni. Dla każdego dnia podaj:\n"
                        prompt += "- Co zwiedzać\n"
                        prompt += "- Gdzie jeść\n"
                        prompt += "- Jakie aktywności wykonać\n"
                        prompt += "- Praktyczne wskazówki\n\n"
                        prompt += "Odpowiedz w języku polskim."
                        
                        val generativeModel = GenerativeModel(
                            modelName = "gemini-1.5-pro",
                            apiKey = apiKey,
                            generationConfig = generationConfig {
                                temperature = 0.7f
                                topK = 40
                                topP = 0.95f
                                maxOutputTokens = 2048
                            }
                        )
                        
                        val response = generativeModel.generateContent(prompt)
                        val generatedText = response.text ?: "Błąd: Nie udało się wygenerować planu"
                        
                        withContext(Dispatchers.Main) {
                            planLoadingTextView.visibility = View.GONE
                            planScrollView.visibility = View.VISIBLE
                            generatedPlanTextView.text = generatedText
                            generatePlanButton.isEnabled = true
                            savePlanData(generatedText)
                        }
                    } catch (e: Exception) {
                        Log.e("ActivityPlannerActivity", "Error generating plan", e)
                        Log.e("ActivityPlannerActivity", "Full error: ${e.stackTraceToString()}")
                        Log.d("ActivityPlannerActivity", "API Key used: ${apiKey.take(10)}...")
                        withContext(Dispatchers.Main) {
                            planLoadingTextView.visibility = View.GONE
                            planScrollView.visibility = View.VISIBLE
                            val errorDetails = when {
                                e.message?.contains("403") == true || e.message?.contains("PERMISSION_DENIED") == true -> 
                                    "Błąd 403: Brak uprawnień.\n\nSprawdź:\n1. Włącz 'Generative Language API' w Google Cloud Console\n2. Sprawdź ograniczenia klucza API\n3. Upewnij się, że billing jest włączony\n\nSzczegóły: ${e.message}"
                                e.message?.contains("401") == true || e.message?.contains("UNAUTHENTICATED") == true ->
                                    "Błąd 401: Nieprawidłowy klucz API.\n\nSprawdź:\n1. Czy klucz API jest poprawny\n2. Czy klucz ma uprawnienia do Generative Language API\n\nSzczegóły: ${e.message}"
                                else ->
                                    "Błąd podczas generowania planu: ${e.message}\n\nSprawdź:\n1. Czy Generative Language API jest włączone w Google Cloud Console\n2. Czy klucz API jest poprawny\n3. Czy masz dostęp do internetu\n4. Czy billing jest włączony"
                            }
                            generatedPlanTextView.text = errorDetails
                            generatePlanButton.isEnabled = true
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ActivityPlannerActivity", "Error loading trip data", e)
            }
    }

    private fun savePlanData(planText: String) {
        val user = auth.currentUser
        if (user == null) return

        val planData = hashMapOf(
            "generatedPlan" to planText,
            "userPreferences" to (userPreferencesEditText.text?.toString() ?: ""),
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("trips")
            .document(tripId)
            .update(planData as Map<String, Any>)
            .addOnSuccessListener {
                Log.d("ActivityPlannerActivity", "Plan data saved")
            }
            .addOnFailureListener { e ->
                Log.e("ActivityPlannerActivity", "Error saving plan data", e)
            }
    }

    private fun loadTripData() {
        db.collection("trips")
            .document(tripId)
            .get()
            .addOnSuccessListener { document ->
                val savedPlan = document.getString("generatedPlan") ?: ""
                val savedPreferences = document.getString("userPreferences") ?: ""
                
                if (savedPreferences.isNotEmpty()) {
                    userPreferencesEditText.setText(savedPreferences)
                }
                
                if (savedPlan.isNotEmpty()) {
                    generatedPlanTextView.text = savedPlan
                    planScrollView.visibility = View.VISIBLE
                    planLoadingTextView.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Log.e("ActivityPlannerActivity", "Error loading trip data", e)
            }
    }
}

