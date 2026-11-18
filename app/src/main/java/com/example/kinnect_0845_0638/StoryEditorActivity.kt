package com.example.kinnect_0845_0638

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.io.InputStream

class StoryEditorActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var storyMediaPreview: ImageView
    private lateinit var captionInput: EditText
    private var selectedMediaUriStr: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story_editor)

        dbHelper = DatabaseHelper.getInstance(this)

        storyMediaPreview = findViewById(R.id.storyMediaPreview)
        captionInput = findViewById(R.id.captionInput)

        findViewById<ImageButton>(R.id.closeButton)?.setOnClickListener { finish() }

        // Receive captured photo URI from CameraActivity
        val mediaUriStr = intent.getStringExtra("MEDIA_URI")
        if (!mediaUriStr.isNullOrEmpty()) {
            selectedMediaUriStr = mediaUriStr
            loadCapturedImage(mediaUriStr)
        }

        val publishListener = android.view.View.OnClickListener {
            uploadStoryToSQLite()
        }

        findViewById<LinearLayout>(R.id.yourStory)?.setOnClickListener(publishListener)
        findViewById<LinearLayout>(R.id.closeFriends)?.setOnClickListener(publishListener)
        findViewById<ImageButton>(R.id.nextButton)?.setOnClickListener(publishListener)
    }

    private fun loadCapturedImage(uriStr: String) {
        try {
            val uri = Uri.parse(uriStr)
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                storyMediaPreview.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load preview image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadStoryToSQLite() {
        val uid = SessionManager.getCurrentUserUid(this)
        if (uid == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedMediaUriStr.isEmpty()) {
            Toast.makeText(this, "No picture to upload", Toast.LENGTH_SHORT).show()
            return
        }

        val captionText = captionInput.text.toString().trim()

        Thread {
            try {
                val uri = Uri.parse(selectedMediaUriStr)
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                val outputStream = ByteArrayOutputStream()
                bitmap?.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
                val base64Image = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)

                dbHelper.createStory(uid, captionText, base64Image)

                runOnUiThread {
                    Toast.makeText(this@StoryEditorActivity, "Story posted!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@StoryEditorActivity, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}