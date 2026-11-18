package com.example.kinnect_0845_0638

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class YourStoryActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var storyMedia: ImageView
    private lateinit var avatar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_your_story)

        dbHelper = DatabaseHelper.getInstance(this)

        storyMedia = findViewById(R.id.storyMedia)
        avatar = findViewById(R.id.avatar)

        findViewById<ImageButton>(R.id.closeButton)?.setOnClickListener { finish() }

        loadLoggedUserInitials()

        val storyId = intent.getStringExtra("STORY_ID")
        if (!storyId.isNullOrEmpty()) {
            loadMyStory(storyId)
        } else {
            fetchLatestUserStory()
        }
    }

    private fun loadLoggedUserInitials() {
        val uid = SessionManager.getCurrentUserUid(this) ?: return
        val user = dbHelper.getUserByUid(uid)
        if (user != null) {
            avatar.text = UserHelper.getUserInitials(user.firstName, user.lastName)
        }
    }

    private fun fetchLatestUserStory() {
        val uid = SessionManager.getCurrentUserUid(this) ?: return
        val story = dbHelper.getLatestUserStory(uid)
        if (story != null && story.mediaUri.isNotEmpty()) {
            renderMedia(story.mediaUri)
        }
    }

    private fun loadMyStory(storyId: String) {
        val story = dbHelper.getStoryById(storyId)
        if (story != null && story.mediaUri.isNotEmpty()) {
            renderMedia(story.mediaUri)
        } else {
            Toast.makeText(this, "Failed to load story", Toast.LENGTH_SHORT).show()
        }
    }

    private fun renderMedia(mediaUriStr: String) {
        Thread {
            var loaded = false
            if (!mediaUriStr.startsWith("content://") && !mediaUriStr.startsWith("http")) {
                try {
                    val cleanBase64 = mediaUriStr.replace("\n", "").replace("\r", "").trim()
                    val decodedBytes = Base64.decode(cleanBase64, Base64.NO_WRAP)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (bitmap != null) {
                        runOnUiThread { storyMedia.setImageBitmap(bitmap) }
                        loaded = true
                    }
                } catch (_: Exception) {}
            }

            if (!loaded) {
                try {
                    val uri = Uri.parse(mediaUriStr)
                    val inputStream = contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                    if (bitmap != null) {
                        runOnUiThread { storyMedia.setImageBitmap(bitmap) }
                    }
                } catch (_: Throwable) {}
            }
        }.start()
    }
}