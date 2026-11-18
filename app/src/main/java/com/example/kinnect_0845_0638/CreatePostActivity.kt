package com.example.kinnect_0845_0638

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.ByteArrayOutputStream

class CreatePostActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var etCaption: EditText
    private lateinit var ivSelectedImagePreview: ImageView
    private lateinit var layoutImagePreview: RelativeLayout

    private var selectedImageUri: Uri? = null

    // Image Picker Launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            ivSelectedImagePreview.setImageURI(uri)
            layoutImagePreview.visibility = View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.createpost_activity)

        dbHelper = DatabaseHelper.getInstance(this)

        // Window Insets Padding for Status/Nav bars
        val mainLayout = findViewById<View>(R.id.mainRootLayout)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        val closeButton = findViewById<ImageView>(R.id.closeButton)
        val postButton = findViewById<TextView>(R.id.postButton)
        val tvUserInitials = findViewById<TextView>(R.id.tvUserInitials)
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val btnPhotoVideo = findViewById<LinearLayout>(R.id.btnPhotoVideo)
        val btnCameraOption = findViewById<LinearLayout>(R.id.btnCameraOption)
        val btnRemoveImage = findViewById<ImageButton>(R.id.btnRemoveImage)

        etCaption = findViewById(R.id.etCaption)
        ivSelectedImagePreview = findViewById(R.id.ivSelectedImagePreview)
        layoutImagePreview = findViewById(R.id.layoutImagePreview)

        closeButton?.setOnClickListener { finish() }

        // Load User Info for Post Header
        loadUserProfile(tvUserInitials, tvUserName)

        // Select Photo Handlers
        btnPhotoVideo?.setOnClickListener { pickImageLauncher.launch("image/*") }
        btnCameraOption?.setOnClickListener { pickImageLauncher.launch("image/*") }

        btnRemoveImage?.setOnClickListener {
            selectedImageUri = null
            ivSelectedImagePreview.setImageURI(null)
            layoutImagePreview.visibility = View.GONE
        }

        // Publish Post Handler
        postButton?.setOnClickListener {
            val caption = etCaption.text.toString().trim()

            if (caption.isEmpty() && selectedImageUri == null) {
                Toast.makeText(this, "Please enter a caption or pick an image", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            publishPostToSQLite(caption)
        }
    }

    private fun loadUserProfile(tvInitials: TextView?, tvName: TextView?) {
        val uid = SessionManager.getCurrentUserUid(this) ?: return
        val user = dbHelper.getUserByUid(uid)

        if (user != null) {
            val firstName = user.firstName
            val lastName = user.lastName
            val fullName = "$firstName $lastName".trim()

            val initials = UserHelper.getUserInitials(firstName, lastName)

            tvInitials?.text = initials
            tvName?.text = if (fullName.isNotEmpty()) fullName else "User"
        }
    }

    private fun publishPostToSQLite(caption: String) {
        val uid = SessionManager.getCurrentUserUid(this) ?: return

        val base64ImageString = if (selectedImageUri != null) {
            bitmapToBase64(selectedImageUri!!)
        } else {
            ""
        }

        dbHelper.createPost(uid, caption, base64ImageString)
        Toast.makeText(this, "Post published!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun bitmapToBase64(uri: Uri): String {
        return try {
            val bitmap = contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: return ""

            val maxSize = 512
            val scale = minOf(
                1.0,
                maxSize.toDouble() / bitmap.width,
                maxSize.toDouble() / bitmap.height
            )

            val resizedBitmap = if (scale < 1.0) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt().coerceAtLeast(1),
                    (bitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }
}