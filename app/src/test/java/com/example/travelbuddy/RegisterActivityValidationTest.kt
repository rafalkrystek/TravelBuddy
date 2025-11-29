package com.example.travelbuddy

import org.junit.Test
import org.junit.Assert.*

class RegisterActivityValidationTest {
    private fun validateInputs(firstName: String, lastName: String, email: String, password: String, confirmPassword: String, termsAccepted: Boolean): Boolean {
        if (firstName.isEmpty()) return false
        if (lastName.isEmpty()) return false
        if (email.isEmpty()) return false
        val emailPattern = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        if (!email.matches(emailPattern.toRegex())) return false
        if (password.isEmpty()) return false
        if (password.length < 6) return false
        if (confirmPassword.isEmpty()) return false
        if (password != confirmPassword) return false
        if (!termsAccepted) return false
        return true
    }

    @Test
    fun `test validation with valid inputs returns true`() {
        assertTrue(validateInputs("John", "Doe", "john.doe@example.com", "password123", "password123", true))
    }

    @Test
    fun `test validation with empty first name returns false`() {
        assertFalse(validateInputs("", "Doe", "john.doe@example.com", "password123", "password123", true))
    }

    @Test
    fun `test validation with empty last name returns false`() {
        assertFalse(validateInputs("John", "", "john.doe@example.com", "password123", "password123", true))
    }

    @Test
    fun `test validation with empty email returns false`() {
        assertFalse(validateInputs("John", "Doe", "", "password123", "password123", true))
    }

    @Test
    fun `test validation with invalid email format returns false`() {
        assertFalse(validateInputs("John", "Doe", "invalid-email", "password123", "password123", true))
    }

    @Test
    fun `test validation with empty password returns false`() {
        assertFalse(validateInputs("John", "Doe", "john.doe@example.com", "", "", true))
    }

    @Test
    fun `test validation with password shorter than 6 characters returns false`() {
        assertFalse(validateInputs("John", "Doe", "john.doe@example.com", "12345", "12345", true))
    }

    @Test
    fun `test validation with password exactly 6 characters returns true`() {
        assertTrue(validateInputs("John", "Doe", "john.doe@example.com", "123456", "123456", true))
    }

    @Test
    fun `test validation with mismatched passwords returns false`() {
        assertFalse(validateInputs("John", "Doe", "john.doe@example.com", "password123", "different123", true))
    }

    @Test
    fun `test validation with terms not accepted returns false`() {
        assertFalse(validateInputs("John", "Doe", "john.doe@example.com", "password123", "password123", false))
    }

    @Test
    fun `test validation with various valid email formats`() {
        listOf("test@example.com", "user.name@example.co.uk", "user+tag@example.com", "user123@example-domain.com")
            .forEach { email ->
                assertTrue(validateInputs("John", "Doe", email, "password123", "password123", true))
            }
    }
}
