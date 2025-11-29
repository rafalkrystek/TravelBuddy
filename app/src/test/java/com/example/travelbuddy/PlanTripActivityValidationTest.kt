package com.example.travelbuddy

import org.junit.Test
import org.junit.Assert.*

class PlanTripActivityValidationTest {
    private fun validateTripInputs(destination: String, startDate: Long?, endDate: Long?, budget: Int): Boolean {
        if (destination.isEmpty()) return false
        if (startDate == null) return false
        if (endDate == null) return false
        if (startDate >= endDate) return false
        if (budget <= 0) return false
        if (budget < 200 || budget > 20000) return false
        return true
    }

    @Test
    fun `test validation with valid inputs returns true`() {
        val startDate = System.currentTimeMillis()
        val endDate = startDate + (7 * 24 * 60 * 60 * 1000)
        assertTrue(validateTripInputs("Poland", startDate, endDate, 5000))
    }

    @Test
    fun `test validation with empty destination returns false`() {
        val startDate = System.currentTimeMillis()
        val endDate = startDate + (7 * 24 * 60 * 60 * 1000)
        assertFalse(validateTripInputs("", startDate, endDate, 5000))
    }

    @Test
    fun `test validation with null start date returns false`() {
        val endDate = System.currentTimeMillis()
        assertFalse(validateTripInputs("Poland", null, endDate, 5000))
    }

    @Test
    fun `test validation with null end date returns false`() {
        val startDate = System.currentTimeMillis()
        assertFalse(validateTripInputs("Poland", startDate, null, 5000))
    }

    @Test
    fun `test validation with end date before start date returns false`() {
        val endDate = System.currentTimeMillis()
        val startDate = endDate + (7 * 24 * 60 * 60 * 1000)
        assertFalse(validateTripInputs("Poland", startDate, endDate, 5000))
    }

    @Test
    fun `test validation with budget zero returns false`() {
        val startDate = System.currentTimeMillis()
        val endDate = startDate + (7 * 24 * 60 * 60 * 1000)
        assertFalse(validateTripInputs("Poland", startDate, endDate, 0))
    }

    @Test
    fun `test validation with budget below minimum returns false`() {
        val startDate = System.currentTimeMillis()
        val endDate = startDate + (7 * 24 * 60 * 60 * 1000)
        assertFalse(validateTripInputs("Poland", startDate, endDate, 100))
    }

    @Test
    fun `test validation with budget above maximum returns false`() {
        val startDate = System.currentTimeMillis()
        val endDate = startDate + (7 * 24 * 60 * 60 * 1000)
        assertFalse(validateTripInputs("Poland", startDate, endDate, 25000))
    }

    @Test
    fun `test validation with budget at minimum boundary returns true`() {
        val startDate = System.currentTimeMillis()
        val endDate = startDate + (7 * 24 * 60 * 60 * 1000)
        assertTrue(validateTripInputs("Poland", startDate, endDate, 200))
    }

    @Test
    fun `test validation with budget at maximum boundary returns true`() {
        val startDate = System.currentTimeMillis()
        val endDate = startDate + (7 * 24 * 60 * 60 * 1000)
        assertTrue(validateTripInputs("Poland", startDate, endDate, 20000))
    }
}
