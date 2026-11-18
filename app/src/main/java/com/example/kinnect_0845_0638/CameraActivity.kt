package com.example.kinnect_0845_0638

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class CameraActivity : AppCompatActivity() {

    private lateinit var pictureImageView: ImageView
    private var selectedImageUri: Uri? = null

    // Photo picker launcher to pick an image from gallery
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            pictureImageView.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        pictureImageView = findViewById(R.id.picture)

        val closeButton = findViewById<ImageButton>(R.id.closeButton)
        val lightButton = findViewById<ImageButton>(R.id.lightButton)
        val flashButton = findViewById<ImageButton>(R.id.flashButton)
        val faceButton = findViewById<ImageButton>(R.id.faceButton)
        val shutterContainer = findViewById<FrameLayout>(R.id.shutterContainer)

        closeButton?.setOnClickListener {
            finish()
        }

        // Tap light or face button to select an image from gallery into the picture View
        lightButton?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        faceButton?.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        flashButton?.setOnClickListener {
            Toast.makeText(this, "Flash toggled", Toast.LENGTH_SHORT).show()
        }

        // Tapping shutter sends selected photo to StoryEditorActivity
        shutterContainer?.setOnClickListener {
            if (selectedImageUri != null) {
                val intent = Intent(this, StoryEditorActivity::class.java).apply {
                    putExtra("MEDIA_URI", selectedImageUri.toString())
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Tap the light icon to pick an image first", Toast.LENGTH_SHORT).show()
            }
        }
    }
}