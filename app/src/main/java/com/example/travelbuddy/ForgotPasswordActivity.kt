package com.example.travelbuddy

import android.os.Bundle
import com.example.travelbuddy.helpers.FirebaseErrorHelper
import com.example.travelbuddy.helpers.ValidationHelper
import com.example.travelbuddy.helpers.setupBackButton
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        
        val emailEdit = findViewById<TextInputEditText>(R.id.emailEditText)
        
        setupBackButton()
        findViewById<android.widget.Button>(R.id.resetPasswordButton).setOnClickListener {
            val email = emailEdit.text?.toString()?.trim() ?: ""
            if (!ValidationHelper.validateEmail(email, emailEdit, this)) return@setOnClickListener
            
            FirebaseAuth.getInstance().sendPasswordResetEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, getString(R.string.reset_email_sent), Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this, FirebaseErrorHelper.getPasswordResetErrorMessage(
                        FirebaseErrorHelper.getErrorCode(task.exception), this,
                        "Błąd: ${task.exception?.localizedMessage}"), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
