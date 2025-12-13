package com.example.travelbuddy

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PasswordEncryption {
    fun getEncryptedSharedPreferences(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "encrypted_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun validatePasswordStrength(password: String): PasswordStrength {
        if (password.length < 8) return PasswordStrength.WEAK
        if (!password.any { it.isDigit() }) return PasswordStrength.MEDIUM
        if (!password.any { it.isUpperCase() }) return PasswordStrength.MEDIUM
        if (!password.any { it.isLowerCase() }) return PasswordStrength.MEDIUM
        if (!password.any { "!@#$%^&*()_+-=[]{}|;:,.<>?".contains(it) }) return PasswordStrength.MEDIUM
        return PasswordStrength.STRONG
    }

    enum class PasswordStrength {
        WEAK, MEDIUM, STRONG
    }
}

