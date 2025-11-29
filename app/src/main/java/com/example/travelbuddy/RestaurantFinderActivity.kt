package com.example.travelbuddy

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RestaurantFinderActivity : BaseActivity(), OnMapReadyCallback {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    
    private lateinit var map: GoogleMap
    private lateinit var favoritesMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private var currentLocation: Location? = null
    private val markers = mutableListOf<Marker>()
    private val favoriteMarkers = mutableListOf<Marker>()
    private val restaurants = mutableListOf<Restaurant>()
    private val favoriteRestaurants = mutableListOf<Restaurant>()
    private val favoritePlaceIds = mutableSetOf<String>()
    
    private lateinit var tabLayout: TabLayout
    private lateinit var searchTabView: View
    private lateinit var favoritesTabView: View
    private lateinit var cuisineTypeSpinner: MaterialAutoCompleteTextView
    private lateinit var radiusSpinner: MaterialAutoCompleteTextView
    private lateinit var searchRestaurantsButton: Button
    private lateinit var locationStatusTextView: TextView
    private lateinit var restaurantsRecyclerView: RecyclerView
    private lateinit var resultsCardView: CardView
    private lateinit var restaurantsAdapter: RestaurantAdapter
    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var favoritesCardView: CardView
    private lateinit var favoritesAdapter: RestaurantAdapter
    private lateinit var noFavoritesTextView: TextView
    
    private val cuisineTypes = listOf(
        "Włoska", "Francuska", "Hiszpańska", "Grecka", "Turecka",
        "Chińska", "Japońska", "Tajska", "Indyjska", "Meksykańska",
        "Amerykańska", "Polska", "Wegetariańska", "Wegańska", "Sushi",
        "Pizza", "Fast food", "Seafood", "Steakhouse", "Kawiarnia"
    )
    
    private val radiusOptions = listOf(
        "500 m" to 500,
        "1 km" to 1000,
        "2 km" to 2000,
        "5 km" to 5000,
        "10 km" to 10000,
        "25 km" to 25000
    )
    
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_finder)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val backButton = findViewById<ImageButton>(R.id.backButton)
        tabLayout = findViewById(R.id.tabLayout)
        searchTabView = findViewById(R.id.searchTabView)
        favoritesTabView = findViewById(R.id.favoritesTabView)
        cuisineTypeSpinner = findViewById(R.id.cuisineTypeSpinner)
        radiusSpinner = findViewById(R.id.radiusSpinner)
        searchRestaurantsButton = findViewById(R.id.searchRestaurantsButton)
        locationStatusTextView = findViewById(R.id.locationStatusTextView)
        restaurantsRecyclerView = findViewById(R.id.restaurantsRecyclerView)
        resultsCardView = findViewById<CardView>(R.id.resultsCardView)
        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView)
        favoritesCardView = findViewById(R.id.favoritesCardView)
        noFavoritesTextView = findViewById(R.id.noFavoritesTextView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
        if (apiKey.isEmpty() || apiKey == "YOUR_GOOGLE_MAPS_API_KEY") {
            Toast.makeText(this, "Błąd: Brak klucza Google Maps API", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }
        placesClient = Places.createClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val cuisineAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, cuisineTypes)
        cuisineTypeSpinner.setAdapter(cuisineAdapter)
        cuisineTypeSpinner.threshold = 1
        cuisineTypeSpinner.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) cuisineTypeSpinner.showDropDown()
        }
        cuisineTypeSpinner.setOnClickListener { cuisineTypeSpinner.showDropDown() }
        
        val radiusLabels = radiusOptions.map { it.first }
        val radiusAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, radiusLabels)
        radiusSpinner.setAdapter(radiusAdapter)
        radiusSpinner.threshold = 1
        radiusSpinner.setText("5 km", false)
        radiusSpinner.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) radiusSpinner.showDropDown()
        }
        radiusSpinner.setOnClickListener { radiusSpinner.showDropDown() }

        restaurantsAdapter = RestaurantAdapter(
            restaurants = restaurants,
            favoritePlaceIds = favoritePlaceIds,
            onItemClick = { restaurant ->
                val latLng = LatLng(restaurant.lat, restaurant.lng)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            },
            onFavoriteClick = { restaurant, isFavorite ->
                if (isFavorite) {
                    saveFavoriteRestaurant(restaurant)
                } else {
                    removeFavoriteRestaurant(restaurant.placeId)
                }
            }
        )
        restaurantsRecyclerView.layoutManager = LinearLayoutManager(this)
        restaurantsRecyclerView.adapter = restaurantsAdapter

        favoritesAdapter = RestaurantAdapter(
            restaurants = favoriteRestaurants,
            favoritePlaceIds = favoritePlaceIds,
            onItemClick = { restaurant ->
                val latLng = LatLng(restaurant.lat, restaurant.lng)
                if (::favoritesMap.isInitialized) {
                    favoritesMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                }
            },
            onFavoriteClick = { restaurant, isFavorite ->
                if (!isFavorite) {
                    removeFavoriteRestaurant(restaurant.placeId)
                }
            }
        )
        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)
        favoritesRecyclerView.adapter = favoritesAdapter

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        searchTabView.visibility = View.VISIBLE
                        favoritesTabView.visibility = View.GONE
                    }
                    1 -> {
                        searchTabView.visibility = View.GONE
                        favoritesTabView.visibility = View.VISIBLE
                        loadFavoriteRestaurants()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        val favoritesMapFragment = supportFragmentManager.findFragmentById(R.id.favoritesMapFragment) as SupportMapFragment
        favoritesMapFragment.getMapAsync { googleMap ->
            favoritesMap = googleMap
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                favoritesMap.isMyLocationEnabled = true
                favoritesMap.uiSettings.isMyLocationButtonEnabled = true
                favoritesMap.uiSettings.isZoomControlsEnabled = true
            }
        }

        backButton.setOnClickListener {
            finish()
        }

        searchRestaurantsButton.setOnClickListener {
            val cuisineType = cuisineTypeSpinner.text?.toString()?.trim()
            if (cuisineType.isNullOrEmpty()) {
                Toast.makeText(this, "Wybierz typ kuchni", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (currentLocation == null) {
                Toast.makeText(this, "Oczekiwanie na lokalizację...", Toast.LENGTH_SHORT).show()
                requestLocation()
                return@setOnClickListener
            }
            val radiusText = radiusSpinner.text?.toString()?.trim() ?: "5 km"
            val radius = radiusOptions.find { it.first == radiusText }?.second ?: 5000
            searchRestaurants(cuisineType, radius)
        }

        requestLocation()
        loadFavoritePlaceIds()
    }

    private fun loadFavoritePlaceIds() {
        val user = auth.currentUser
        if (user == null) return

        db.collection("users")
            .document(user.uid)
            .collection("favorite_restaurants")
            .get()
            .addOnSuccessListener { documents ->
                favoritePlaceIds.clear()
                for (document in documents) {
                    favoritePlaceIds.add(document.id)
                }
                restaurantsAdapter.notifyDataSetChanged()
            }
    }

    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            getCurrentLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Toast.makeText(this, "Brak uprawnień do lokalizacji", Toast.LENGTH_LONG).show()
                locationStatusTextView.text = "Brak uprawnień do lokalizacji"
                locationStatusTextView.visibility = TextView.VISIBLE
            }
        }
    }

    private fun getCurrentLocation() {
        locationStatusTextView.visibility = TextView.VISIBLE
        locationStatusTextView.text = "Pobieranie lokalizacji..."
        
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLocation = location
                locationStatusTextView.text = "Lokalizacja pobrana"
                val latLng = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                map.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("Twoja lokalizacja")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                )
            } else {
                locationStatusTextView.text = "Nie udało się pobrać lokalizacji"
            }
        }.addOnFailureListener {
            locationStatusTextView.text = "Błąd pobierania lokalizacji"
            Log.e("RestaurantFinder", "Error getting location", it)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        map.isMyLocationEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true
        map.uiSettings.isZoomControlsEnabled = true
    }

    private fun searchRestaurants(cuisineType: String, radius: Int) {
        if (currentLocation == null) {
            Toast.makeText(this, "Brak lokalizacji", Toast.LENGTH_SHORT).show()
            return
        }

        searchRestaurantsButton.isEnabled = false
        locationStatusTextView.text = "Wyszukiwanie restauracji..."
        locationStatusTextView.visibility = TextView.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val lat = currentLocation!!.latitude
                val lng = currentLocation!!.longitude
                
                val query = "$cuisineType restaurant"
                val foundRestaurants = mutableListOf<Restaurant>()
                val url = "https:"
                
                try {
                    val includedTypes = org.json.JSONArray()
                    includedTypes.put("restaurant")
                    
                    val center = org.json.JSONObject()
                    center.put("latitude", lat)
                    center.put("longitude", lng)
                    
                    val circle = org.json.JSONObject()
                    circle.put("center", center)
                    circle.put("radius", radius.toDouble())
                    
                    val locationRestriction = org.json.JSONObject()
                    locationRestriction.put("circle", circle)
                    
                    val requestBody = org.json.JSONObject()
                    requestBody.put("includedTypes", includedTypes)
                    requestBody.put("maxResultCount", 20)
                    requestBody.put("locationRestriction", locationRestriction)
                    if (cuisineType.isNotEmpty()) {
                        requestBody.put("textQuery", query)
                    }
                    
                    val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("X-Goog-Api-Key", BuildConfig.GOOGLE_MAPS_API_KEY)
                    connection.setRequestProperty("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.rating,places.location")
                    connection.doOutput = true
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    
                    connection.outputStream.use { os ->
                        os.write(requestBody.toString().toByteArray(Charsets.UTF_8))
                    }
                    
                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = org.json.JSONObject(response)
                    
                    if (jsonResponse.has("places")) {
                            val places = jsonResponse.getJSONArray("places")
                            
                            for (i in 0 until places.length()) {
                                val place = places.getJSONObject(i)
                                var rating = 0.0
                                if (place.has("rating")) {
                                    val ratingObj = place.opt("rating")
                                    if (ratingObj is Number) {
                                        rating = ratingObj.toDouble()
                                    } else if (ratingObj is org.json.JSONObject) {
                                        rating = ratingObj.optDouble("rating", 0.0)
                                    }
                                }
                                if (rating == 0.0 || rating >= 4.0) {
                                    var name = "Brak nazwy"
                                    if (place.has("displayName")) {
                                        val displayName = place.optJSONObject("displayName")
                                        name = displayName?.optString("text", "Brak nazwy") ?: "Brak nazwy"
                                    }
                                    val address = place.optString("formattedAddress", "Brak adresu")
                                    val location = place.optJSONObject("location")
                                    val placeLat = location?.optDouble("latitude", 0.0) ?: 0.0
                                    val placeLng = location?.optDouble("longitude", 0.0) ?: 0.0
                                    val placeId = place.optString("id", "")
                                    
                                    if (placeLat != 0.0 && placeLng != 0.0) {
                                        foundRestaurants.add(Restaurant(name, address, rating, placeLat, placeLng, placeId))
                                    }
                                }
                            }
                        }
                    } else {
                        val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                        withContext(Dispatchers.Main) {
                            val errorMsg = when (responseCode) {
                                401 -> "Błąd 401: Brak uprawnień do API. Sprawdź klucz API i uprawnienia."
                                403 -> "Błąd 403: Brak uprawnień. Włącz Places API (New) w Google Cloud Console."
                                400 -> "Błąd 400: Nieprawidłowe żądanie."
                                else -> "Błąd HTTP: $responseCode"
                            }
                            locationStatusTextView.text = "$errorMsg\n\nSzczegóły: $errorResponse"
                            Toast.makeText(this@RestaurantFinderActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        locationStatusTextView.text = "Błąd: ${e.message}"
                        Toast.makeText(this@RestaurantFinderActivity, "Błąd: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
                
                withContext(Dispatchers.Main) {
                    displayRestaurants(foundRestaurants)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    locationStatusTextView.text = "Błąd: ${e.message}"
                    Toast.makeText(this@RestaurantFinderActivity, "Błąd: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    searchRestaurantsButton.isEnabled = true
                }
            }
        }
    }

    private fun displayRestaurants(foundRestaurants: List<Restaurant>) {
        markers.forEach { it.remove() }
        markers.clear()
        restaurants.clear()
        restaurants.addAll(foundRestaurants)
        restaurantsAdapter.notifyDataSetChanged()
        for (restaurant in foundRestaurants) {
            val latLng = LatLng(restaurant.lat, restaurant.lng)
            val marker = map.addMarker(MarkerOptions().position(latLng).title(restaurant.name).snippet("⭐ ${restaurant.rating} | ${restaurant.address}"))
            if (marker != null) markers.add(marker)
        }
        if (foundRestaurants.isNotEmpty()) {
            resultsCardView.visibility = View.VISIBLE
            locationStatusTextView.text = "Znaleziono ${foundRestaurants.size} restauracji"
            if (markers.isNotEmpty()) {
                val bounds = com.google.android.gms.maps.model.LatLngBounds.Builder()
                for (marker in markers) {
                    bounds.include(marker.position)
                }
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
            }
        } else {
            resultsCardView.visibility = View.GONE
            locationStatusTextView.text = "Nie znaleziono restauracji"
        }
    }

    private fun saveFavoriteRestaurant(restaurant: Restaurant) {
        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "Musisz być zalogowany, aby zapisać ulubione", Toast.LENGTH_SHORT).show()
            return
        }

        val restaurantData = hashMapOf(
            "name" to restaurant.name,
            "address" to restaurant.address,
            "rating" to restaurant.rating,
            "lat" to restaurant.lat,
            "lng" to restaurant.lng,
            "placeId" to restaurant.placeId,
            "savedAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("users")
            .document(user.uid)
            .collection("favorite_restaurants")
            .document(restaurant.placeId)
            .set(restaurantData)
            .addOnSuccessListener {
                favoritePlaceIds.add(restaurant.placeId)
                restaurantsAdapter.notifyDataSetChanged()
                Toast.makeText(this, "Zapisano do ulubionych", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("RestaurantFinder", "Error saving favorite", e)
                Toast.makeText(this, "Błąd zapisywania", Toast.LENGTH_SHORT).show()
            }
    }

    private fun removeFavoriteRestaurant(placeId: String) {
        val user = auth.currentUser
        if (user == null) return

        db.collection("users")
            .document(user.uid)
            .collection("favorite_restaurants")
            .document(placeId)
            .delete()
            .addOnSuccessListener {
                favoritePlaceIds.remove(placeId)
                favoriteRestaurants.removeAll { it.placeId == placeId }
                restaurantsAdapter.notifyDataSetChanged()
                favoritesAdapter.notifyDataSetChanged()
                updateFavoritesMap()
                Toast.makeText(this, "Usunięto z ulubionych", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("RestaurantFinder", "Error removing favorite", e)
                Toast.makeText(this, "Błąd usuwania", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadFavoriteRestaurants() {
        val user = auth.currentUser
        if (user == null) {
            noFavoritesTextView.visibility = View.VISIBLE
            favoritesCardView.visibility = View.GONE
            return
        }

        db.collection("users")
            .document(user.uid)
            .collection("favorite_restaurants")
            .get()
            .addOnSuccessListener { documents ->
                favoriteRestaurants.clear()
                favoritePlaceIds.clear()
                
                for (document in documents) {
                    val restaurant = Restaurant(
                        name = document.getString("name") ?: "",
                        address = document.getString("address") ?: "",
                        rating = document.getDouble("rating") ?: 0.0,
                        lat = document.getDouble("lat") ?: 0.0,
                        lng = document.getDouble("lng") ?: 0.0,
                        placeId = document.id
                    )
                    favoriteRestaurants.add(restaurant)
                    favoritePlaceIds.add(restaurant.placeId)
                }

                restaurantsAdapter.notifyDataSetChanged()
                favoritesAdapter.notifyDataSetChanged()
                updateFavoritesMap()

                if (favoriteRestaurants.isEmpty()) {
                    noFavoritesTextView.visibility = View.VISIBLE
                    favoritesCardView.visibility = View.GONE
                } else {
                    noFavoritesTextView.visibility = View.GONE
                    favoritesCardView.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                Log.e("RestaurantFinder", "Error loading favorites", e)
                noFavoritesTextView.visibility = View.VISIBLE
                favoritesCardView.visibility = View.GONE
            }
    }

    private fun updateFavoritesMap() {
        favoriteMarkers.forEach { it.remove() }
        favoriteMarkers.clear()

        if (favoriteRestaurants.isEmpty() || !::favoritesMap.isInitialized) return

        for (restaurant in favoriteRestaurants) {
            val latLng = LatLng(restaurant.lat, restaurant.lng)
            val marker = favoritesMap.addMarker(MarkerOptions().position(latLng).title(restaurant.name).snippet("⭐ ${restaurant.rating} | ${restaurant.address}"))
            if (marker != null) favoriteMarkers.add(marker)
        }

        if (favoriteMarkers.isNotEmpty()) {
            val bounds = LatLngBounds.Builder()
            for (marker in favoriteMarkers) {
                bounds.include(marker.position)
            }
            favoritesMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), 100))
        }
    }

    data class Restaurant(
        val name: String,
        val address: String,
        val rating: Double,
        val lat: Double,
        val lng: Double,
        val placeId: String
    )
}

