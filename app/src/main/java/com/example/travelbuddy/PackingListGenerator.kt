package com.example.travelbuddy

/**
 * Generator listy pakowania na podstawie temperatury i innych parametrów.
 * Wyodrębniona logika biznesowa do łatwego testowania.
 */
object PackingListGenerator {
    
    /**
     * Określa typ klimatu na podstawie temperatury
     */
    fun determineClimateType(avgTemp: Double, minTemp: Double, maxTemp: Double): String {
        return when {
            avgTemp >= 28 -> "BARDZO_GORĄCO"
            avgTemp >= 22 -> "GORĄCO"
            avgTemp >= 15 -> "CIEPŁO"
            avgTemp >= 8 -> "CHŁODNO"
            avgTemp >= 0 -> "ZIMNO"
            else -> "BARDZO_ZIMNO"
        }
    }
    
    /**
     * Pobiera opis klimatu
     */
    fun getClimateDescription(climateType: String): String {
        return when (climateType) {
            "BARDZO_GORĄCO" -> "Bardzo gorąco (>28°C) - lekkie ubrania letnie"
            "GORĄCO" -> "Gorąco (22-28°C) - ubrania letnie"
            "CIEPŁO" -> "Ciepło (15-22°C) - lekkie ubrania z bluzą"
            "CHŁODNO" -> "Chłodno (8-15°C) - ubrania warstwowe"
            "ZIMNO" -> "Zimno (0-8°C) - ciepłe ubrania"
            "BARDZO_ZIMNO" -> "Bardzo zimno (<0°C) - pełna odzież zimowa"
            else -> "Nieznany"
        }
    }
    
    /**
     * Mapuje sezon na typ klimatu (fallback gdy nie ma danych pogodowych)
     */
    fun mapSeasonToClimate(season: String): String {
        return when (season) {
            "Lato" -> "GORĄCO"
            "Zima" -> "ZIMNO"
            else -> "CIEPŁO"
        }
    }
}

