package com.example.travelbuddy.helpers

import java.text.SimpleDateFormat
import java.util.*

object DateHelper {
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    
    fun formatDate(timestamp: Long): String = dateFormatter.format(Date(timestamp))
    
    fun formatDate(date: Date): String = dateFormatter.format(date)
    
    fun parseDate(dateString: String): Date? = try {
        dateFormatter.parse(dateString)
    } catch (e: Exception) {
        null
    }
    
    fun calculateDays(startTimestamp: Long, endTimestamp: Long): Int {
        return ((endTimestamp - startTimestamp) / (1000 * 60 * 60 * 24)).toInt() + 1
    }
}

