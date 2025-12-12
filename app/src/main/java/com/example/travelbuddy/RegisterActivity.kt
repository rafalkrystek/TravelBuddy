package com.example.travelbuddy

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.Toast
import com.example.travelbuddy.helpers.FirebaseErrorHelper
import com.example.travelbuddy.helpers.ValidationHelper
import com.example.travelbuddy.helpers.setupBackButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterActivity : BaseActivity() {
    private lateinit var firstNameEditText: TextInputEditText
    private lateinit var lastNameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var termsCheckBox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        firstNameEditText = findViewById(R.id.firstNameEditText)
        lastNameEditText = findViewById(R.id.lastNameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        termsCheckBox = findViewById(R.id.termsCheckBox)
        setupBackButton()
        findViewById<android.widget.Button>(R.id.registerButton).setOnClickListener { performRegistration() }
        findViewById<android.widget.TextView>(R.id.signInText).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun performRegistration() {
        val firstName = firstNameEditText.text?.toString()?.trim() ?: ""
        val lastName = lastNameEditText.text?.toString()?.trim() ?: ""
        val email = emailEditText.text?.toString()?.trim() ?: ""
        val password = passwordEditText.text?.toString()?.trim() ?: ""
        val confirmPassword = confirmPasswordEditText.text?.toString()?.trim() ?: ""

        if (firstName.isEmpty()) {
            firstNameEditText.error = getString(R.string.first_name_required)
            return
        }
        if (lastName.isEmpty()) {
            lastNameEditText.error = getString(R.string.last_name_required)
            return
        }
        if (!ValidationHelper.validateEmail(email, emailEditText, this)) return
        if (!ValidationHelper.validatePassword(password, passwordEditText, this)) return
        val passwordStrength = PasswordEncryption.validatePasswordStrength(password)
        if (passwordStrength == PasswordEncryption.PasswordStrength.WEAK) {
            passwordEditText.error = getString(R.string.password_too_weak)
            return
        }
        if (password != confirmPassword) {
            confirmPasswordEditText.error = getString(R.string.passwords_dont_match)
            return
        }
        if (!termsCheckBox.isChecked) {
            Toast.makeText(this, getString(R.string.accept_terms), Toast.LENGTH_SHORT).show()
            return
        }
        PasswordEncryption.getEncryptedSharedPreferences(this).edit().putString("registered_email", email).apply()
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                FirebaseAuth.getInstance().currentUser?.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName("$firstName $lastName").build()
                )
                Toast.makeText(this, getString(R.string.registration_successful), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, FirebaseErrorHelper.getRegistrationErrorMessage(
                    FirebaseErrorHelper.getErrorCode(task.exception), this), Toast.LENGTH_LONG).show()
            }
        }
    }
}
