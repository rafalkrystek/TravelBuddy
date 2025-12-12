package com.example.travelbuddy.helpers

import com.example.travelbuddy.R
import com.google.android.material.textfield.TextInputEditText

object ValidationHelper {
    fun validateEmail(email: String, emailEditText: TextInputEditText, context: android.content.Context): Boolean {
        return when {
            email.isEmpty() -> {
                emailEditText.error = context.getString(R.string.email_required)
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailEditText.error = context.getString(R.string.invalid_email)
                false
            }
            else -> true
        }
    }
    
    fun validatePassword(password: String, passwordEditText: TextInputEditText, context: android.content.Context): Boolean {
        return when {
            password.isEmpty() -> {
                passwordEditText.error = context.getString(R.string.password_required)
                false
            }
            password.length < 6 -> {
                passwordEditText.error = context.getString(R.string.password_min_length)
                false
            }
            else -> true
        }
    }
}

