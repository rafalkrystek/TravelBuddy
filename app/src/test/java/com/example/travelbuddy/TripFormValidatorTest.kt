package com.example.travelbuddy

import org.junit.Test
import org.junit.Assert.*

class TripFormValidatorTest {
    
    @Test
    fun validateTripForm_withValidData_returnsTrue() {
        // Given
        val country = "Polska"
        val city = "Warszawa"
        val startDate = 1705276800000L // 15.01.2024
        val endDate = 1705881600000L   // 22.01.2024
        
        // When
        val result = TripFormValidator.validateTripForm(country, city, startDate, endDate)
        
        // Then
        assertTrue("Powinno zwrócić true dla prawidłowych danych", result.first)
        assertTrue("Komunikat błędu powinien być pusty", result.second.isEmpty())
    }
    
    @Test
    fun validateTripForm_withEmptyCountry_returnsFalse() {
        // Given
        val country = ""
        val city = "Warszawa"
        val startDate = 1705276800000L
        val endDate = 1705881600000L
        
        // When
        val result = TripFormValidator.validateTripForm(country, city, startDate, endDate)
        
        // Then
        assertFalse("Powinno zwrócić false dla pustego kraju", result.first)
        assertEquals("Wybierz kraj", result.second)
    }
    
    @Test
    fun validateTripForm_withEmptyCity_returnsFalse() {
        // Given
        val country = "Polska"
        val city = ""
        val startDate = 1705276800000L
        val endDate = 1705881600000L
        
        // When
        val result = TripFormValidator.validateTripForm(country, city, startDate, endDate)
        
        // Then
        assertFalse("Powinno zwrócić false dla pustego miasta", result.first)
        assertEquals("Wybierz miasto", result.second)
    }
    
    @Test
    fun validateTripForm_withNullDates_returnsFalse() {
        // Given
        val country = "Polska"
        val city = "Warszawa"
        val startDate: Long? = null
        val endDate: Long? = null
        
        // When
        val result = TripFormValidator.validateTripForm(country, city, startDate, endDate)
        
        // Then
        assertFalse("Powinno zwrócić false dla null dat", result.first)
        assertEquals("Wybierz daty", result.second)
    }
    
    @Test
    fun validateTripForm_withStartDateAfterEndDate_returnsFalse() {
        // Given
        val country = "Polska"
        val city = "Warszawa"
        val startDate = 1705881600000L // 22.01.2024
        val endDate = 1705276800000L   // 15.01.2024
        
        // When
        val result = TripFormValidator.validateTripForm(country, city, startDate, endDate)
        
        // Then
        assertFalse("Powinno zwrócić false gdy data początku jest po dacie końca", result.first)
        assertEquals("Data końca musi być późniejsza niż data początku", result.second)
    }
    
    @Test
    fun validateTripForm_withSameStartAndEndDate_returnsFalse() {
        // Given
        val country = "Polska"
        val city = "Warszawa"
        val startDate = 1705276800000L
        val endDate = 1705276800000L // ta sama data
        
        // When
        val result = TripFormValidator.validateTripForm(country, city, startDate, endDate)
        
        // Then
        assertFalse("Powinno zwrócić false gdy daty są takie same", result.first)
        assertEquals("Data końca musi być późniejsza niż data początku", result.second)
    }
    
}

