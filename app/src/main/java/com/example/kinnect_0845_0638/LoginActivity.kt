package com.example.kinnect_0845_0638

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_activity)

        dbHelper = DatabaseHelper.getInstance(this)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<TextView>(R.id.loginButton)
        val createAccountBtn = findViewById<TextView>(R.id.createAccount)
        val btnTogglePassword = findViewById<ImageButton>(R.id.btnTogglePassword)

        val layoutRecentLogin = findViewById<LinearLayout>(R.id.layoutRecentLogin)
        val cardRecentLogin = findViewById<LinearLayout>(R.id.cardRecentLogin)
        val tvRecentInitials = findViewById<TextView>(R.id.jw)
        val tvRecentName = findViewById<TextView>(R.id.tvRecentName)

        // 1. Setup Recent Login Card from local storage
        val sharedPref = getSharedPreferences("KinnectPrefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPref.getString("recent_email", null)
        val savedName = sharedPref.getString("recent_name", null)
        val savedInitials = sharedPref.getString("recent_initials", null)

        if (!savedEmail.isNullOrEmpty()) {
            layoutRecentLogin.visibility = View.VISIBLE
            tvRecentName.text = savedName ?: "User"
            tvRecentInitials.text = savedInitials ?: "JW"
            cardRecentLogin.setOnClickListener {
                etEmail.setText(savedEmail)
                etPassword.requestFocus()
            }
        } else {
            layoutRecentLogin.visibility = View.GONE
        }

        // 2. Safe Password Visibility Toggle
        btnTogglePassword.setOnClickListener {
            if (isPasswordVisible) {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            } else {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            }
            isPasswordVisible = !isPasswordVisible

            val passLength = etPassword.text?.length ?: 0
            if (passLength > 0) {
                etPassword.setSelection(passLength)
            }
        }

        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        // 3. Login Button Click
        loginButton.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = dbHelper.getUserByEmail(email)
            if (user != null && user.password == password) {
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                SessionManager.setCurrentUserUid(this, user.uid)
                checkUserProfile(user.uid, email, user.firstName, user.lastName, user.username)
            } else {
                Toast.makeText(this, "Login Failed: Invalid email or password", Toast.LENGTH_LONG).show()
            }
        }

        // 4. Navigate to Sign Up
        createAccountBtn.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun checkUserProfile(
        uid: String,
        email: String,
        firstName: String,
        lastName: String,
        username: String
    ) {
        val initials = UserHelper.getUserInitials(firstName, lastName)

        // Save for Recent Login card next time
        val sharedPref = getSharedPreferences("KinnectPrefs", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putString("recent_email", email)
            putString(
                "recent_name",
                if ("$firstName $lastName".trim().isNotEmpty()) "$firstName $lastName".trim() else "User"
            )
            putString("recent_initials", initials)
            apply()
        }

        // Set online status in SQLite
        dbHelper.setOnlineStatus(uid, true)

        // Direct first-time users to profile setup if username is missing
        if (username.isEmpty()) {
            startActivity(Intent(this, EditProfileActivity::class.java))
        } else {
            startActivity(Intent(this, HomeFeedActivity::class.java))
        }
        finish()
    }
}