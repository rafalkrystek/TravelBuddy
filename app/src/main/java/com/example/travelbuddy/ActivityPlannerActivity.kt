package com.example.travelbuddy

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import android.widget.Toast
import com.example.travelbuddy.helpers.DateHelper
import com.example.travelbuddy.helpers.getTripDocument
import com.example.travelbuddy.helpers.GeminiApiHelper
import com.example.travelbuddy.helpers.setupBackButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityPlannerActivity : BaseActivity() {
    
    private lateinit var tripId: String
    private lateinit var destination: String
    private var startDateTimestamp: Long = 0
    private var endDateTimestamp: Long = 0
    
    // Szczegółowe dane o lokalizacji podróży
    private var tripCountry: String = ""
    private var tripCity: String = ""
    private var tripBudget: Int = 0
    
    private lateinit var userPreferencesEditText: TextInputEditText
    private lateinit var generatePlanButton: Button
    private lateinit var planLoadingTextView: TextView
    private lateinit var planScrollView: NestedScrollView
    private lateinit var generatedPlanTextView: TextView
    
    // Nowe elementy UI do modyfikacji planu
    private lateinit var modifyPlanSection: View
    private lateinit var modifyPlanEditText: TextInputEditText
    private lateinit var modifyPlanButton: Button
    private lateinit var savePlanButton: Button
    private lateinit var deletePlanButton: Button
    
    // Sekcja "Mój plan" na górze
    private lateinit var myPlanCard: androidx.cardview.widget.CardView
    private lateinit var myPlanTextView: TextView
    private lateinit var planStatusTextView: TextView
    private lateinit var editSavedPlanButton: Button
    private lateinit var deleteSavedPlanButton: Button
    
    // Historia konwersacji z AI
    private var currentPlan: String = ""
    private var conversationHistory: MutableList<String> = mutableListOf()
    private var isPlanSaved: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_activity_planner)


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
        userPreferencesEditText = findViewById(R.id.userPreferencesEditText)
        generatePlanButton = findViewById(R.id.generatePlanButton)
        planLoadingTextView = findViewById(R.id.planLoadingTextView)
        planScrollView = findViewById(R.id.planScrollView)
        generatedPlanTextView = findViewById(R.id.generatedPlanTextView)

        destinationTextView.text = destination
        dateRangeTextView.text = "$startDate - $endDate"
        
        // Inicjalizacja nowych elementów UI
        modifyPlanSection = findViewById(R.id.modifyPlanSection)
        modifyPlanEditText = findViewById(R.id.modifyPlanEditText)
        modifyPlanButton = findViewById(R.id.modifyPlanButton)
        savePlanButton = findViewById(R.id.savePlanButton)
        deletePlanButton = findViewById(R.id.deletePlanButton)
        
        // Inicjalizacja sekcji "Mój plan"
        myPlanCard = findViewById(R.id.myPlanCard)
        myPlanTextView = findViewById(R.id.myPlanTextView)
        planStatusTextView = findViewById(R.id.planStatusTextView)
        editSavedPlanButton = findViewById(R.id.editSavedPlanButton)
        deleteSavedPlanButton = findViewById(R.id.deleteSavedPlanButton)
        
        DateHelper.parseDate(startDate)?.let { startDateTimestamp = it.time }
        DateHelper.parseDate(endDate)?.let { endDateTimestamp = it.time }
        
        setupBackButton()

        generatePlanButton.setOnClickListener {
            generateTravelPlan(isNewPlan = true)
        }
        
        // Przycisk modyfikacji planu
        modifyPlanButton.setOnClickListener {
            val modifyRequest = modifyPlanEditText.text?.toString()?.trim() ?: ""
            if (modifyRequest.isNotEmpty()) {
                modifyTravelPlan(modifyRequest)
            } else {
                Toast.makeText(this, "Wpisz, co chcesz zmienić w planie", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Przycisk zapisania planu
        savePlanButton.setOnClickListener {
            if (currentPlan.isNotEmpty()) {
                savePlanData(currentPlan)
                Toast.makeText(this, "Plan zapisany!", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Przycisk usunięcia planu i wygenerowania nowego
        deletePlanButton.setOnClickListener {
            showDeletePlanConfirmation()
        }
        
        // Przyciski w sekcji "Mój plan"
        editSavedPlanButton.setOnClickListener {
            // Przewiń do sekcji modyfikacji i skup na polu edycji
            modifyPlanSection.visibility = View.VISIBLE
            modifyPlanEditText.requestFocus()
            Toast.makeText(this, "Wpisz zmiany, które chcesz wprowadzić", Toast.LENGTH_SHORT).show()
        }
        
        deleteSavedPlanButton.setOnClickListener {
            showDeletePlanConfirmation()
        }

        loadTripData()
    }

    private fun showDeletePlanConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Usuń plan")
            .setMessage("Czy na pewno chcesz usunąć obecny plan i wygenerować nowy? Ta operacja jest nieodwracalna.")
            .setPositiveButton("Usuń i wygeneruj nowy") { _, _ ->
                deletePlanAndGenerateNew()
            }
            .setNegativeButton("Anuluj", null)
            .show()
    }
    
    private fun deletePlanAndGenerateNew() {
        // Wyczyść obecny plan
        currentPlan = ""
        isPlanSaved = false
        conversationHistory.clear()
        generatedPlanTextView.text = ""
        planScrollView.visibility = View.GONE
        modifyPlanSection.visibility = View.GONE
        modifyPlanEditText.setText("")
        
        // Ukryj sekcję "Mój plan"
        myPlanCard.visibility = View.GONE
        myPlanTextView.text = ""
        
        // Usuń plan z Firestore
        FirebaseFirestore.getInstance().getTripDocument(tripId).update(
                mapOf(
                    "generatedPlan" to "",
                    "conversationHistory" to emptyList<String>(),
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Plan usunięty. Możesz wygenerować nowy.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("ActivityPlannerActivity", "Error deleting plan", e)
            }
    }
    
    private fun modifyTravelPlan(modifyRequest: String) {
        if (currentPlan.isEmpty()) {
            Toast.makeText(this, "Najpierw wygeneruj plan", Toast.LENGTH_SHORT).show()
            return
        }
        
        planLoadingTextView.text = "Modyfikuję plan..."
        planLoadingTextView.visibility = View.VISIBLE
        modifyPlanButton.isEnabled = false
        
        CoroutineScope(Dispatchers.IO).launch {
            val apiKey = BuildConfig.GEMINI_API_KEY
            try {
                val prompt = buildSystemPrompt() + "\n\n=== KONTEKST MODYFIKACJI ===\n" +
                    "Użytkownik wygenerował już plan podróży i chce go zmodyfikować.\n\n" +
                    "OBECNY PLAN PODRÓŻY:\n---\n$currentPlan\n---\n\n" +
                    "PROŚBA UŻYTKOWNIKA O MODYFIKACJĘ:\n$modifyRequest\n\n" +
                    "INSTRUKCJE:\n1. Zmodyfikuj plan według prośby użytkownika\n" +
                    "2. Zachowaj resztę planu bez zmian, chyba że użytkownik prosi o więcej\n" +
                    "3. Odpowiedz pełnym, zaktualizowanym planem podróży\n" +
                    "4. Wszystkie propozycje muszą dotyczyć miejsca: ${getTripLocationString()}\n" +
                    "5. Odpowiedz w języku polskim\n"
                
                val generatedText = GeminiApiHelper.generateContent(apiKey, prompt)
                
                if (generatedText != null) {
                    conversationHistory.add("Użytkownik: $modifyRequest")
                    conversationHistory.add("AI: [Zaktualizowany plan]")
                    currentPlan = generatedText
                    isPlanSaved = true
                    
                    withContext(Dispatchers.Main) {
                        planLoadingTextView.visibility = View.GONE
                        modifyPlanButton.isEnabled = true
                        modifyPlanEditText.setText("")
                        myPlanCard.visibility = View.VISIBLE
                        myPlanTextView.text = generatedText
                        planStatusTextView.text = "✓ Zaktualizowano"
                        savePlanData(generatedText)
                        Toast.makeText(this@ActivityPlannerActivity, "Plan zmodyfikowany i zapisany!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    throw Exception("Nie udało się zmodyfikować planu")
                }
            } catch (e: Exception) {
                Log.e("ActivityPlannerActivity", "Error modifying plan", e)
                withContext(Dispatchers.Main) {
                    planLoadingTextView.visibility = View.GONE
                    modifyPlanButton.isEnabled = true
                    Toast.makeText(this@ActivityPlannerActivity, "Błąd modyfikacji: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun generateTravelPlan(isNewPlan: Boolean = false) {
        val userPreferences = userPreferencesEditText.text?.toString()?.trim() ?: ""
        
        // Jeśli już mamy plan i to nie jest nowy plan, pokaż sekcję modyfikacji
        if (currentPlan.isNotEmpty() && !isNewPlan) {
            modifyPlanSection.visibility = View.VISIBLE
            return
        }
        
        val daysDiff = if (startDateTimestamp > 0 && endDateTimestamp > 0) {
            DateHelper.calculateDays(startDateTimestamp, endDateTimestamp)
        } else {
            7
        }
        
        val startDateFormatted = if (startDateTimestamp > 0) DateHelper.formatDate(startDateTimestamp) else ""
        val endDateFormatted = if (endDateTimestamp > 0) DateHelper.formatDate(endDateTimestamp) else ""
        
        FirebaseFirestore.getInstance().getTripDocument(tripId).get()
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
                        
                        val prompt = buildPrompt(weatherCity, weatherInfo, startDateFormatted, endDateFormatted, daysDiff, userPreferences)
                        val generatedText = GeminiApiHelper.generateContent(apiKey, prompt)
                        
                        withContext(Dispatchers.Main) {
                            planLoadingTextView.visibility = View.GONE
                            generatePlanButton.isEnabled = true
                            
                            if (generatedText != null) {
                                currentPlan = generatedText
                            isPlanSaved = true
                            conversationHistory.clear()
                            conversationHistory.add("Wygenerowano nowy plan podróży")
                            
                            myPlanCard.visibility = View.VISIBLE
                                myPlanTextView.text = generatedText
                            planStatusTextView.text = "✓ Zapisano"
                            planScrollView.visibility = View.GONE
                            generatedPlanTextView.text = ""
                            modifyPlanSection.visibility = View.VISIBLE
                            
                                savePlanData(generatedText)
                            Toast.makeText(this@ActivityPlannerActivity, "Plan wygenerowany i zapisany!", Toast.LENGTH_SHORT).show()
                            } else {
                                planScrollView.visibility = View.VISIBLE
                                generatedPlanTextView.text = "Błąd: Nie udało się wygenerować planu. Sprawdź klucz API."
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ActivityPlannerActivity", "Error generating plan", e)
                        withContext(Dispatchers.Main) {
                            planLoadingTextView.visibility = View.GONE
                            planScrollView.visibility = View.VISIBLE
                            generatedPlanTextView.text = GeminiApiHelper.getErrorMessage(e, apiKey)
                            generatePlanButton.isEnabled = true
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ActivityPlannerActivity", "Error loading trip data", e)
            }
    }

    private fun buildPrompt(
        weatherCity: String,
        weatherInfo: String,
        startDateFormatted: String,
        endDateFormatted: String,
        daysDiff: Int,
        userPreferences: String
    ): String {
        var prompt = buildSystemPrompt()
        prompt += "\n\n=== SZCZEGÓŁY PODRÓŻY ===\n"
        prompt += "📍 LOKALIZACJA: ${getTripLocationString()}\n"
        if (tripCity.isNotEmpty()) prompt += "- Miasto: $tripCity\n"
        if (tripCountry.isNotEmpty()) prompt += "- Kraj: $tripCountry\n"
        if (weatherCity.isNotEmpty() && weatherCity != tripCity) prompt += "- Miasto pogodowe: $weatherCity\n"
        if (weatherInfo.isNotEmpty()) prompt += "- Aktualna prognoza pogody:\n$weatherInfo\n"
        prompt += "\n📅 DATY:\n"
        prompt += "- Data rozpoczęcia: $startDateFormatted\n"
        prompt += "- Data zakończenia: $endDateFormatted\n"
        prompt += "- Liczba dni: $daysDiff\n"
        if (tripBudget > 0) prompt += "\n💰 BUDŻET: $tripBudget zł\n"
        if (userPreferences.isNotEmpty()) prompt += "\n👤 PREFERENCJE UŻYTKOWNIKA:\n$userPreferences\n"
        prompt += "\n=== ZADANIE ===\n"
        prompt += "Stwórz szczegółowy plan podróży do ${getTripLocationString()} z podziałem na dni.\n"
        prompt += "Dla każdego dnia podaj:\n"
        prompt += "- Co zwiedzać (konkretne miejsca w ${tripCity.ifEmpty { destination }})\n"
        prompt += "- Gdzie jeść (lokalne restauracje i kuchnia ${tripCountry.ifEmpty { "lokalna" }})\n"
        prompt += "- Jakie aktywności wykonać\n"
        prompt += "- Praktyczne wskazówki dotyczące ${tripCity.ifEmpty { destination }}\n\n"
        prompt += "WAŻNE: Wszystkie propozycje MUSZĄ dotyczyć miejsca: ${getTripLocationString()}.\n"
        prompt += "Odpowiedz w języku polskim."
        return prompt
    }

    private fun savePlanData(planText: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) return

        val planData = hashMapOf(
            "generatedPlan" to planText,
            "userPreferences" to (userPreferencesEditText.text?.toString() ?: ""),
            "conversationHistory" to conversationHistory,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        FirebaseFirestore.getInstance().getTripDocument(tripId).update(planData as Map<String, Any>)
            .addOnSuccessListener {
                Log.d("ActivityPlannerActivity", "Plan data saved")
            }
            .addOnFailureListener { e ->
                Log.e("ActivityPlannerActivity", "Error saving plan data", e)
            }
    }

    private fun loadTripData() {
        FirebaseFirestore.getInstance().collection("trips")
            .document(tripId)
            .get()
            .addOnSuccessListener { document ->
                // Załaduj dane o lokalizacji podróży
                tripCountry = document.getString("country") ?: ""
                tripCity = document.getString("city") ?: ""
                tripBudget = (document.getLong("budget") ?: 0).toInt()
                
                // Jeśli nie ma osobnych pól, spróbuj wyciągnąć z destination
                if (tripCountry.isEmpty() && destination.contains(",")) {
                    val parts = destination.split(",").map { it.trim() }
                    if (parts.size >= 2) {
                        tripCity = parts[0]
                        tripCountry = parts[1]
                    }
                } else if (tripCity.isEmpty() && !destination.contains(",")) {
                    // Stary format - destination to sam kraj
                    tripCountry = destination
                }
                
                Log.d("ActivityPlannerActivity", "Loaded trip context: country=$tripCountry, city=$tripCity, budget=$tripBudget")
                
                // Zaktualizuj tytuł w UI z pełną lokalizacją
                val destinationTextView = findViewById<TextView>(R.id.destinationTextView)
                destinationTextView.text = getTripLocationString()
                
                val savedPlan = document.getString("generatedPlan") ?: ""
                val savedPreferences = document.getString("userPreferences") ?: ""
                
                // Załaduj historię konwersacji
                @Suppress("UNCHECKED_CAST")
                val savedHistory = document.get("conversationHistory") as? List<String> ?: emptyList()
                conversationHistory.clear()
                conversationHistory.addAll(savedHistory)
                
                if (savedPreferences.isNotEmpty()) {
                    userPreferencesEditText.setText(savedPreferences)
                }
                
                if (savedPlan.isNotEmpty()) {
                    currentPlan = savedPlan
                    isPlanSaved = true
                    
                    // Pokaż sekcję "Mój plan" z zapisanym planem
                    myPlanCard.visibility = View.VISIBLE
                    myPlanTextView.text = savedPlan
                    planStatusTextView.text = "✓ Zapisano"
                    
                    // Ukryj główny scroll z planem (plan jest w "Mój plan")
                    planScrollView.visibility = View.GONE
                    generatedPlanTextView.text = ""
                    planLoadingTextView.visibility = View.GONE
                    
                    // Pokaż sekcję modyfikacji jeśli mamy plan
                    modifyPlanSection.visibility = View.VISIBLE
                } else {
                    // Brak zapisanego planu - ukryj sekcję "Mój plan"
                    myPlanCard.visibility = View.GONE
                    isPlanSaved = false
                }
            }
            .addOnFailureListener { e ->
                Log.e("ActivityPlannerActivity", "Error loading trip data", e)
            }
    }
    
    /**
     * Buduje system prompt z pełnym kontekstem podróży.
     * Ten prompt jest automatycznie wstrzykiwany na początku każdej rozmowy z AI.
     */
    private fun buildSystemPrompt(): String {
        val sb = StringBuilder()
        
        sb.append("=== SYSTEM PROMPT - ASYSTENT PLANOWANIA PODRÓŻY ===\n\n")
        
        sb.append("Jesteś profesjonalnym asystentem planowania podróży. ")
        sb.append("Pomagasz użytkownikowi zaplanować podróż do konkretnego miejsca.\n\n")
        
        sb.append("🌍 KONTEKST PODRÓŻY (automatycznie wykryty):\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        
        if (tripCity.isNotEmpty()) {
            sb.append("📍 MIASTO DOCELOWE: $tripCity\n")
        }
        if (tripCountry.isNotEmpty()) {
            sb.append("🏳️ KRAJ: $tripCountry\n")
        }
        if (tripCity.isEmpty() && tripCountry.isEmpty() && destination.isNotEmpty()) {
            sb.append("📍 CEL PODRÓŻY: $destination\n")
        }
        
        if (startDateTimestamp > 0) {
            sb.append("📅 DATA ROZPOCZĘCIA: ${DateHelper.formatDate(startDateTimestamp)}\n")
        }
        if (endDateTimestamp > 0) {
            sb.append("📅 DATA ZAKOŃCZENIA: ${DateHelper.formatDate(endDateTimestamp)}\n")
        }
        if (startDateTimestamp > 0 && endDateTimestamp > 0) {
            sb.append("⏱️ DŁUGOŚĆ PODRÓŻY: ${DateHelper.calculateDays(startDateTimestamp, endDateTimestamp)} dni\n")
        }
        if (tripBudget > 0) {
            sb.append("💰 BUDŻET: $tripBudget zł\n")
        }
        
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")
        
        sb.append("📋 ZASADY:\n")
        sb.append("1. WSZYSTKIE propozycje i rekomendacje MUSZĄ dotyczyć miejsca: ${getTripLocationString()}\n")
        sb.append("2. Sugeruj KONKRETNE miejsca, restauracje i atrakcje z tego regionu\n")
        sb.append("3. Uwzględniaj lokalną kulturę, kuchnię i zwyczaje ${tripCountry.ifEmpty { "tego regionu" }}\n")
        sb.append("4. Dostosuj propozycje do pory roku i pogody\n")
        sb.append("5. Odpowiadaj ZAWSZE w języku polskim\n")
        sb.append("6. Bądź konkretny - podawaj nazwy miejsc, ulice, ceny orientacyjne\n\n")
        
        return sb.toString()
    }
    
    /**
     * Zwraca sformatowany string z lokalizacją podróży
     */
    private fun getTripLocationString(): String {
        return when {
            tripCity.isNotEmpty() && tripCountry.isNotEmpty() -> "$tripCity, $tripCountry"
            tripCity.isNotEmpty() -> tripCity
            tripCountry.isNotEmpty() -> tripCountry
            destination.isNotEmpty() -> destination
            else -> "nieznana lokalizacja"
        }
    }
}

