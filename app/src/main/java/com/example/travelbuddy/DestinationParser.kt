package com.example.travelbuddy

/**
 * Klasa pomocnicza do parsowania destination string.
 * Obsługuje formaty: "Miasto, Kraj" oraz "Kraj" (stary format).
 */
object DestinationParser {
    
    /**
     * Parsuje destination string do pary (city, country)
     * @param destination String w formacie "Miasto, Kraj" lub "Kraj"
     * @return Pair<String, String> - (city, country)
     */
    fun parseDestination(destination: String): Pair<String, String> {
        if (destination.contains(",")) {
            val parts = destination.split(",").map { it.trim() }
            if (parts.size >= 2) {
                return Pair(parts[0], parts[1])
            }
        }
        // Stary format - destination to sam kraj
        return Pair("", destination)
    }
    
    /**
     * Tworzy destination string z miasta i kraju
     * @return String w formacie "Miasto, Kraj"
     */
    fun createDestination(city: String, country: String): String {
        return "$city, $country"
    }
}


