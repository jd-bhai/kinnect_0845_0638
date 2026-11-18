package com.example.kinnect_0845_0638

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

class SignupActivity : AppCompatActivity() {

    private var isPasswordVisible = false
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_activity)

        dbHelper = DatabaseHelper.getInstance(this)

        // Bind Views
        val backButton = findViewById<ImageButton>(R.id.backButton)
        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etBirthDay = findViewById<EditText>(R.id.etBirthDay)
        val etBirthMonth = findViewById<EditText>(R.id.etBirthMonth)
        val etBirthYear = findViewById<EditText>(R.id.etBirthYear)
        val rgGender = findViewById<RadioGroup>(R.id.rgGender)
        val etSignupEmail = findViewById<EditText>(R.id.etSignupEmail)
        val etSignupPassword = findViewById<EditText>(R.id.etSignupPassword)
        val btnToggleSignupPassword = findViewById<ImageButton>(R.id.btnToggleSignupPassword)
        val cbTerms = findViewById<CheckBox>(R.id.cbTerms)
        val createAccountButton = findViewById<TextView>(R.id.createAccountButton)
        val loginText = findViewById<TextView>(R.id.loginText)

        // Password Strength Indicators
        val strengthBar1 = findViewById<View>(R.id.strengthBar1)
        val strengthBar2 = findViewById<View>(R.id.strengthBar2)
        val strengthBar3 = findViewById<View>(R.id.strengthBar3)
        val tvStrengthLabel = findViewById<TextView>(R.id.tvStrengthLabel)

        // 1. Back Navigation
        backButton.setOnClickListener { finish() }
        loginText.setOnClickListener { finish() }

        // 2. Password Visibility Toggle
        btnToggleSignupPassword.setOnClickListener {
            if (isPasswordVisible) {
                etSignupPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            } else {
                etSignupPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            }
            isPasswordVisible = !isPasswordVisible
            etSignupPassword.setSelection(etSignupPassword.text.length)
        }

        // 3. Dynamic Password Strength Evaluation
        etSignupPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s.toString()
                updatePasswordStrengthUI(password, strengthBar1, strengthBar2, strengthBar3, tvStrengthLabel)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 4. Create Account Click
        createAccountButton.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val day = etBirthDay.text.toString().trim()
            val month = etBirthMonth.text.toString().trim()
            val year = etBirthYear.text.toString().trim()
            val email = etSignupEmail.text.toString().trim()
            val password = etSignupPassword.text.toString().trim()

            val selectedGenderId = rgGender.checkedRadioButtonId
            val gender = if (selectedGenderId != -1) {
                findViewById<RadioButton>(selectedGenderId).text.toString()
            } else "Male"

            if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!cbTerms.isChecked) {
                Toast.makeText(this, "Please accept Terms and Policy", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if user already exists
            val existingUser = dbHelper.getUserByEmail(email)
            if (existingUser != null) {
                Toast.makeText(this, "Email is already registered!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val uid = UUID.randomUUID().toString()
            val birthday = "$day-$month-$year"

            val newUser = UserData(
                uid = uid,
                firstName = firstName,
                lastName = lastName,
                birthday = birthday,
                gender = gender,
                email = email,
                password = password,
                username = "",
                bio = "",
                profilePicUrl = "",
                isOnline = true
            )

            val success = dbHelper.insertUser(newUser)
            if (success) {
                SessionManager.setCurrentUserUid(this, uid)
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, EditProfileActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Failed to create account in database", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updatePasswordStrengthUI(
        pass: String,
        bar1: View,
        bar2: View,
        bar3: View,
        label: TextView
    ) {
        val gray = Color.parseColor("#D1D5DB")
        val red = Color.parseColor("#D93025")
        val orange = Color.parseColor("#F2994A")
        val green = Color.parseColor("#27AE60")

        when {
            pass.isEmpty() -> {
                bar1.setBackgroundColor(gray)
                bar2.setBackgroundColor(gray)
                bar3.setBackgroundColor(gray)
                label.text = "Weak"
                label.setTextColor(gray)
            }
            pass.length < 6 -> {
                bar1.setBackgroundColor(red)
                bar2.setBackgroundColor(gray)
                bar3.setBackgroundColor(gray)
                label.text = "Weak"
                label.setTextColor(red)
            }
            pass.length in 6..9 -> {
                bar1.setBackgroundColor(orange)
                bar2.setBackgroundColor(orange)
                bar3.setBackgroundColor(gray)
                label.text = "Medium"
                label.setTextColor(orange)
            }
            else -> {
                bar1.setBackgroundColor(green)
                bar2.setBackgroundColor(green)
                bar3.setBackgroundColor(green)
                label.text = "Strong"
                label.setTextColor(green)
            }
        }
    }
}