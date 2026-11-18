package com.example.kinnect_0845_0638

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class EditProfileActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var ivProfilePicture: ImageView
    private lateinit var tvInitialsAvatar: TextView
    private lateinit var storyimg: ImageView

    private var selectedProfileUri: Uri? = null
    private var selectedCoverUri: Uri? = null

    // Image Pickers
    private val pickProfileImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedProfileUri = uri
            ivProfilePicture.setImageURI(uri)
            ivProfilePicture.visibility = View.VISIBLE
            tvInitialsAvatar.visibility = View.GONE
        }
    }

    private val pickCoverImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedCoverUri = uri
            storyimg.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.editprofile_activity)

        dbHelper = DatabaseHelper.getInstance(this)

        // Handle System Window Insets for Status and Navigation Bar Padding
        val mainLayout = findViewById<View>(R.id.mainRootLayout)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    systemBars.bottom
                )
                insets
            }
        }

        // Bind Views
        val backButton = findViewById<TextView>(R.id.backButton)
        val btnSave = findViewById<TextView>(R.id.btnSave)
        val btnEditProfilePic = findViewById<TextView>(R.id.btnEditProfilePic)
        val btnEditCoverPhoto = findViewById<TextView>(R.id.btnEditCoverPhoto)
        val btnCoverCamera = findViewById<ImageButton>(R.id.btnCoverCamera)

        ivProfilePicture = findViewById(R.id.ivProfilePicture)
        tvInitialsAvatar = findViewById(R.id.tvInitialsAvatar)
        storyimg = findViewById(R.id.storyimg)

        val etName = findViewById<EditText>(R.id.etName)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etWebsite = findViewById<EditText>(R.id.etWebsite)
        val etBio = findViewById<EditText>(R.id.etBio)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etGender = findViewById<EditText>(R.id.etGender)

        // 1. Cancel / Back Action
        backButton?.setOnClickListener { finish() }

        // 2. Image Pickers
        btnEditProfilePic?.setOnClickListener {
            pickProfileImageLauncher.launch("image/*")
        }

        btnEditCoverPhoto?.setOnClickListener {
            pickCoverImageLauncher.launch("image/*")
        }

        btnCoverCamera?.setOnClickListener {
            pickCoverImageLauncher.launch("image/*")
        }

        // Fetch existing database user profile
        loadUserProfile(etName, etUsername, etWebsite, etBio, etEmail, etPhone, etGender)

        // 3. Save Action
        btnSave?.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val fullName = etName.text.toString().trim()
            val website = etWebsite.text.toString().trim()
            val bio = etBio.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val gender = etGender.text.toString().trim()

            if (username.isEmpty()) {
                etUsername.error = "Username is required"
                etUsername.requestFocus()
                return@setOnClickListener
            }

            val uid = SessionManager.getCurrentUserUid(this)
            if (uid != null) {
                val nameParts = fullName.split(" ")
                val firstName = if (nameParts.isNotEmpty()) nameParts[0] else ""
                val lastName = if (nameParts.size > 1) nameParts.subList(1, nameParts.size).joinToString(" ") else ""

                val updates = mutableMapOf<String, Any>(
                    "username" to username,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "website" to website,
                    "bio" to bio,
                    "email" to email,
                    "phone" to phone,
                    "gender" to gender
                )

                if (selectedProfileUri != null) {
                    updates["profilePicUrl"] = selectedProfileUri.toString()
                }

                if (selectedCoverUri != null) {
                    updates["coverPhotoUrl"] = selectedCoverUri.toString()
                }

                val success = dbHelper.updateUserProfile(uid, updates)
                if (success) {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this, HomeFeedActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to update profile", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadUserProfile(
        etName: EditText,
        etUsername: EditText,
        etWebsite: EditText,
        etBio: EditText,
        etEmail: EditText,
        etPhone: EditText,
        etGender: EditText
    ) {
        val uid = SessionManager.getCurrentUserUid(this) ?: return
        val user = dbHelper.getUserByUid(uid)

        if (user != null) {
            val firstName = user.firstName
            val lastName = user.lastName
            val username = user.username
            val website = user.website
            val bio = user.bio
            val email = user.email
            val phone = user.phone
            val gender = user.gender

            val profilePicUrl = user.profilePicUrl
            val coverPhotoUrl = user.coverPhotoUrl

            etName.setText("$firstName $lastName".trim())
            etUsername.setText(username)
            etWebsite.setText(website)
            etBio.setText(bio)
            etEmail.setText(email)
            etPhone.setText(phone)
            etGender.setText(gender)

            // Load existing profile pic / initials
            if (profilePicUrl.isNotEmpty()) {
                try {
                    ivProfilePicture.setImageURI(Uri.parse(profilePicUrl))
                    ivProfilePicture.visibility = View.VISIBLE
                    tvInitialsAvatar.visibility = View.GONE
                } catch (e: Exception) {
                    setInitials(firstName, lastName)
                }
            } else {
                setInitials(firstName, lastName)
            }

            // Load existing cover photo
            if (coverPhotoUrl.isNotEmpty()) {
                try {
                    storyimg.setImageURI(Uri.parse(coverPhotoUrl))
                } catch (_: Exception) {}
            }
        }
    }

    private fun setInitials(firstName: String, lastName: String) {
        if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
            tvInitialsAvatar.text = "${firstName[0]}${lastName[0]}".uppercase()
        } else if (firstName.isNotEmpty()) {
            tvInitialsAvatar.text = firstName[0].toString().uppercase()
        } else {
            tvInitialsAvatar.text = "K"
        }
        tvInitialsAvatar.visibility = View.VISIBLE
        ivProfilePicture.visibility = View.GONE
    }
}