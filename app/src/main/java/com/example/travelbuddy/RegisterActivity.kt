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
            firstNameEditText.error = "Required"
            return
        }
        if (lastName.isEmpty()) {
            lastNameEditText.error = "Required"
            return
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Invalid email"
            return
        }
        if (password.isEmpty() || password.length < 6) {
            passwordEditText.error = "Min 6 characters"
            return
        }
        val passwordStrength = PasswordEncryption.validatePasswordStrength(password)
        if (passwordStrength == PasswordEncryption.PasswordStrength.WEAK) {
            passwordEditText.error = "Password too weak. Use 8+ chars with numbers and letters"
            return
        }
        if (password != confirmPassword) {
            confirmPasswordEditText.error = "Passwords don't match"
            return
        }
        if (!termsCheckBox.isChecked) {
            Toast.makeText(this, "Accept terms", Toast.LENGTH_SHORT).show()
            return
        }
        val encryptedPrefs = PasswordEncryption.getEncryptedSharedPreferences(this)
        encryptedPrefs.edit().putString("registered_email", email).apply()
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                auth.currentUser?.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName("$firstName $lastName").build()
                )
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                val errorCode = (task.exception as? FirebaseAuthException)?.errorCode
                val msg = when (errorCode) {
                    "ERROR_WEAK_PASSWORD" -> "Password too weak"
                    "ERROR_INVALID_EMAIL" -> "Invalid email"
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "Email already in use"
                    "ERROR_NETWORK_REQUEST_FAILED" -> "Network error"
                    else -> "Registration failed"
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
