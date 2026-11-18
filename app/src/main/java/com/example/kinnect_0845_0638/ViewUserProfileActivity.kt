package com.example.kinnect_0845_0638

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ViewUserProfileActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var tvFullName: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvGender: TextView
    private lateinit var tvWebsite: TextView
    private lateinit var tvInitialsAvatar: TextView
    private lateinit var ivProfilePicture: ImageView
    private lateinit var ivCoverPhoto: ImageView
    private lateinit var btnAddFriend: TextView
    private lateinit var btnMessage: TextView

    private var targetUid: String = ""
    private var targetFullName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_user_profile)

        dbHelper = DatabaseHelper.getInstance(this)

        val mainLayout = findViewById<View>(R.id.mainRootLayout)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        targetUid = intent.getStringExtra("TARGET_UID") ?: ""

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        tvFullName = findViewById(R.id.tvFullName)
        tvUsername = findViewById(R.id.tvUsername)
        tvBio = findViewById(R.id.tvBio)
        tvEmail = findViewById(R.id.tvEmail)
        tvGender = findViewById(R.id.tvGender)
        tvWebsite = findViewById(R.id.tvWebsite)
        tvInitialsAvatar = findViewById(R.id.tvInitialsAvatar)
        ivProfilePicture = findViewById(R.id.ivProfilePicture)
        ivCoverPhoto = findViewById(R.id.ivCoverPhoto)
        btnAddFriend = findViewById(R.id.btnAddFriend)
        btnMessage = findViewById(R.id.btnMessage)

        btnBack?.setOnClickListener { finish() }

        // Check current friend or sent request status
        checkFriendAndRequestStatus()

        // Send Friend Request / Unfriend
        btnAddFriend?.setOnClickListener {
            val currentUid = SessionManager.getCurrentUserUid(this) ?: return@setOnClickListener
            if (targetUid.isEmpty()) return@setOnClickListener

            if (btnAddFriend.text == "Friends") {
                showUnfriendMenuPopup(btnAddFriend)
            } else if (btnAddFriend.text == "+ Add friend" || btnAddFriend.text == "Add Friend") {
                val success = dbHelper.sendFriendRequest(currentUid, targetUid)
                if (success) {
                    btnAddFriend.text = "Sent"
                    btnAddFriend.isEnabled = false
                    Toast.makeText(this, "Friend request sent!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to send request", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Open 1-on-1 Chat
        btnMessage?.setOnClickListener {
            if (targetUid.isNotEmpty()) {
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("RECEIVER_UID", targetUid)
                    putExtra("RECEIVER_NAME", if (targetFullName.isNotEmpty()) targetFullName else "User")
                }
                startActivity(intent)
            }
        }

        // Load Target User Profile
        if (targetUid.isNotEmpty()) {
            loadTargetUserProfile(targetUid)
        } else {
            Toast.makeText(this, "User ID not specified", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showUnfriendMenuPopup(anchorView: View) {
        val popupView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_rounded_card)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            setPadding(8.dpToPx(), 4.dpToPx(), 8.dpToPx(), 4.dpToPx())
            elevation = 16f
        }

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 16f
            isOutsideTouchable = true
        }

        val tvUnfriend = TextView(this).apply {
            text = "Unfriend"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#D93025"))
            setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())

            setOnClickListener {
                unfriendUser(targetUid)
                popupWindow.dismiss()
            }
        }

        popupView.addView(tvUnfriend)
        popupWindow.showAsDropDown(anchorView, 0, 4.dpToPx())
    }

    private fun unfriendUser(uid: String) {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return
        dbHelper.unfriendUser(currentUid, uid)
        Toast.makeText(this, "Unfriended user", Toast.LENGTH_SHORT).show()
        btnAddFriend.text = "+ Add friend"
        btnAddFriend.isEnabled = true
    }

    private fun checkFriendAndRequestStatus() {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return
        if (targetUid.isEmpty()) return

        val isFriend = dbHelper.isFriend(currentUid, targetUid)
        if (isFriend) {
            btnAddFriend.text = "Friends"
            btnAddFriend.isEnabled = true
        } else {
            val isSent = dbHelper.hasSentFriendRequest(currentUid, targetUid)
            if (isSent) {
                btnAddFriend.text = "Sent"
                btnAddFriend.isEnabled = false
            } else {
                btnAddFriend.text = "+ Add friend"
                btnAddFriend.isEnabled = true
            }
        }
    }

    private fun loadTargetUserProfile(uid: String) {
        val user = dbHelper.getUserByUid(uid)

        if (user != null) {
            val firstName = user.firstName
            val lastName = user.lastName
            val username = user.username
            val bio = user.bio
            val email = user.email
            val gender = user.gender
            val website = user.website
            val profilePicUrl = user.profilePicUrl
            val coverPhotoUrl = user.coverPhotoUrl

            targetFullName = "$firstName $lastName".trim()
            tvFullName.text = if (targetFullName.isNotEmpty()) targetFullName else "User"
            tvUsername.text = if (username.isNotEmpty()) "@$username" else "@user"
            tvBio.text = if (bio.isNotEmpty()) bio else "No bio added yet."
            tvEmail.text = if (email.isNotEmpty()) email else "Not set"
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
        } else {
            if (targetFullName.isEmpty()) {
                tvFullName.text = "User"
                tvInitialsAvatar.text = "U"
            }
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}