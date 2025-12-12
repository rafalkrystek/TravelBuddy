package com.example.travelbuddy

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.CheckBox
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firstNameEditText: TextInputEditText
    private lateinit var lastNameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var termsCheckBox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        auth = FirebaseAuth.getInstance()
        firstNameEditText = findViewById(R.id.firstNameEditText)
        lastNameEditText = findViewById(R.id.lastNameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        termsCheckBox = findViewById(R.id.termsCheckBox)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }
        findViewById<android.widget.Button>(R.id.registerButton).setOnClickListener { performRegistration() }
        findViewById<android.widget.TextView>(R.id.signInText).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun performRegistration() {
        val firstName = firstNameEditText.text.toString().trim()
        val lastName = lastNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        if (firstName.isEmpty()) {
            firstNameEditText.error = getString(R.string.first_name_required)
            return
        }
        if (lastName.isEmpty()) {
            lastNameEditText.error = getString(R.string.last_name_required)
            return
        }
        if (email.isEmpty()) {
            emailEditText.error = getString(R.string.email_required)
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = getString(R.string.invalid_email)
            return
        }
        if (password.isEmpty()) {
            passwordEditText.error = getString(R.string.password_required)
            return
        }
        if (password.length < 6) {
            passwordEditText.error = getString(R.string.password_min_length)
            return
        }
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
        val encryptedPrefs = PasswordEncryption.getEncryptedSharedPreferences(this)
        encryptedPrefs.edit().putString("registered_email", email).apply()
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                auth.currentUser?.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName("$firstName $lastName").build()
                )
                Toast.makeText(this, getString(R.string.registration_successful), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                val errorCode = (task.exception as? FirebaseAuthException)?.errorCode
                val msg = when (errorCode) {
                    "ERROR_WEAK_PASSWORD" -> getString(R.string.error_weak_password)
                    "ERROR_INVALID_EMAIL" -> getString(R.string.error_invalid_email)
                    "ERROR_EMAIL_ALREADY_IN_USE" -> getString(R.string.error_email_already_in_use)
                    "ERROR_NETWORK_REQUEST_FAILED" -> getString(R.string.error_network)
                    else -> getString(R.string.error_registration_failed)
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
