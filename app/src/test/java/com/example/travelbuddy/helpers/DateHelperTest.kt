package com.example.travelbuddy.helpers

import org.junit.Test
import org.junit.Assert.*
import java.util.*

class DateHelperTest {
    
    @Test
    fun formatDate_withValidTimestamp_returnsFormattedString() {
        // Given: timestamp dla 15.01.2024
        val timestamp = 1705276800000L // 15.01.2024 00:00:00 UTC
        
        // When
        val result = DateHelper.formatDate(timestamp)
        
        // Then
        assertTrue("Powinno być w formacie dd.MM.yyyy", result.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}")))
        assertTrue("Powinno zawierać datę", result.isNotEmpty())
    }
    
    @Test
    fun formatDate_withDateObject_returnsFormattedString() {
        // Given
        val calendar = Calendar.getInstance()
        calendar.set(2024, Calendar.JANUARY, 15)
        val date = calendar.time
        
        // When
        val result = DateHelper.formatDate(date)
        
        // Then
        assertTrue("Powinno być w formacie dd.MM.yyyy", result.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}")))
    }
    
    @Test
    fun parseDate_withValidString_returnsDate() {
        // Given
        val dateString = "15.01.2024"
        
        // When
        val result = DateHelper.parseDate(dateString)
        
        // Then
        assertNotNull("Powinno zwrócić datę", result)
        // Sprawdź czy zwrócona data ma poprawny format
        if (result != null) {
            val formatted = DateHelper.formatDate(result)
            assertTrue("Zwrócona data powinna być w formacie dd.MM.yyyy", formatted.matches(Regex("\\d{2}\\.\\d{2}\\.\\d{4}")))
        }
    }
    
    @Test
    fun parseDate_withInvalidString_returnsNull() {
        // Given
        val dateString = "invalid-date"
        
        // When
        val result = DateHelper.parseDate(dateString)
        
        // Then
        assertNull("Powinno zwrócić null dla nieprawidłowej daty", result)
    }
    
    @Test
    fun parseDate_withEmptyString_returnsNull() {
        // Given
        val dateString = ""
        
        // When
        val result = DateHelper.parseDate(dateString)
        
        // Then
        assertNull("Powinno zwrócić null dla pustego stringa", result)
    }
    
    @Test
    fun calculateDays_withValidDates_returnsCorrectDays() {
        // Given: 7 dni różnicy
        val startTimestamp = 1705276800000L // 15.01.2024
        val endTimestamp = 1705881600000L   // 22.01.2024
        
        // When
        val result = DateHelper.calculateDays(startTimestamp, endTimestamp)
        
        // Then
        assertEquals("Powinno zwrócić 8 dni (włącznie z początkiem i końcem)", 8, result)
    }
    
    @Test
    fun calculateDays_withSameDate_returnsOne() {
        // Given: ten sam dzień
        val timestamp = 1705276800000L
        
        // When
        val result = DateHelper.calculateDays(timestamp, timestamp)
        
        // Then
        assertEquals("Powinno zwrócić 1 dzień", 1, result)
    }
    
    @Test
    fun calculateDays_withEndBeforeStart_returnsNegative() {
        // Given: data końca przed początkiem
        val startTimestamp = 1705881600000L // 22.01.2024
        val endTimestamp = 1705276800000L   // 15.01.2024
        
        // When
        val result = DateHelper.calculateDays(startTimestamp, endTimestamp)
        
        // Then
        assertTrue("Powinno zwrócić ujemną wartość", result < 0)
    }
}

