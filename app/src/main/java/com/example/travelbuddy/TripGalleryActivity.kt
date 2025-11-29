package com.example.travelbuddy

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.sqrt

class TripGalleryActivity : BaseActivity() {

    private lateinit var destination: String
    private lateinit var photosRecyclerView: RecyclerView
    private lateinit var photosAdapter: PhotoAdapter
    private val photos = mutableListOf<Photo>()
    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_gallery)

        destination = intent.getStringExtra("trip_destination") ?: ""
        if (destination.isEmpty()) {
            Toast.makeText(this, "Błąd: Brak miejsca docelowego", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.destinationTextView).text = "Galeria: $destination"
        statusTextView = findViewById(R.id.statusTextView)
        photosRecyclerView = findViewById(R.id.photosRecyclerView)

        photosAdapter = PhotoAdapter(photos) { showFullScreenImage(it.url) }
        photosRecyclerView.layoutManager = GridLayoutManager(this, 2)
        photosRecyclerView.adapter = photosAdapter

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }
        loadPhotos()
    }

    private fun loadPhotos() {
        statusTextView.text = "Ładowanie zdjęć..."
        statusTextView.visibility = TextView.VISIBLE
        photosRecyclerView.visibility = RecyclerView.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val allPhotos = mutableListOf<Photo>()
                val queries = getSearchQueriesForCountry(destination)
                Log.d("TripGalleryActivity", "Loading photos for: $destination, queries: ${queries.size}")
                
                for (query in queries) {
                    if (allPhotos.size >= 10) break
                    
                    try {
                        val enhancedQuery = if (!query.lowercase().contains(destination.lowercase())) {
                            "$query $destination"
                        } else {
                            query
                        }
                        
                        Log.d("TripGalleryActivity", "Trying query: $enhancedQuery (current photos: ${allPhotos.size})")
                        
                        val pexelsPhotos = fetchPhotosFromPexels(enhancedQuery)
                        Log.d("TripGalleryActivity", "Pexels returned ${pexelsPhotos.size} photos for: $enhancedQuery")
                        if (pexelsPhotos.isNotEmpty()) {
                            allPhotos.addAll(pexelsPhotos)
                            if (allPhotos.size >= 10) break
                        }
                        
                        if (pexelsPhotos.isEmpty()) {
                            val pixabayPhotos = fetchPhotosFromPixabay(enhancedQuery)
                            Log.d("TripGalleryActivity", "Pixabay returned ${pixabayPhotos.size} photos for: $enhancedQuery")
                            if (pixabayPhotos.isNotEmpty()) {
                                allPhotos.addAll(pixabayPhotos)
                                if (allPhotos.size >= 10) break
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("TripGalleryActivity", "Error for query '$query': ${e.message}", e)
                    }
                }
                
                Log.d("TripGalleryActivity", "Total photos collected: ${allPhotos.size}")
                
                if (allPhotos.isEmpty()) {
                    Log.d("TripGalleryActivity", "No photos from specific queries, trying general queries")
                    val generalQueries = listOf(
                        "$destination landmarks",
                        "$destination tourism",
                        "$destination city",
                        "$destination country",
                        "$destination travel"
                    )
                    
                    for (query in generalQueries) {
                        if (allPhotos.size >= 10) break
                        try {
                            val pexelsPhotos = fetchPhotosFromPexels(query)
                            if (pexelsPhotos.isNotEmpty()) {
                                allPhotos.addAll(pexelsPhotos)
                                if (allPhotos.size >= 10) break
                            }
                            if (pexelsPhotos.isEmpty()) {
                                val pixabayPhotos = fetchPhotosFromPixabay(query)
                                if (pixabayPhotos.isNotEmpty()) {
                                    allPhotos.addAll(pixabayPhotos)
                                    if (allPhotos.size >= 10) break
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("TripGalleryActivity", "Error with general query '$query': ${e.message}")
                        }
                    }
                    Log.d("TripGalleryActivity", "After general queries: ${allPhotos.size} photos")
                }
                
                withContext(Dispatchers.Main) {
                    photos.clear()
                    val photosToShow = allPhotos.take(10)
                    photos.addAll(photosToShow)
                    photosAdapter.notifyDataSetChanged()
                    if (photos.isEmpty()) {
                        val pexelsKey = BuildConfig.PEXELS_API_KEY
                        val pixabayKey = BuildConfig.PIXABAY_API_KEY
                        val hasPexels = pexelsKey.isNotEmpty() && pexelsKey != "YOUR_PEXELS_API_KEY"
                        val hasPixabay = pixabayKey.isNotEmpty() && pixabayKey != "YOUR_PIXABAY_API_KEY"
                        
                        val errorMsg = when {
                            !hasPexels && !hasPixabay -> "Brak kluczy API.\n\nDodaj PEXELS_API_KEY lub PIXABAY_API_KEY do pliku local.properties"
                            !hasPexels -> "Brak klucza Pexels API.\n\nDodaj PEXELS_API_KEY do pliku local.properties"
                            else -> "Nie udało się załadować zdjęć.\n\nSprawdź połączenie z internetem lub spróbuj ponownie później."
                        }
                        statusTextView.text = errorMsg
                        statusTextView.visibility = TextView.VISIBLE
                        photosRecyclerView.visibility = RecyclerView.GONE
                        Log.d("TripGalleryActivity", "No photos loaded. Pexels: $hasPexels, Pixabay: $hasPixabay")
                    } else {
                        statusTextView.visibility = TextView.GONE
                        photosRecyclerView.visibility = RecyclerView.VISIBLE
                        Log.d("TripGalleryActivity", "Successfully loaded ${photos.size} photos")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusTextView.text = "Błąd: ${e.message}"
                    statusTextView.visibility = TextView.VISIBLE
                }
            }
        }
    }

    private fun getSearchQueriesForCountry(country: String): List<String> {
        val c = country.lowercase().trim()
        return when (c) {
            "polska", "poland" -> listOf(
                "Palace of Culture and Science Warsaw",
                "Palace of Culture Warsaw",
                "Wawel Castle Krakow",
                "Wawel Castle",
                "Warsaw Old Town",
                "Krakow Old Town",
                "Main Market Square Krakow",
                "Malbork Castle",
                "Wieliczka Salt Mine",
                "Tatra Mountains Poland",
                "Morskie Oko",
                "Gdansk Old Town",
                "Gdansk",
                "Wroclaw Market Square",
                "Auschwitz Memorial",
                "Bialowieza Forest",
                "Zamosc Old Town",
                "Torun Old Town",
                "Poland landmarks",
                "Poland tourism"
            )
            "brazylia", "brazil" -> listOf("Christ the Redeemer", "Iguazu Falls", "Amazon Rainforest", "Copacabana Beach", "Rio de Janeiro", "Brazil landmarks")
            "francja", "france" -> listOf("Eiffel Tower", "Louvre Museum", "Notre Dame", "Mont Saint Michel", "Versailles", "France landmarks")
            "włochy", "italy", "italia" -> listOf("Colosseum", "Leaning Tower Pisa", "Venice", "Florence", "Amalfi Coast", "Italy landmarks")
            "hiszpania", "spain" -> listOf("Sagrada Familia", "Alhambra", "Park Guell", "Seville", "Spain landmarks")
            "niemcy", "germany" -> listOf("Neuschwanstein", "Brandenburg Gate", "Cologne Cathedral", "Germany landmarks")
            "anglia", "england", "uk" -> listOf("Big Ben", "Tower Bridge", "Stonehenge", "London landmarks")
            "japonia", "japan" -> listOf("Mount Fuji", "Tokyo Tower", "Fushimi Inari", "Kyoto", "Japan landmarks")
            "chiny", "china" -> listOf("Great Wall of China", "Forbidden City", "Shanghai", "China landmarks")
            "angola" -> listOf("Luanda", "Angola landmarks", "Angola tourism")
            "andora", "andorra" -> listOf(
                "Andorra la Vella",
                "Andorra capital",
                "Andorra Pyrenees",
                "Andorra mountains",
                "Andorra ski resort",
                "Andorra landmarks",
                "Andorra tourism",
                "Andorra valley",
                "Andorra church",
                "Andorra architecture"
            )
            else -> {
                val countryName = country.trim()
                listOf(
                    "$countryName capital city",
                    "$countryName main city",
                    "$countryName famous landmarks",
                    "$countryName tourist attractions",
                    "$countryName architecture",
                    "$countryName nature",
                    "$countryName mountains",
                    "$countryName beaches",
                    "$countryName culture",
                    "$countryName travel"
                )
            }
        }
    }

    private suspend fun fetchPhotosFromPexels(query: String): List<Photo> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.PEXELS_API_KEY
            if (apiKey.isEmpty() || apiKey == "YOUR_PEXELS_API_KEY") {
                Log.d("TripGalleryActivity", "Pexels API key not set")
                return@withContext emptyList()
            }
            
            val url = "https://api.pexels.com/v1/search?query=${java.net.URLEncoder.encode(query, "UTF-8")}&per_page=20"
            Log.d("TripGalleryActivity", "Pexels URL: $url")
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", apiKey)
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            Log.d("TripGalleryActivity", "Pexels response code: $responseCode for query: $query")
            
            if (responseCode == 401) {
                Log.e("TripGalleryActivity", "Pexels API: Unauthorized - check API key")
            } else if (responseCode == 429) {
                Log.e("TripGalleryActivity", "Pexels API: Rate limit exceeded")
            }
            
            if (responseCode == 200) {
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                if (json.has("photos")) {
                    val photosArray = json.getJSONArray("photos")
                    val photosList = (0 until photosArray.length()).map {
                        val p = photosArray.getJSONObject(it)
                        Photo(p.getJSONObject("src").getString("large"), p.getString("photographer"), query)
                    }
                    Log.d("TripGalleryActivity", "Pexels returned ${photosList.size} photos for: $query")
                    photosList
                } else {
                    Log.d("TripGalleryActivity", "Pexels response has no 'photos' field")
                    emptyList()
                }
            } else {
                val errorMessage = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                Log.e("TripGalleryActivity", "Pexels API error $responseCode: $errorMessage")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("TripGalleryActivity", "Pexels error for query '$query': ${e.message}", e)
            emptyList()
        }
    }

    private suspend fun fetchPhotosFromPixabay(query: String): List<Photo> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.PIXABAY_API_KEY
            if (apiKey.isEmpty() || apiKey == "YOUR_PIXABAY_API_KEY") {
                Log.d("TripGalleryActivity", "Pixabay API key not set")
                return@withContext emptyList()
            }
            
            val url = "https://pixabay.com/api/?key=$apiKey&q=${java.net.URLEncoder.encode(query, "UTF-8")}&image_type=photo&per_page=20"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            val responseCode = connection.responseCode
            Log.d("TripGalleryActivity", "Pixabay response code: $responseCode for query: $query")
            
            if (responseCode == 200) {
                val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                if (json.has("hits")) {
                    val hits = json.getJSONArray("hits")
                    val photosList = (0 until hits.length()).map {
                        val h = hits.getJSONObject(it)
                        Photo(
                            h.optString("largeImageURL", h.optString("webformatURL", h.getString("previewURL"))),
                            h.getString("user"),
                            query
                        )
                    }
                    Log.d("TripGalleryActivity", "Pixabay returned ${photosList.size} photos for: $query")
                    photosList
                } else {
                    Log.d("TripGalleryActivity", "Pixabay response has no 'hits' field")
                    emptyList()
                }
            } else {
                val errorMessage = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error message"
                Log.e("TripGalleryActivity", "Pixabay API error $responseCode: $errorMessage")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("TripGalleryActivity", "Pixabay error for query '$query': ${e.message}", e)
            emptyList()
        }
    }

    private fun showFullScreenImage(imageUrl: String) {
        android.util.Log.d("TripGalleryActivity", "showFullScreenImage called with: $imageUrl")
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_fullscreen_image, null)
        val imageView = view.findViewById<ImageView>(R.id.fullScreenImageView)
        val closeButton = view.findViewById<ImageButton>(R.id.closeButton)

        val dialog = AlertDialog.Builder(this).setView(view).setCancelable(true).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.black)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        
        Glide.with(this)
            .load(imageUrl)
            .fitCenter()
            .placeholder(android.R.color.black)
            .error(android.R.color.black)
            .into(imageView)
        
        android.util.Log.d("TripGalleryActivity", "Image loaded into ImageView")

        imageView.setOnClickListener {
            dialog.dismiss()
        }

        closeButton.setOnClickListener { dialog.dismiss() }
        dialog.show()
        android.util.Log.d("TripGalleryActivity", "Dialog shown")
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun midPoint(point: android.graphics.PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    data class Photo(val url: String, val photographer: String, val query: String)

    class PhotoAdapter(private val photos: List<Photo>, private val onClick: (Photo) -> Unit) :
        RecyclerView.Adapter<PhotoAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.photoImageView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_photo, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val photo = photos[position]
            Glide.with(holder.imageView.context).load(photo.url).centerCrop().into(holder.imageView)
            
            holder.itemView.setOnClickListener { 
                android.util.Log.d("PhotoAdapter", "Item clicked: ${photo.url}")
                onClick(photo) 
            }
            holder.imageView.setOnClickListener { 
                android.util.Log.d("PhotoAdapter", "ImageView clicked: ${photo.url}")
                onClick(photo) 
            }
            
            holder.itemView.isClickable = true
            holder.itemView.isFocusable = true
            holder.imageView.isClickable = true
            holder.imageView.isFocusable = true
        }

        override fun getItemCount() = photos.size
    }
}
