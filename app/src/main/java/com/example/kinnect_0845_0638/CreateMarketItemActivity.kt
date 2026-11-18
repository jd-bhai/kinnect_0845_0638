package com.example.kinnect_0845_0638

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream

class CreateMarketItemActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var ivSelectedImage: ImageView
    private var selectedImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            ivSelectedImage.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.createmarketitem_activity)

        dbHelper = DatabaseHelper.getInstance(this)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val etTitle = findViewById<EditText>(R.id.etTitle)
        val etPrice = findViewById<EditText>(R.id.etPrice)
        val etLocation = findViewById<EditText>(R.id.etLocation)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val btnSelectImage = findViewById<Button>(R.id.btnSelectImage)
        val btnPublish = findViewById<Button>(R.id.btnPublish)
        ivSelectedImage = findViewById(R.id.ivSelectedImage)

        btnBack?.setOnClickListener { finish() }
        btnSelectImage?.setOnClickListener { pickImageLauncher.launch("image/*") }

        btnPublish?.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val price = etPrice.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val description = etDescription.text.toString().trim()

            if (title.isEmpty() || price.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "Please enter title, price, and location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            publishMarketItem(title, price, location, description)
        }
    }

    private fun publishMarketItem(
        title: String,
        price: String,
        location: String,
        description: String
    ) {
        val uid = SessionManager.getCurrentUserUid(this) ?: return

        val base64Image = if (selectedImageUri != null) {
            bitmapToBase64(selectedImageUri!!)
        } else {
            ""
        }

        dbHelper.createMarketItem(uid, title, price, location, description, base64Image)
        Toast.makeText(this, "Item listed successfully!", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun bitmapToBase64(uri: Uri): String {
        return try {
            val bitmap = contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            } ?: return ""

            val maxSize = 400
            val scale = minOf(1.0, maxSize.toDouble() / bitmap.width, maxSize.toDouble() / bitmap.height)
            val resized = if (scale < 1.0) {
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
            } else {
                bitmap
            }

            val baos = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 40, baos)
            Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }
}