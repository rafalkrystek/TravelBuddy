package com.example.travelbuddy

import org.junit.Test
import org.junit.Assert.*

class PackingListGeneratorTest {
    
    @Test
    fun determineClimateType_withVeryHotTemperature_returnsBardzoGoraco() {
        // Given
        val avgTemp = 30.0
        val minTemp = 28.0
        val maxTemp = 32.0
        
        // When
        val result = PackingListGenerator.determineClimateType(avgTemp, minTemp, maxTemp)
        
        // Then
        assertEquals("BARDZO_GORĄCO", result)
    }
    
    @Test
    fun determineClimateType_withHotTemperature_returnsGoraco() {
        // Given
        val avgTemp = 25.0
        val minTemp = 22.0
        val maxTemp = 28.0
        
        // When
        val result = PackingListGenerator.determineClimateType(avgTemp, minTemp, maxTemp)
        
        // Then
        assertEquals("GORĄCO", result)
    }
    
    @Test
    fun determineClimateType_withWarmTemperature_returnsCieplo() {
        // Given
        val avgTemp = 18.0
        val minTemp = 15.0
        val maxTemp = 22.0
        
        // When
        val result = PackingListGenerator.determineClimateType(avgTemp, minTemp, maxTemp)
        
        // Then
        assertEquals("CIEPŁO", result)
    }
    
    @Test
    fun determineClimateType_withColdTemperature_returnsChlodno() {
        // Given
        val avgTemp = 12.0
        val minTemp = 8.0
        val maxTemp = 15.0
        
        // When
        val result = PackingListGenerator.determineClimateType(avgTemp, minTemp, maxTemp)
        
        // Then
        assertEquals("CHŁODNO", result)
    }
    
    @Test
    fun determineClimateType_withFreezingTemperature_returnsZimno() {
        // Given
        val avgTemp = 5.0
        val minTemp = 0.0
        val maxTemp = 8.0
        
        // When
        val result = PackingListGenerator.determineClimateType(avgTemp, minTemp, maxTemp)
        
        // Then
        assertEquals("ZIMNO", result)
    }
    
    @Test
    fun determineClimateType_withVeryColdTemperature_returnsBardzoZimno() {
        // Given
        val avgTemp = -5.0
        val minTemp = -10.0
        val maxTemp = 0.0
        
        // When
        val result = PackingListGenerator.determineClimateType(avgTemp, minTemp, maxTemp)
        
        // Then
        assertEquals("BARDZO_ZIMNO", result)
    }
    
    @Test
    fun getClimateDescription_withValidClimateType_returnsDescription() {
        // Given
        val climateType = "BARDZO_GORĄCO"
        
        // When
        val result = PackingListGenerator.getClimateDescription(climateType)
        
        // Then
        assertTrue("Powinno zwrócić opis", result.isNotEmpty())
        assertTrue("Powinno zawierać informację o temperaturze", result.contains("28"))
    }
    
    @Test
    fun getClimateDescription_withInvalidClimateType_returnsNieznany() {
        // Given
        val climateType = "INVALID"
        
        // When
        val result = PackingListGenerator.getClimateDescription(climateType)
        
        // Then
        assertEquals("Nieznany", result)
    }
    
    @Test
    fun mapSeasonToClimate_withLato_returnsGoraco() {
        // Given
        val season = "Lato"
        
        // When
        val result = PackingListGenerator.mapSeasonToClimate(season)
        
        // Then
        assertEquals("GORĄCO", result)
    }
    
    @Test
    fun mapSeasonToClimate_withZima_returnsZimno() {
        // Given
        val season = "Zima"
        
        // When
        val result = PackingListGenerator.mapSeasonToClimate(season)
        
        // Then
        assertEquals("ZIMNO", result)
    }
    
    @Test
    fun mapSeasonToClimate_withOtherSeason_returnsCieplo() {
        // Given
        val season = "Wiosna"
        
        // When
        val result = PackingListGenerator.mapSeasonToClimate(season)
        
        // Then
        assertEquals("CIEPŁO", result)
    }
}

