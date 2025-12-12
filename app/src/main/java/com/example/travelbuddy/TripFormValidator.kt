package com.example.travelbuddy

/**
 * Klasa pomocnicza do walidacji formularza podróży.
 * Wyodrębniona logika biznesowa do łatwego testowania.
 */
object TripFormValidator {
    
    /**
     * Waliduje dane formularza podróży
     * @return Pair<Boolean, String> - (czy walidacja przeszła, komunikat błędu)
     */
    fun validateTripForm(
        country: String,
        city: String,
        startDate: Long?,
        endDate: Long?,
        budget: Int
    ): Pair<Boolean, String> {
        return when {
            country.isEmpty() -> Pair(false, "Wybierz kraj")
            city.isEmpty() -> Pair(false, "Wybierz miasto")
            startDate == null || endDate == null -> Pair(false, "Wybierz daty")
            startDate >= endDate -> Pair(false, "Data końca musi być późniejsza niż data początku")
            budget <= 0 -> Pair(false, "Wybierz budżet")
            else -> Pair(true, "")
        }
    }
    
}

