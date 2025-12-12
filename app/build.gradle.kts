plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services") // Dodane do Firebase
}

// Load local properties for API keys
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = mutableMapOf<String, String>()
if (localPropertiesFile.exists()) {
    localPropertiesFile.readLines().forEach { line ->
        if (line.contains("=") && !line.trimStart().startsWith("#")) {
            val (key, value) = line.split("=", limit = 2)
            localProperties[key.trim()] = value.trim()
        }
    }
}

android {
    namespace = "com.example.travelbuddy"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.travelbuddy"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Gemini API Key from local.properties or default
        val geminiApiKey = localProperties["GEMINI_API_KEY"] ?: "YOUR_GEMINI_API_KEY"
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        
        // OpenWeatherMap API Key from local.properties or default
        val openWeatherApiKey = localProperties["OPENWEATHER_API_KEY"] ?: "YOUR_OPENWEATHER_API_KEY"
        buildConfigField("String", "OPENWEATHER_API_KEY", "\"$openWeatherApiKey\"")
        
        // Google Maps API Key from local.properties or default
        val googleMapsApiKey = localProperties["GOOGLE_MAPS_API_KEY"] ?: "YOUR_GOOGLE_MAPS_API_KEY"
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"$googleMapsApiKey\"")
        
        // Set manifest placeholder for Google Maps API key
        manifestPlaceholders["googleMapsApiKey"] = googleMapsApiKey
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation("com.google.firebase:firebase-auth-ktx:22.3.0")
    implementation("com.google.firebase:firebase-firestore-ktx:24.10.0")
    implementation("com.google.android.material:material:1.11.0")
    
    // RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    
    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Google Gemini API - using latest stable version
    // Sprawdź dostępne wersje: https://mvnrepository.com/artifact/com.google.ai.client.generativeai/generativeai
    implementation("com.google.ai.client.generativeai:generativeai:0.2.2")
    
    // Google Maps and Places API
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.libraries.places:places:3.3.0")
    
    // Security - Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    testImplementation(libs.junit)
    // JSON library for unit tests (org.json.JSONObject is Android-only)
    testImplementation("org.json:json:20240303")
    // Mockito for mocking in unit tests
    testImplementation("org.mockito:mockito-core:5.1.1")
    testImplementation("org.mockito:mockito-inline:5.1.1")
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
}
