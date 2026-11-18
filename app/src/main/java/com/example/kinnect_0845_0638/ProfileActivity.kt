package com.example.kinnect_0845_0638

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ProfileActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var tvFullName: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvWebsite: TextView
    private lateinit var tvInitialsAvatar: TextView
    private lateinit var ivProfilePicture: ImageView
    private lateinit var ivCoverPhoto: ImageView
    private lateinit var containerUserPosts: LinearLayout
    private lateinit var tvNoPosts: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        dbHelper = DatabaseHelper.getInstance(this)

        val mainLayout = findViewById<View>(R.id.mainRootLayout)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // Bind Views
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnEditProfile = findViewById<TextView>(R.id.btnEditProfile)

        tvFullName = findViewById(R.id.tvFullName)
        tvUsername = findViewById(R.id.tvUsername)
        tvBio = findViewById(R.id.tvBio)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvGender = findViewById(R.id.tvGender)
        tvWebsite = findViewById(R.id.tvWebsite)
        tvInitialsAvatar = findViewById(R.id.tvInitialsAvatar)
        ivProfilePicture = findViewById(R.id.ivProfilePicture)
        ivCoverPhoto = findViewById(R.id.ivCoverPhoto)
        containerUserPosts = findViewById(R.id.containerUserPosts)
        tvNoPosts = findViewById(R.id.tvNoPosts)

        // Navigation
        btnBack?.setOnClickListener { finish() }

        btnEditProfile?.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadCurrentProfileData()
        loadMyPostsFromSQLite()
    }

    private fun loadCurrentProfileData() {
        val uid = SessionManager.getCurrentUserUid(this) ?: return
        val user = dbHelper.getUserByUid(uid) ?: return

        val firstName = user.firstName
        val lastName = user.lastName
        val username = user.username
        val bio = user.bio
        val email = user.email
        val phone = user.phone
        val gender = user.gender
        val website = user.website
        val profilePicUrl = user.profilePicUrl
        val coverPhotoUrl = user.coverPhotoUrl

        val fullName = "$firstName $lastName".trim()
        tvFullName.text = if (fullName.isNotEmpty()) fullName else "User"
        tvUsername.text = if (username.isNotEmpty()) "@$username" else "@user"
        tvBio.text = if (bio.isNotEmpty()) bio else "No bio added yet."
        tvEmail.text = if (email.isNotEmpty()) email else "Not set"
        tvPhone.text = if (phone.isNotEmpty()) phone else "Not set"
        tvGender.text = if (gender.isNotEmpty()) gender else "Not set"
        tvWebsite.text = if (website.isNotEmpty()) website else "Not set"

        val initials = UserHelper.getUserInitials(firstName, lastName)
        tvInitialsAvatar.text = initials

        if (profilePicUrl.isNotEmpty()) {
            try {
                val uri = Uri.parse(profilePicUrl)
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    ivProfilePicture.setImageBitmap(bitmap)
                    ivProfilePicture.visibility = View.VISIBLE
                    tvInitialsAvatar.visibility = View.GONE
                } else {
                    tvInitialsAvatar.visibility = View.VISIBLE
                    ivProfilePicture.visibility = View.GONE
                }
            } catch (_: Throwable) {
                tvInitialsAvatar.visibility = View.VISIBLE
                ivProfilePicture.visibility = View.GONE
            }
        } else {
            tvInitialsAvatar.visibility = View.VISIBLE
            ivProfilePicture.visibility = View.GONE
        }

        if (coverPhotoUrl.isNotEmpty()) {
            try {
                val uri = Uri.parse(coverPhotoUrl)
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    ivCoverPhoto.setImageBitmap(bitmap)
                }
            } catch (_: Throwable) {}
        }
    }

    private fun loadMyPostsFromSQLite() {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return
        containerUserPosts.removeAllViews()

        val posts = dbHelper.getPostsByAuthor(currentUid)
        tvNoPosts.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE

        for (post in posts) {
            renderUserPostCard(post.caption, post.imageUri)
        }
    }

    private fun renderUserPostCard(captionText: String, imageUriStr: String) {
        val postCard = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 10.dpToPx()) }
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F9FAFB"))
            setPadding(12.dpToPx(), 12.dpToPx(), 12.dpToPx(), 12.dpToPx())
        }

        if (captionText.isNotEmpty()) {
            val tvCaption = TextView(this).apply {
                text = captionText
                textSize = 14f
                setTextColor(Color.parseColor("#1C1E21"))
            }
            postCard.addView(tvCaption)
        }

        if (imageUriStr.isNotEmpty()) {
            val ivPostImage = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    180.dpToPx()
                ).apply { setMargins(0, 6.dpToPx(), 0, 0) }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            postCard.addView(ivPostImage)

            Thread {
                var imageLoaded = false
                if (!imageUriStr.startsWith("content://") && !imageUriStr.startsWith("http")) {
                    try {
                        val cleanBase64 = imageUriStr.replace("\n", "").replace("\r", "").trim()
                        val decodedBytes = Base64.decode(cleanBase64, Base64.NO_WRAP)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        if (bitmap != null) {
                            runOnUiThread { ivPostImage.setImageBitmap(bitmap) }
                            imageLoaded = true
                        }
                    } catch (_: Exception) {}
                }

                if (!imageLoaded) {
                    try {
                        val uri = Uri.parse(imageUriStr)
                        val inputStream = contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                        if (bitmap != null) {
                            runOnUiThread { ivPostImage.setImageBitmap(bitmap) }
                        }
                    } catch (_: Throwable) {}
                }
            }.start()
        }

        containerUserPosts.addView(postCard, 0)
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}