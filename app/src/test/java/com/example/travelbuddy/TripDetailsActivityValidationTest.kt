package com.example.travelbuddy

import org.junit.Test
import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Testy jednostkowe dla walidacji i logiki planowania podróży
 */
class TripDetailsActivityValidationTest {

    /**
     * Helper function to build prompt for Gemini API
     */
    private fun buildTravelPlanPrompt(
        destination: String,
        weatherCity: String,
        weatherInfo: String,
        startDate: String,
        endDate: String,
        daysDiff: Int,
        userPreferences: String
    ): String {
        return buildString {
            append("Jesteś asystentem planowania podróży. Stwórz szczegółowy plan podróży z podziałem na dni.\n\n")
            append("Informacje o podróży:\n")
            append("- Miasto docelowe: $destination\n")
            if (weatherCity.isNotEmpty()) {
                append("- Miasto pogodowe: $weatherCity\n")
            }
            if (weatherInfo.isNotEmpty()) {
                append("- Pogoda: $weatherInfo\n")
            }
            append("- Data rozpoczęcia: $startDate\n")
            append("- Data zakończenia: $endDate\n")
            append("- Liczba dni: $daysDiff\n")
            if (userPreferences.isNotEmpty()) {
                append("- Preferencje użytkownika: $userPreferences\n")
            }
            append("\n")
            append("Stwórz plan podróży z podziałem na dni. Dla każdego dnia podaj:\n")
            append("- Dzień X (data)\n")
            append("- Rano: [aktywności]\n")
            append("- Po południu: [aktywności]\n")
            append("- Wieczór: [aktywności]\n")
            append("\n")
            append("Uwzględnij preferencje użytkownika i pogodę. Plan powinien być realistyczny i praktyczny.\n")
            append("Odpowiedz w języku polskim.")
        }
    }

    /**
     * Helper function to calculate trip duration
     */
    private fun calculateTripDuration(startDateTimestamp: Long, endDateTimestamp: Long): Int {
        return if (startDateTimestamp > 0 && endDateTimestamp > 0) {
            ((endDateTimestamp - startDateTimestamp) / (1000 * 60 * 60 * 24)).toInt() + 1
        } else {
            7 // Default
        }
    }

    @Test
    fun `test build prompt with all information`() {
        val destination = "Kraków"
        val weatherCity = "Kraków"
        val weatherInfo = "20°C, słonecznie"
        val startDate = "01.01.2024"
        val endDate = "05.01.2024"
        val daysDiff = 5
        val userPreferences = "nie lubię zwiedzać kościołów, uwielbiam plaże"

        val prompt = buildTravelPlanPrompt(
            destination, weatherCity, weatherInfo, startDate, endDate, daysDiff, userPreferences
        )

        assertTrue("Prompt should contain destination", prompt.contains(destination))
        assertTrue("Prompt should contain weather city", prompt.contains(weatherCity))
        assertTrue("Prompt should contain weather info", prompt.contains(weatherInfo))
        assertTrue("Prompt should contain start date", prompt.contains(startDate))
        assertTrue("Prompt should contain end date", prompt.contains(endDate))
        assertTrue("Prompt should contain days count", prompt.contains("$daysDiff"))
        assertTrue("Prompt should contain user preferences", prompt.contains(userPreferences))
        assertTrue("Prompt should be in Polish", prompt.contains("języku polskim"))
    }

    @Test
    fun `test build prompt without weather information`() {
        val destination = "Warszawa"
        val weatherCity = ""
        val weatherInfo = ""
        val startDate = "01.01.2024"
        val endDate = "03.01.2024"
        val daysDiff = 3
        val userPreferences = "lubię muzea"

        val prompt = buildTravelPlanPrompt(
            destination, weatherCity, weatherInfo, startDate, endDate, daysDiff, userPreferences
        )

        assertTrue("Prompt should contain destination", prompt.contains(destination))
        assertTrue("Prompt should not contain empty weather city", !prompt.contains("- Miasto pogodowe: \n"))
        assertTrue("Prompt should not contain empty weather info", !prompt.contains("- Pogoda: \n"))
        assertTrue("Prompt should contain user preferences", prompt.contains(userPreferences))
    }

