package com.example.travelbuddy

import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection

class WeatherActivity : BaseActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tripId: String
    private lateinit var destination: String
    
    private lateinit var weatherCitySpinner: MaterialAutoCompleteTextView
    private lateinit var loadWeatherButton: Button
    private lateinit var weatherTextView: TextView
    private lateinit var weatherLoadingTextView: TextView
    
    private val citiesByCountry = mapOf(
        "Polska" to listOf("Warszawa", "Kraków", "Gdańsk", "Wrocław", "Poznań", "Łódź", "Katowice", "Lublin", "Białystok", "Szczecin"),
        "Niemcy" to listOf("Berlin", "Monachium", "Hamburg", "Frankfurt", "Kolonia", "Drezno", "Stuttgart", "Düsseldorf"),
        "Albania" to listOf("Tirana", "Durrës", "Vlorë", "Shkodër", "Korçë", "Elbasan", "Sarandë", "Berat", "Gjirokastër"),
        "Chorwacja" to listOf("Zagrzeb", "Split", "Dubrownik", "Rijeka", "Zadar", "Pula", "Osijek"),
        "Czarnogóra" to listOf("Podgorica", "Nikšić", "Pljevlja", "Bijelo Polje", "Cetinje", "Bar", "Kotor"),
        "Bośnia i Hercegowina" to listOf("Sarajewo", "Banja Luka", "Tuzla", "Mostar", "Zenica", "Bihać"),
        "Serbia" to listOf("Belgrad", "Nowy Sad", "Niš", "Kragujevac", "Subotica", "Zrenjanin"),
        "Słowenia" to listOf("Lublana", "Maribor", "Celje", "Kranj", "Velenje", "Koper"),
        "Macedonia" to listOf("Skopje", "Bitola", "Kumanovo", "Prilep", "Tetovo", "Ohrid"),
        "Kosowo" to listOf("Prisztina", "Prizren", "Peć", "Mitrovica", "Gjakova", "Ferizaj"),
        "Grecja" to listOf("Ateny", "Saloniki", "Patras", "Heraklion", "Larisa", "Wolos"),
        "Turcja" to listOf("Stambuł", "Ankara", "Izmir", "Bursa", "Antalya", "Adana"),
        "Włochy" to listOf("Rzym", "Mediolan", "Neapol", "Turyn", "Palermo", "Genua", "Bologna", "Florencja", "Wenecja"),
        "Hiszpania" to listOf("Madryt", "Barcelona", "Walencja", "Sewilla", "Bilbao", "Málaga", "Murcja"),
        "Francja" to listOf("Paryż", "Marsylia", "Lyon", "Tuluza", "Nicea", "Nantes", "Strasburg"),
        "Portugalia" to listOf("Lizbona", "Porto", "Braga", "Coimbra", "Faro", "Évora"),
        "Wielka Brytania" to listOf("Londyn", "Birmingham", "Manchester", "Glasgow", "Liverpool", "Leeds", "Edynburg"),
        "Irlandia" to listOf("Dublin", "Cork", "Limerick", "Galway", "Waterford", "Drogheda")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

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
        weatherCitySpinner = findViewById(R.id.weatherCitySpinner)
        loadWeatherButton = findViewById(R.id.loadWeatherButton)
        weatherTextView = findViewById(R.id.weatherTextView)
        weatherLoadingTextView = findViewById(R.id.weatherLoadingTextView)

        destinationTextView.text = destination
        dateRangeTextView.text = "$startDate - $endDate"

        setupWeather()
        backButton.setOnClickListener {
            finish()
        }

        loadWeatherButton.setOnClickListener { loadWeather() }
        loadWeatherData()
    }

    private fun setupWeather() {
        val cities = citiesByCountry[destination] ?: emptyList()
        if (cities.isEmpty()) {
            val allCities = citiesByCountry.values.flatten().sorted()
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, allCities)
            weatherCitySpinner.setAdapter(adapter)
        } else {
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cities)
            weatherCitySpinner.setAdapter(adapter)
        }
        
        weatherCitySpinner.threshold = 1
        weatherCitySpinner.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                weatherCitySpinner.showDropDown()
            }
        }
        weatherCitySpinner.setOnClickListener {
            weatherCitySpinner.showDropDown()
        }
    }

    private fun loadWeather() {
        val city = weatherCitySpinner.text?.toString()?.trim() ?: ""
        if (city.isEmpty()) {
            Toast.makeText(this, "Wybierz miasto", Toast.LENGTH_SHORT).show()
            return
        }

        weatherLoadingTextView.visibility = TextView.VISIBLE
        weatherTextView.visibility = TextView.GONE
        loadWeatherButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val apiKey = BuildConfig.OPENWEATHER_API_KEY
                Log.d("WeatherActivity", "Using API key: ${apiKey.take(8)}...")
                
                if (apiKey.isEmpty() || apiKey == "YOUR_OPENWEATHER_API_KEY") {
                    withContext(Dispatchers.Main) {
                        weatherTextView.text = "Błąd: Brak klucza API OpenWeatherMap.\n\nDodaj OPENWEATHER_API_KEY do pliku local.properties:\nOPENWEATHER_API_KEY=twój_klucz\n\nKlucz możesz uzyskać na:\nhttps://openweathermap.org/api"
                        weatherTextView.visibility = TextView.VISIBLE
                        weatherLoadingTextView.visibility = TextView.GONE
                        loadWeatherButton.isEnabled = true
                    }
                    return@launch
                }
                
                val url = URL("https://api.openweathermap.org/data/2.5/forecast?q=$city&appid=$apiKey&units=metric&lang=pl")
                Log.d("WeatherActivity", "Requesting weather forecast for: $city")
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                Log.d("WeatherActivity", "Response code: $responseCode")
                
                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val list = json.getJSONArray("list")
                    val dailyForecasts = mutableMapOf<String, MutableList<JSONObject>>()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val displayDateFormat = SimpleDateFormat("EEEE, dd.MM", Locale("pl", "PL"))
                    
                    for (i in 0 until list.length()) {
                        val item = list.getJSONObject(i)
                        val dt = item.getLong("dt") * 1000
                        val date = Date(dt)
                        val dayKey = dateFormat.format(date)
                        
                        if (!dailyForecasts.containsKey(dayKey)) {
                            dailyForecasts[dayKey] = mutableListOf()
                        }
                        dailyForecasts[dayKey]?.add(item)
                    }
                    
                    val forecastText = StringBuilder()
                    forecastText.append("Prognoza pogody na 5 dni:\n\n")
                    
                    var dayCount = 0
                    val sortedDays = dailyForecasts.keys.sorted()
                    for (dayKey in sortedDays) {
                        if (dayCount >= 5) break
                        
                        val dayForecasts = dailyForecasts[dayKey]!!
                        var minTemp = Double.MAX_VALUE
                        var maxTemp = Double.MIN_VALUE
                        var avgHumidity = 0
                        var mostCommonDescription = ""
                        val descriptionCounts = mutableMapOf<String, Int>()
                        
                        for (forecast in dayForecasts) {
                            val main = forecast.getJSONObject("main")
                            val tempMin = main.getDouble("temp_min")
                            val tempMax = main.getDouble("temp_max")
                            val humidity = main.getInt("humidity")
                            
                            if (tempMin < minTemp) minTemp = tempMin
                            if (tempMax > maxTemp) maxTemp = tempMax
                            avgHumidity += humidity
                            
                            val weather = forecast.getJSONArray("weather").getJSONObject(0)
                            val desc = weather.getString("description")
                            val currentCount = descriptionCounts.getOrDefault(desc, 0)
                            descriptionCounts[desc] = currentCount + 1
                        }
                        
                        avgHumidity /= dayForecasts.size
                        var maxCount = 0
                        for ((desc, count) in descriptionCounts) {
                            if (count > maxCount) {
                                maxCount = count
                                mostCommonDescription = desc
                            }
                        }
                        
                        val date = dateFormat.parse(dayKey)!!
                        val dayName = if (dayCount == 0) "Dzisiaj" else displayDateFormat.format(date)
                        val descriptionCapitalized = mostCommonDescription.replaceFirstChar { it.uppercaseChar() }
                        
                        forecastText.append("$dayName:\n")
                        forecastText.append("  Temp: ${minTemp.toInt()}°C / ${maxTemp.toInt()}°C\n")
                        forecastText.append("  $descriptionCapitalized\n")
                        forecastText.append("  Wilgotność: $avgHumidity%\n\n")
                        
                        dayCount++
                    }
                    
                    withContext(Dispatchers.Main) {
                        weatherTextView.text = forecastText.toString().trim()
                        weatherTextView.visibility = TextView.VISIBLE
                        weatherLoadingTextView.visibility = TextView.GONE
                        loadWeatherButton.isEnabled = true
                        saveWeatherData(city, forecastText.toString().trim())
                    }
                } else {
                    val errorMessage = when (responseCode) {
                        401 -> "Błąd autoryzacji (401). Sprawdź klucz API OpenWeatherMap w local.properties"
                        404 -> "Miasto nie zostało znalezione. Spróbuj innej nazwy."
                        429 -> "Zbyt wiele zapytań. Spróbuj ponownie później."
                        else -> "Błąd podczas pobierania pogody. Kod: $responseCode"
                    }
                    withContext(Dispatchers.Main) {
                        weatherTextView.text = errorMessage
                        weatherTextView.visibility = TextView.VISIBLE
                        weatherLoadingTextView.visibility = TextView.GONE
                        loadWeatherButton.isEnabled = true
                    }
                }
            } catch (e: Exception) {
                Log.e("WeatherActivity", "Error loading weather", e)
                withContext(Dispatchers.Main) {
                    weatherTextView.text = "Błąd podczas pobierania pogody: ${e.message}"
                    weatherTextView.visibility = TextView.VISIBLE
                    weatherLoadingTextView.visibility = TextView.GONE
                    loadWeatherButton.isEnabled = true
                }
            }
        }
    }

    private fun saveWeatherData(city: String, weatherText: String) {
        val user = auth.currentUser
        if (user == null) return

        val weatherData = hashMapOf(
            "weatherCity" to city,
            "weatherInfo" to weatherText,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("trips")
            .document(tripId)
            .update(weatherData as Map<String, Any>)
            .addOnSuccessListener {
                Log.d("WeatherActivity", "Weather data saved")
            }
            .addOnFailureListener { e ->
                Log.e("WeatherActivity", "Error saving weather data", e)
            }
    }

    private fun loadWeatherData() {
        db.collection("trips")
            .document(tripId)
            .get()
            .addOnSuccessListener { document ->
                val weatherCity = document.getString("weatherCity") ?: ""
                val weatherInfo = document.getString("weatherInfo") ?: ""
                
                if (weatherCity.isNotEmpty()) {
                    weatherCitySpinner.setText(weatherCity, false)
                }
                if (weatherInfo.isNotEmpty()) {
                    weatherTextView.text = weatherInfo
                    weatherTextView.visibility = TextView.VISIBLE
                    weatherLoadingTextView.visibility = TextView.GONE
                }
            }
            .addOnFailureListener { e ->
                Log.e("WeatherActivity", "Error loading weather data", e)
            }
    }
}

