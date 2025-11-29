package com.example.travelbuddy

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

class LoginActivity : BaseActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth = FirebaseAuth.getInstance()
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        findViewById<android.widget.ImageButton>(R.id.backButton).setOnClickListener { finish() }
        findViewById<Button>(R.id.signInButton).setOnClickListener { performLogin() }
        findViewById<TextView>(R.id.signUpText).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        findViewById<TextView>(R.id.forgotPasswordText).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun performLogin() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Invalid email"
            return
        }
        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            return
        }
        val encryptedPrefs = PasswordEncryption.getEncryptedSharedPreferences(this)
        encryptedPrefs.edit().putString("last_login_email", email).apply()
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            } else {
                val errorCode = (task.exception as? FirebaseAuthException)?.errorCode
                val msg = when (errorCode) {
                    "ERROR_INVALID_EMAIL" -> "Invalid email"
                    "ERROR_USER_NOT_FOUND" -> "No account found"
                    "ERROR_WRONG_PASSWORD" -> "Incorrect password"
                    "ERROR_USER_DISABLED" -> "Account disabled"
                    "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts"
                    "ERROR_NETWORK_REQUEST_FAILED" -> "Network error"
                    else -> "Login failed"
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
