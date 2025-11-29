package com.example.travelbuddy

import org.junit.Test
import org.junit.Assert.*

class LoginActivityValidationTest {
    private fun validateLoginInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) return false
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        if (!email.matches(emailPattern.toRegex())) return false
        if (password.isEmpty()) return false
        return true
    }

    @Test
    fun `test validation with valid inputs returns true`() {
        val result = validateLoginInputs("test@example.com", "password123")
        assertTrue("Validation should pass with valid inputs", result)
    }

    @Test
    fun `test validation with empty email returns false`() {
        val result = validateLoginInputs("", "password123")
        assertFalse("Validation should fail with empty email", result)
    }

    @Test
    fun `test validation with empty password returns false`() {
        val result = validateLoginInputs("test@example.com", "")
        assertFalse("Validation should fail with empty password", result)
    }

    @Test
    fun `test validation with invalid email format returns false`() {
        val result = validateLoginInputs("invalid-email", "password123")
        assertFalse("Validation should fail with invalid email format", result)
    }

    @Test
    fun `test validation with various valid email formats`() {
        listOf("test@example.com", "user.name@example.co.uk", "user+tag@example.com", "user123@example-domain.com")
            .forEach { email ->
                assertTrue("Validation should pass with valid email: $email", validateLoginInputs(email, "password123"))
            }
    }
}
