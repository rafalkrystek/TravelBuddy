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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
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
        db.collection("trips")
            .document(tripId)
            .update(
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
                // Buduj prompt z pełnym kontekstem podróży
                var prompt = buildSystemPrompt()
                prompt += "\n\n=== KONTEKST MODYFIKACJI ===\n"
                prompt += "Użytkownik wygenerował już plan podróży i chce go zmodyfikować.\n\n"
                prompt += "OBECNY PLAN PODRÓŻY:\n"
                prompt += "---\n$currentPlan\n---\n\n"
                prompt += "PROŚBA UŻYTKOWNIKA O MODYFIKACJĘ:\n"
                prompt += "$modifyRequest\n\n"
                prompt += "INSTRUKCJE:\n"
                prompt += "1. Zmodyfikuj plan według prośby użytkownika\n"
                prompt += "2. Zachowaj resztę planu bez zmian, chyba że użytkownik prosi o więcej\n"
                prompt += "3. Odpowiedz pełnym, zaktualizowanym planem podróży\n"
                prompt += "4. Wszystkie propozycje muszą dotyczyć miejsca: ${getTripLocationString()}\n"
                prompt += "5. Odpowiedz w języku polskim\n"
                
                val generatedText = callGeminiAPI(apiKey, prompt)
                
                if (generatedText != null) {
                    // Dodaj do historii konwersacji
                    conversationHistory.add("Użytkownik: $modifyRequest")
                    conversationHistory.add("AI: [Zaktualizowany plan]")
                    currentPlan = generatedText
                    isPlanSaved = true
                    
                    withContext(Dispatchers.Main) {
                        planLoadingTextView.visibility = View.GONE
                        modifyPlanButton.isEnabled = true
                        modifyPlanEditText.setText("")
                        
                        // Aktualizuj sekcję "Mój plan"
                        myPlanCard.visibility = View.VISIBLE
                        myPlanTextView.text = generatedText
                        planStatusTextView.text = "✓ Zaktualizowano"
                        
                        // Auto-zapisz zmodyfikowany plan
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
                    
                    // DEBUG: Logowanie informacji o kluczu API
                    Log.d("ActivityPlannerActivity", "=== DEBUG GEMINI API ===")
                    Log.d("ActivityPlannerActivity", "API Key length: ${apiKey.length}")
                    Log.d("ActivityPlannerActivity", "API Key first 10 chars: ${apiKey.take(10)}")
                    Log.d("ActivityPlannerActivity", "API Key last 10 chars: ${apiKey.takeLast(10)}")
                    Log.d("ActivityPlannerActivity", "API Key is empty: ${apiKey.isEmpty()}")
                    Log.d("ActivityPlannerActivity", "API Key is default: ${apiKey == "YOUR_GEMINI_API_KEY"}")
                    Log.d("ActivityPlannerActivity", "BuildConfig.GEMINI_API_KEY value: ${BuildConfig.GEMINI_API_KEY}")
                    
                    try {
                        if (apiKey.isEmpty() || apiKey == "YOUR_GEMINI_API_KEY") {
                            Log.e("ActivityPlannerActivity", "API Key is missing or default!")
                            withContext(Dispatchers.Main) {
                                planLoadingTextView.visibility = View.GONE
                                planScrollView.visibility = View.VISIBLE
                                generatedPlanTextView.text = "Błąd: Brak klucza API Gemini.\n\nDodaj GEMINI_API_KEY do pliku local.properties:\nGEMINI_API_KEY=twój_klucz\n\nKlucz możesz uzyskać w Google AI Studio:\nhttps://aistudio.google.com/app/apikey"
                                generatePlanButton.isEnabled = true
                            }
                            return@launch
                        }
                        
                        // Buduj prompt z pełnym kontekstem podróży
                        var prompt = buildSystemPrompt()
                        prompt += "\n\n=== SZCZEGÓŁY PODRÓŻY ===\n"
                        prompt += "📍 LOKALIZACJA: ${getTripLocationString()}\n"
                        if (tripCity.isNotEmpty()) {
                            prompt += "- Miasto: $tripCity\n"
                        }
                        if (tripCountry.isNotEmpty()) {
                            prompt += "- Kraj: $tripCountry\n"
                        }
                        if (weatherCity.isNotEmpty() && weatherCity != tripCity) {
                            prompt += "- Miasto pogodowe: $weatherCity\n"
                        }
                        if (weatherInfo.isNotEmpty()) {
                            prompt += "- Aktualna prognoza pogody:\n$weatherInfo\n"
                        }
                        prompt += "\n📅 DATY:\n"
                        prompt += "- Data rozpoczęcia: $startDateFormatted\n"
                        prompt += "- Data zakończenia: $endDateFormatted\n"
                        prompt += "- Liczba dni: $daysDiff\n"
                        if (tripBudget > 0) {
                            prompt += "\n💰 BUDŻET: $tripBudget zł\n"
                        }
                        if (userPreferences.isNotEmpty()) {
                            prompt += "\n👤 PREFERENCJE UŻYTKOWNIKA:\n$userPreferences\n"
                        }
                        prompt += "\n=== ZADANIE ===\n"
                        prompt += "Stwórz szczegółowy plan podróży do ${getTripLocationString()} z podziałem na dni.\n"
                        prompt += "Dla każdego dnia podaj:\n"
                        prompt += "- Co zwiedzać (konkretne miejsca w ${tripCity.ifEmpty { destination }})\n"
                        prompt += "- Gdzie jeść (lokalne restauracje i kuchnia ${tripCountry.ifEmpty { "lokalna" }})\n"
                        prompt += "- Jakie aktywności wykonać\n"
                        prompt += "- Praktyczne wskazówki dotyczące ${tripCity.ifEmpty { destination }}\n\n"
                        prompt += "WAŻNE: Wszystkie propozycje MUSZĄ dotyczyć miejsca: ${getTripLocationString()}.\n"
                        prompt += "Odpowiedz w języku polskim."
                        
                        // DEBUG: Logowanie promptu i konfiguracji
                        Log.d("ActivityPlannerActivity", "Prompt length: ${prompt.length}")
                        Log.d("ActivityPlannerActivity", "Prompt preview (first 200 chars): ${prompt.take(200)}")
                        
                        // Użyj bezpośredniego REST API do Gemini (omija problem z biblioteką i API v1)
                        Log.d("ActivityPlannerActivity", "Using direct REST API call to Gemini")
                        
                        // Najpierw spróbuj sprawdzić dostępne modele przez ListModels
                        var availableModels = mutableListOf<String>()
                        try {
                            Log.d("ActivityPlannerActivity", "Checking available models via ListModels API...")
                            val listUrl = URL("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
                            val listConnection = listUrl.openConnection() as HttpURLConnection
                            listConnection.requestMethod = "GET"
                            listConnection.connectTimeout = 10000
                            listConnection.readTimeout = 10000
                            
                            val listResponseCode = listConnection.responseCode
                            if (listResponseCode == 200) {
                                val listResponse = listConnection.inputStream.bufferedReader().use { it.readText() }
                                val listJson = JSONObject(listResponse)
                                if (listJson.has("models")) {
                                    val modelsArray = listJson.getJSONArray("models")
                                    for (i in 0 until modelsArray.length()) {
                                        val model = modelsArray.getJSONObject(i)
                                        val modelName = model.getString("name")
                                        // Usuń prefiks "models/"
                                        val shortName = modelName.replace("models/", "")
                                        availableModels.add(shortName)
                                        Log.d("ActivityPlannerActivity", "Found available model: $shortName")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.w("ActivityPlannerActivity", "Could not list models: ${e.message}")
                        }
                        
                        // Lista modeli do wypróbowania (używamy dostępnych modeli lub fallback)
                        // WAŻNE: Stare modele (gemini-pro, gemini-1.5-*) nie istnieją!
                        // Dostępne są tylko nowe modele Gemini 2.0 i 2.5
                        val modelsToTry = if (availableModels.isNotEmpty()) {
                            // Filtruj tylko modele obsługujące generateContent
                            val generativeModels = availableModels.filter { model ->
                                model.contains("gemini-2") || model.contains("gemini-flash") || model.contains("gemini-pro")
                            }
                            Log.d("ActivityPlannerActivity", "Using models from ListModels: $generativeModels")
                            if (generativeModels.isNotEmpty()) generativeModels else listOf("gemini-2.0-flash")
                        } else {
                            Log.d("ActivityPlannerActivity", "ListModels failed, using fallback models")
                            listOf(
                                "gemini-2.0-flash",      // Najszybszy dostępny model
                                "gemini-2.5-flash",      // Nowszy model
                                "gemini-flash-latest",   // Najnowszy Flash
                                "gemini-2.5-pro"         // Pro model
                            )
                        }
                        
                        var generatedText: String? = null
                        var lastError: Exception? = null
                        var successfulModel: String? = null
                        
                        // Próbuj każdy model aż jeden zadziała
                        for (modelName in modelsToTry) {
                            try {
                                Log.d("ActivityPlannerActivity", "=== Trying model: $modelName ===")
                                
                                // URL dla Gemini API - próbuj najpierw v1 (starsze API), potem v1beta
                                val apiVersions = listOf("v1", "v1beta")
                                var modelSuccess = false
                                
                                for (apiVersion in apiVersions) {
                                    try {
                                        Log.d("ActivityPlannerActivity", "Trying API version: $apiVersion for model: $modelName")
                                        
                                        val url = URL("https://generativelanguage.googleapis.com/$apiVersion/models/$modelName:generateContent?key=$apiKey")
                                        
                                        Log.d("ActivityPlannerActivity", "Calling Gemini API: $url")
                                        
                                        val connection = url.openConnection() as HttpURLConnection
                                        connection.requestMethod = "POST"
                                        connection.setRequestProperty("Content-Type", "application/json")
                                        connection.doOutput = true
                                        connection.connectTimeout = 30000
                                        connection.readTimeout = 30000
                                        
                                        // Przygotuj body request (poprawna struktura dla Gemini API)
                                        val requestBody = JSONObject().apply {
                                            put("contents", org.json.JSONArray().apply {
                                                put(JSONObject().apply {
                                                    put("parts", org.json.JSONArray().apply {
                                                        put(JSONObject().apply {
                                                            put("text", prompt)
                                                        })
                                                    })
                                                })
                                            })
                                            put("generationConfig", JSONObject().apply {
                                                put("temperature", 0.7)
                                                put("topK", 40)
                                                put("topP", 0.95)
                                                put("maxOutputTokens", 2048)
                                            })
                                        }
                                        
                                        // Wyślij request
                                        OutputStreamWriter(connection.outputStream).use { writer ->
                                            writer.write(requestBody.toString())
                                            writer.flush()
                                        }
                                        
                                        val responseCode = connection.responseCode
                                        Log.d("ActivityPlannerActivity", "Response code: $responseCode for $apiVersion/$modelName")
                                        
                                        if (responseCode == 200) {
                                            val response = connection.inputStream.bufferedReader().use { it.readText() }
                                            val jsonResponse = JSONObject(response)
                                            
                                            Log.d("ActivityPlannerActivity", "Response received successfully")
                                            
                                            // Parsuj odpowiedź
                                            val candidates = jsonResponse.getJSONArray("candidates")
                                            if (candidates.length() > 0) {
                                                val candidate = candidates.getJSONObject(0)
                                                val content = candidate.getJSONObject("content")
                                                val parts = content.getJSONArray("parts")
                                                if (parts.length() > 0) {
                                                    val part = parts.getJSONObject(0)
                                                    generatedText = part.getString("text")
                                                    successfulModel = "$modelName ($apiVersion)"
                                                    modelSuccess = true
                                                    
                                                    Log.d("ActivityPlannerActivity", "SUCCESS! Model $modelName with API $apiVersion worked!")
                                                    Log.d("ActivityPlannerActivity", "Response text length: ${generatedText?.length ?: 0}")
                                                    Log.d("ActivityPlannerActivity", "Generated text preview: ${generatedText?.take(200)}")
                                                    
                                                    break // Sukces - przerwij pętlę wersji API
                                                }
                                            }
                                            
                                            if (generatedText == null) {
                                                throw Exception("No text in response: $response")
                                            }
                                        } else {
                                            val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                                            Log.w("ActivityPlannerActivity", "API $apiVersion error $responseCode: $errorResponse")
                                            // Jeśli to nie ostatnia wersja API, spróbuj następnej
                                            if (apiVersion != apiVersions.last()) {
                                                continue
                                            }
                                            throw Exception("API error $responseCode: $errorResponse")
                                        }
                                    } catch (e: Exception) {
                                        Log.w("ActivityPlannerActivity", "API version $apiVersion failed: ${e.message}")
                                        // Jeśli to nie ostatnia wersja API, spróbuj następnej
                                        if (apiVersion == apiVersions.last()) {
                                            throw e
                                        }
                                        continue
                                    }
                                }
                                
                                if (modelSuccess) {
                                    break // Sukces - przerwij pętlę modeli
                                }
                            } catch (e: Exception) {
                                val errorMsg = "Model '$modelName' failed: ${e.message} (${e.javaClass.simpleName})"
                                Log.w("ActivityPlannerActivity", errorMsg)
                                Log.w("ActivityPlannerActivity", "Error type: ${e.javaClass.name}")
                                lastError = e
                                // Kontynuuj do następnego modelu
                            }
                        }
                        
                        if (generatedText == null || successfulModel == null) {
                            Log.e("ActivityPlannerActivity", "=== ALL MODELS FAILED ===")
                            throw lastError ?: Exception("Wszystkie modele nie powiodły się. Próbowano: ${modelsToTry.joinToString(", ")}")
                        }
                        
                        Log.d("ActivityPlannerActivity", "Successfully used model: $successfulModel")
                        
                        val finalText = generatedText ?: "Błąd: Nie udało się wygenerować planu"
                        
                        withContext(Dispatchers.Main) {
                            planLoadingTextView.visibility = View.GONE
                            generatePlanButton.isEnabled = true
                            
                            // Zapisz plan i pokaż sekcję modyfikacji
                            currentPlan = finalText
                            isPlanSaved = true
                            conversationHistory.clear()
                            conversationHistory.add("Wygenerowano nowy plan podróży")
                            
                            // Pokaż plan w sekcji "Mój plan"
                            myPlanCard.visibility = View.VISIBLE
                            myPlanTextView.text = finalText
                            planStatusTextView.text = "✓ Zapisano"
                            
                            // Ukryj główny scroll (plan jest w "Mój plan")
                            planScrollView.visibility = View.GONE
                            generatedPlanTextView.text = ""
                            
                            // Pokaż sekcję modyfikacji
                            modifyPlanSection.visibility = View.VISIBLE
                            
                            savePlanData(finalText)
                            Toast.makeText(this@ActivityPlannerActivity, "Plan wygenerowany i zapisany!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        // DEBUG: Szczegółowe logowanie błędu
                        Log.e("ActivityPlannerActivity", "=== ERROR DEBUG ===")
                        Log.e("ActivityPlannerActivity", "Error class: ${e.javaClass.name}")
                        Log.e("ActivityPlannerActivity", "Error message: ${e.message}")
                        Log.e("ActivityPlannerActivity", "Error cause: ${e.cause?.message}")
                        Log.e("ActivityPlannerActivity", "Full stack trace:")
                        e.printStackTrace()
                        Log.e("ActivityPlannerActivity", "API Key used (first 10): ${apiKey.take(10)}...")
                        Log.e("ActivityPlannerActivity", "API Key used (last 10): ...${apiKey.takeLast(10)}")
                        Log.e("ActivityPlannerActivity", "Model name used: gemini-1.5-pro")
                        
                        // Sprawdź czy błąd dotyczy modelu
                        val errorMessage = e.message ?: ""
                        if (errorMessage.contains("not found") || errorMessage.contains("not supported")) {
                            Log.e("ActivityPlannerActivity", "Model error detected - trying alternative models")
                            // Spróbuj alternatywnych nazw modeli
                            val alternativeModels = listOf("gemini-pro", "gemini-1.5-flash", "gemini-1.0-pro")
                            Log.d("ActivityPlannerActivity", "Available alternative models: $alternativeModels")
                        }
                        
                        withContext(Dispatchers.Main) {
                            planLoadingTextView.visibility = View.GONE
                            planScrollView.visibility = View.VISIBLE
                            val errorDetails = when {
                                e.message?.contains("403") == true || e.message?.contains("PERMISSION_DENIED") == true -> 
                                    "Błąd 403: Brak uprawnień.\n\nSprawdź:\n1. Włącz 'Generative Language API' w Google Cloud Console\n2. Sprawdź ograniczenia klucza API\n3. Upewnij się, że billing jest włączony\n\nSzczegóły: ${e.message}\n\nDEBUG: Klucz API (pierwsze 10 znaków): ${apiKey.take(10)}..."
                                e.message?.contains("401") == true || e.message?.contains("UNAUTHENTICATED") == true ->
                                    "Błąd 401: Nieprawidłowy klucz API.\n\nSprawdź:\n1. Czy klucz API jest poprawny\n2. Czy klucz ma uprawnienia do Generative Language API\n\nSzczegóły: ${e.message}\n\nDEBUG: Klucz API (pierwsze 10 znaków): ${apiKey.take(10)}..."
                                e.message?.contains("not found") == true || e.message?.contains("not supported") == true ->
                                    "Błąd: Żaden model nie jest dostępny.\n\nSzczegóły: ${e.message}\n\n⚠️ PROBLEM: Wszystkie żądania kończą się błędem (100% błędów w Google Cloud Console).\n\n🔍 DIAGNOZA:\n1. Otwórz Google Cloud Console:\n   https://console.cloud.google.com/\n\n2. Przejdź do: APIs & Services → Dashboard\n\n3. Kliknij na \"Generative Language API\" w tabeli\n\n4. Sprawdź zakładkę \"Errors\" - zobaczysz szczegóły błędów\n\n5. Sprawdź zakładkę \"Logs\" - zobaczysz dokładne komunikaty błędów\n\n🔧 MOŻLIWE ROZWIĄZANIA:\n\nA) Sprawdź szczegóły błędów:\n   - W Google Cloud Console zobaczysz dokładny komunikat błędu\n   - Może być: \"Model not found\", \"Permission denied\", \"Quota exceeded\"\n\nB) Sprawdź uprawnienia klucza API:\n   - APIs & Services → Credentials\n   - Otwórz swój klucz API\n   - W sekcji \"API restrictions\":\n     • Wybierz \"Don't restrict key\" LUB\n     • Jeśli \"Restrict key\", upewnij się że \"Generative Language API\" jest zaznaczone\n\nC) Sprawdź czy billing jest zsynchronizowany:\n   - Czasami po podpięciu billing trzeba odczekać 5-10 minut\n   - Sprawdź czy projekt ma przypisane konto rozliczeniowe\n\nD) Spróbuj wygenerować nowy klucz API:\n   - APIs & Services → Credentials → Create Credentials → API Key\n   - Upewnij się że ma dostęp do Generative Language API\n\nDEBUG:\n- Klucz API (pierwsze 10 znaków): ${apiKey.take(10)}...\n- Długość klucza: ${apiKey.length}\n- Próbowano kilku modeli - wszystkie niepowodzenie\n- Sprawdź Logcat i Google Cloud Console Logs dla szczegółów"
                                else ->
                                    "Błąd podczas generowania planu: ${e.message}\n\nSprawdź:\n1. Czy Generative Language API jest włączone w Google Cloud Console\n2. Czy klucz API jest poprawny\n3. Czy masz dostęp do internetu\n4. Czy billing jest włączony\n\nDEBUG:\n- Klucz API (pierwsze 10 znaków): ${apiKey.take(10)}...\n- Długość klucza: ${apiKey.length}\n- Model: gemini-1.5-pro\n- Błąd: ${e.javaClass.simpleName}"
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

    private suspend fun callGeminiAPI(apiKey: String, prompt: String): String? {
        // Najpierw pobierz listę dostępnych modeli
        var availableModels = mutableListOf<String>()
        try {
            val listUrl = URL("https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey")
            val listConnection = listUrl.openConnection() as HttpURLConnection
            listConnection.requestMethod = "GET"
            listConnection.connectTimeout = 10000
            listConnection.readTimeout = 10000
            
            val listResponseCode = listConnection.responseCode
            if (listResponseCode == 200) {
                val listResponse = listConnection.inputStream.bufferedReader().use { it.readText() }
                val listJson = JSONObject(listResponse)
                if (listJson.has("models")) {
                    val modelsArray = listJson.getJSONArray("models")
                    for (i in 0 until modelsArray.length()) {
                        val model = modelsArray.getJSONObject(i)
                        val modelName = model.getString("name").replace("models/", "")
                        availableModels.add(modelName)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ActivityPlannerActivity", "Could not list models: ${e.message}")
        }
        
        val modelsToTry = if (availableModels.isNotEmpty()) {
            availableModels.filter { model ->
                model.contains("gemini-2") || model.contains("gemini-flash") || model.contains("gemini-pro")
            }.ifEmpty { listOf("gemini-2.0-flash") }
        } else {
            listOf("gemini-2.0-flash", "gemini-2.5-flash", "gemini-flash-latest", "gemini-2.5-pro")
        }
        
        for (modelName in modelsToTry) {
            for (apiVersion in listOf("v1", "v1beta")) {
                try {
                    val url = URL("https://generativelanguage.googleapis.com/$apiVersion/models/$modelName:generateContent?key=$apiKey")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = 30000
                    connection.readTimeout = 30000
                    
                    val requestBody = JSONObject().apply {
                        put("contents", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("parts", org.json.JSONArray().apply {
                                    put(JSONObject().apply {
                                        put("text", prompt)
                                    })
                                })
                            })
                        })
                        put("generationConfig", JSONObject().apply {
                            put("temperature", 0.7)
                            put("topK", 40)
                            put("topP", 0.95)
                            put("maxOutputTokens", 4096)
                        })
                    }
                    
                    OutputStreamWriter(connection.outputStream).use { writer ->
                        writer.write(requestBody.toString())
                        writer.flush()
                    }
                    
                    if (connection.responseCode == 200) {
                        val response = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonResponse = JSONObject(response)
                        val candidates = jsonResponse.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            val content = candidates.getJSONObject(0).getJSONObject("content")
                            val parts = content.getJSONArray("parts")
                            if (parts.length() > 0) {
                                return parts.getJSONObject(0).getString("text")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("ActivityPlannerActivity", "Model $modelName ($apiVersion) failed: ${e.message}")
                }
            }
        }
        return null
    }

    private fun savePlanData(planText: String) {
        val user = auth.currentUser
        if (user == null) return

        val planData = hashMapOf(
            "generatedPlan" to planText,
            "userPreferences" to (userPreferencesEditText.text?.toString() ?: ""),
            "conversationHistory" to conversationHistory,
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
        
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        if (startDateTimestamp > 0) {
            sb.append("📅 DATA ROZPOCZĘCIA: ${dateFormatter.format(Date(startDateTimestamp))}\n")
        }
        if (endDateTimestamp > 0) {
            sb.append("📅 DATA ZAKOŃCZENIA: ${dateFormatter.format(Date(endDateTimestamp))}\n")
        }
        if (startDateTimestamp > 0 && endDateTimestamp > 0) {
            val days = ((endDateTimestamp - startDateTimestamp) / (1000 * 60 * 60 * 24)).toInt() + 1
            sb.append("⏱️ DŁUGOŚĆ PODRÓŻY: $days dni\n")
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