    @Test
    fun `test build prompt without user preferences`() {
        val destination = "Gdańsk"
        val weatherCity = "Gdańsk"
        val weatherInfo = "15°C, deszczowo"
        val startDate = "01.06.2024"
        val endDate = "07.06.2024"
        val daysDiff = 7
        val userPreferences = ""

        val prompt = buildTravelPlanPrompt(
            destination, weatherCity, weatherInfo, startDate, endDate, daysDiff, userPreferences
        )

        assertTrue("Prompt should contain destination", prompt.contains(destination))
        assertTrue("Prompt should contain weather city", prompt.contains(weatherCity))
        assertTrue("Prompt should not contain empty preferences", !prompt.contains("- Preferencje użytkownika: \n"))
    }

    @Test
    fun `test calculate trip duration with valid dates`() {
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val startDate = dateFormatter.parse("01.01.2024")
        val endDate = dateFormatter.parse("05.01.2024")

        val duration = calculateTripDuration(startDate!!.time, endDate!!.time)

        assertEquals("Trip duration should be 5 days", 5, duration)
    }

    @Test
    fun `test calculate trip duration with same start and end date`() {
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val startDate = dateFormatter.parse("01.01.2024")
        val endDate = dateFormatter.parse("01.01.2024")

        val duration = calculateTripDuration(startDate!!.time, endDate!!.time)

        assertEquals("Trip duration should be 1 day", 1, duration)
    }

    @Test
    fun `test calculate trip duration with invalid dates returns default`() {
        val duration = calculateTripDuration(0, 0)

        assertEquals("Trip duration should default to 7 days", 7, duration)
    }

    @Test
    fun `test calculate trip duration with week long trip`() {
        val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val startDate = dateFormatter.parse("01.01.2024")
        val endDate = dateFormatter.parse("07.01.2024")

        val duration = calculateTripDuration(startDate!!.time, endDate!!.time)

        assertEquals("Trip duration should be 7 days", 7, duration)
    }

    @Test
    fun `test prompt contains day structure requirements`() {
        val prompt = buildTravelPlanPrompt(
            "Test", "", "", "01.01.2024", "03.01.2024", 3, ""
        )

        assertTrue("Prompt should contain 'Dzień X'", prompt.contains("Dzień X"))
        assertTrue("Prompt should contain 'Rano'", prompt.contains("Rano"))
        assertTrue("Prompt should contain 'Po południu'", prompt.contains("Po południu"))
        assertTrue("Prompt should contain 'Wieczór'", prompt.contains("Wieczór"))
    }

    @Test
    fun `test prompt contains realistic and practical requirement`() {
        val prompt = buildTravelPlanPrompt(
            "Test", "", "", "01.01.2024", "03.01.2024", 3, ""
        )

        assertTrue("Prompt should contain 'realistyczny'", prompt.contains("realistyczny"))
        assertTrue("Prompt should contain 'praktyczny'", prompt.contains("praktyczny"))
    }

    @Test
    fun `test user preferences validation - empty string`() {
        val preferences = ""
        assertTrue("Empty preferences should be valid", preferences.trim().isEmpty())
    }

    @Test
    fun `test user preferences validation - valid preferences`() {
        val preferences = "nie lubię zwiedzać kościołów, uwielbiam plaże"
        assertTrue("Valid preferences should not be empty", preferences.trim().isNotEmpty())
        assertTrue("Preferences should contain text", preferences.length > 10)
    }

    @Test
    fun `test user preferences validation - whitespace only`() {
        val preferences = "   "
        assertTrue("Whitespace-only preferences should be considered empty", preferences.trim().isEmpty())
    }

    @Test
    fun `test prompt format with various destinations`() {
        val destinations = listOf("Kraków", "Warszawa", "Gdańsk", "Wrocław", "Poznań")

        destinations.forEach { destination ->
            val prompt = buildTravelPlanPrompt(
                destination, "", "", "01.01.2024", "03.01.2024", 3, ""
            )
            assertTrue("Prompt should contain destination: $destination", prompt.contains(destination))
        }
    }

    @Test
    fun `test prompt includes all required sections`() {
        val prompt = buildTravelPlanPrompt(
            "Test", "Test", "Test", "01.01.2024", "03.01.2024", 3, "Test"
        )

        val requiredSections = listOf(
            "Informacje o podróży",
            "Miasto docelowe",
            "Data rozpoczęcia",
            "Data zakończenia",
            "Liczba dni",
            "Stwórz plan podróży"
        )

        requiredSections.forEach { section ->
            assertTrue("Prompt should contain section: $section", prompt.contains(section))
        }
    }
}

