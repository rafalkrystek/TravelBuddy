package com.example.travelbuddy.helpers

import android.content.Context
import com.google.android.material.textfield.TextInputEditText
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito.*

class ValidationHelperTest {
    
    @Test
    fun validateEmail_withValidEmail_returnsTrue() {
        // Given
        val email = "test@example.com"
        val emailEditText = mock(TextInputEditText::class.java)
        val context = mock(Context::class.java)
        
        // When
        val result = ValidationHelper.validateEmail(email, emailEditText, context)
        
        // Then
        assertTrue("Powinno zwrócić true dla prawidłowego emaila", result)
    }
    
    @Test
    fun validateEmail_withEmptyEmail_returnsFalse() {
        // Given
        val email = ""
        val emailEditText = mock(TextInputEditText::class.java)
        val context = mock(Context::class.java)
        `when`(context.getString(anyInt())).thenReturn("Email jest wymagany")
        
        // When
        val result = ValidationHelper.validateEmail(email, emailEditText, context)
        
        // Then
        assertFalse("Powinno zwrócić false dla pustego emaila", result)
    }
    
    @Test
    fun validateEmail_withInvalidEmail_returnsFalse() {
        // Given
        val email = "invalid-email"
        val emailEditText = mock(TextInputEditText::class.java)
        val context = mock(Context::class.java)
        `when`(context.getString(anyInt())).thenReturn("Nieprawidłowy email")
        
        // When
        val result = ValidationHelper.validateEmail(email, emailEditText, context)
        
        // Then
        assertFalse("Powinno zwrócić false dla nieprawidłowego emaila", result)
    }
    
    @Test
    fun validateEmail_withEmailWithoutAt_returnsFalse() {
        // Given
        val email = "testexample.com"
        val emailEditText = mock(TextInputEditText::class.java)
        val context = mock(Context::class.java)
        `when`(context.getString(anyInt())).thenReturn("Nieprawidłowy email")
        
        // When
        val result = ValidationHelper.validateEmail(email, emailEditText, context)
        
        // Then
        assertFalse("Powinno zwrócić false dla emaila bez @", result)
    }
    
    @Test
    fun validatePassword_withValidPassword_returnsTrue() {
        // Given
        val password = "password123"
        val passwordEditText = mock(TextInputEditText::class.java)
        val context = mock(Context::class.java)
        
        // When
        val result = ValidationHelper.validatePassword(password, passwordEditText, context)
        
        // Then
        assertTrue("Powinno zwrócić true dla hasła o długości >= 6", result)
    }
    
    @Test
    fun validatePassword_withEmptyPassword_returnsFalse() {
        // Given
        val password = ""
        val passwordEditText = mock(TextInputEditText::class.java)
        val context = mock(Context::class.java)
        `when`(context.getString(anyInt())).thenReturn("Hasło jest wymagane")
        
        // When
        val result = ValidationHelper.validatePassword(password, passwordEditText, context)
        
        // Then
        assertFalse("Powinno zwrócić false dla pustego hasła", result)
    }
    
    @Test
    fun validatePassword_withShortPassword_returnsFalse() {
        // Given
        val password = "12345" // 5 znaków
        val passwordEditText = mock(TextInputEditText::class.java)
        val context = mock(Context::class.java)
        `when`(context.getString(anyInt())).thenReturn("Hasło musi mieć minimum 6 znaków")
        
        // When
        val result = ValidationHelper.validatePassword(password, passwordEditText, context)
        
        // Then
        assertFalse("Powinno zwrócić false dla hasła < 6 znaków", result)
    }
    
    @Test
    fun validatePassword_withExactly6Characters_returnsTrue() {
        // Given
        val password = "123456" // dokładnie 6 znaków
        val passwordEditText = mock(TextInputEditText::class.java)
        val context = mock(Context::class.java)
        
        // When
        val result = ValidationHelper.validatePassword(password, passwordEditText, context)
        
        // Then
        assertTrue("Powinno zwrócić true dla hasła o długości 6", result)
    }
}

