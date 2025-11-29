package com.example.travelbuddy

import android.os.Bundle
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        val emailEdit = findViewById<TextInputEditText>(R.id.emailEditText)
        findViewById<android.widget.Button>(R.id.resetPasswordButton).setOnClickListener {
            val email = emailEdit.text.toString().trim()
            if (email.isEmpty()) {
                emailEdit.error = "Email is required"
                return@setOnClickListener
            }
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Email wysłany", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Błąd: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}
