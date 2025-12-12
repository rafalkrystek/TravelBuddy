package com.example.travelbuddy

import android.os.Bundle
import android.util.Patterns
import android.widget.ImageButton
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException

class ForgotPasswordActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        
        val emailEdit = findViewById<TextInputEditText>(R.id.emailEditText)
        
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { 
            finish() 
        }
        
        findViewById<android.widget.Button>(R.id.resetPasswordButton).setOnClickListener {
            val email = emailEdit.text.toString().trim()
            
            if (email.isEmpty()) {
                emailEdit.error = getString(R.string.email_required)
                return@setOnClickListener
            }
            
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEdit.error = getString(R.string.invalid_email)
                return@setOnClickListener
            }
            
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, getString(R.string.reset_email_sent), Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val errorCode = (task.exception as? FirebaseAuthException)?.errorCode
                        val msg = when (errorCode) {
                            "ERROR_INVALID_EMAIL" -> getString(R.string.error_invalid_email)
                            "ERROR_USER_NOT_FOUND" -> getString(R.string.error_user_not_found)
                            "ERROR_NETWORK_REQUEST_FAILED" -> getString(R.string.error_network)
                            else -> "Błąd: ${task.exception?.localizedMessage}"
                        }
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
