package com.example.travelbuddy

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.travelbuddy.helpers.FirebaseErrorHelper
import com.example.travelbuddy.helpers.ValidationHelper
import com.example.travelbuddy.helpers.setupBackButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : BaseActivity() {
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        setupBackButton()
        findViewById<Button>(R.id.signInButton).setOnClickListener { performLogin() }
        findViewById<TextView>(R.id.signUpText).setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        findViewById<TextView>(R.id.forgotPasswordText).setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun performLogin() {
        val email = emailEditText.text?.toString()?.trim() ?: ""
        val password = passwordEditText.text?.toString()?.trim() ?: ""
        
        if (!ValidationHelper.validateEmail(email, emailEditText, this)) return
        if (password.isEmpty()) {
            passwordEditText.error = getString(R.string.password_required)
            return
        }
        
        PasswordEncryption.getEncryptedSharedPreferences(this).edit().putString("last_login_email", email).apply()
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            } else {
                Toast.makeText(this, FirebaseErrorHelper.getLoginErrorMessage(
                    FirebaseErrorHelper.getErrorCode(task.exception), this), Toast.LENGTH_LONG).show()
            }
        }
    }
}
