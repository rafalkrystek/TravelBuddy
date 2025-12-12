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
        val encryptedPrefs = PasswordEncryption.getEncryptedSharedPreferences(this)
        encryptedPrefs.edit().putString("last_login_email", email).apply()
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show()
                val intent = Intent(this, DashboardActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                val errorCode = (task.exception as? FirebaseAuthException)?.errorCode
                val msg = when (errorCode) {
                    "ERROR_INVALID_EMAIL" -> getString(R.string.error_invalid_email)
                    "ERROR_USER_NOT_FOUND" -> getString(R.string.error_user_not_found)
                    "ERROR_WRONG_PASSWORD" -> getString(R.string.error_wrong_password)
                    "ERROR_USER_DISABLED" -> getString(R.string.error_user_disabled)
                    "ERROR_TOO_MANY_REQUESTS" -> getString(R.string.error_too_many_requests)
                    "ERROR_NETWORK_REQUEST_FAILED" -> getString(R.string.error_network)
                    "ERROR_INVALID_CREDENTIAL" -> getString(R.string.error_wrong_password)
                    else -> getString(R.string.error_login_failed)
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }
    }
}
