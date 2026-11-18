package com.example.kinnect_0845_0638

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgotpassword_activity)

        dbHelper = DatabaseHelper.getInstance(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etResetEmail = findViewById<EditText>(R.id.etResetEmail)
        val btnSendResetEmail = findViewById<TextView>(R.id.btnSendResetEmail)
        val btnBackToLogin = findViewById<TextView>(R.id.btnBackToLogin)

        // Navigation
        btnBack.setOnClickListener { finish() }
        btnBackToLogin.setOnClickListener { finish() }

        // Send Reset Email
        btnSendResetEmail.setOnClickListener {
            val email = etResetEmail.text.toString().trim()

            if (email.isEmpty()) {
                etResetEmail.error = "Email is required"
                etResetEmail.requestFocus()
                return@setOnClickListener
            }

            val user = dbHelper.getUserByEmail(email)
            if (user != null) {
                Toast.makeText(
                    this,
                    "Password reset link sent to $email! Please check your inbox.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            } else {
                Toast.makeText(
                    this,
                    "No user account found with email $email",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}