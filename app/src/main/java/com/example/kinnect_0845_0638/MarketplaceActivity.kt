package com.example.kinnect_0845_0638

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MarketplaceActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var gridMarketItems: GridLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_marketplace)

        dbHelper = DatabaseHelper.getInstance(this)

        val mainLayout = findViewById<View>(R.id.mainRootLayout)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        gridMarketItems = findViewById(R.id.gridMarketItems)

        val btnHome = findViewById<ImageButton>(R.id.home)
        val btnFriends = findViewById<ImageButton>(R.id.friends)
        val btnSell = findViewById<LinearLayout>(R.id.btnSell)

        // Navigate to Home Feed
        btnHome?.setOnClickListener {
            val intent = Intent(this, HomeFeedActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }

        // Navigate to Friends
        btnFriends?.setOnClickListener {
            val intent = Intent(this, FriendsActivity::class.java)
            startActivity(intent)
        }

        // Open Sell Item Screen
        btnSell?.setOnClickListener {
            val intent = Intent(this, CreateMarketItemActivity::class.java)
            startActivity(intent)
        }

        // Fetch Live Listings from SQLite
        loadMarketplaceItems()
    }

    override fun onResume() {
        super.onResume()
        loadMarketplaceItems()
    }

    private fun loadMarketplaceItems() {
        gridMarketItems.removeAllViews()
        val items = dbHelper.getAllMarketItems()

        for (item in items) {
            addMarketplaceCard(item.title, item.price, item.location, item.image)
        }
    }

    private fun addMarketplaceCard(
        title: String,
        price: String,
        location: String,
        imageBase64: String
    ) {
        val card = RelativeLayout(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(6.dpToPx(), 6.dpToPx(), 6.dpToPx(), 6.dpToPx())
            }
            setBackgroundResource(R.drawable.bg_rounded_card)
            setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
        }

        // Image View
        val ivItem = ImageView(this).apply {
            id = View.generateViewId()
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                140.dpToPx()
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageResource(R.drawable.default_gallery)

            if (imageBase64.isNotEmpty()) {
                try {
                    val decodedBytes = Base64.decode(imageBase64, Base64.NO_WRAP)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    if (bitmap != null) setImageBitmap(bitmap)
                } catch (_: Exception) {}
            }
        }
        card.addView(ivItem)

        // Price Text
        val tvPrice = TextView(this).apply {
            id = View.generateViewId()
            val params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.BELOW, ivItem.id)
                setMargins(0, 4.dpToPx(), 0, 0)
            }
            layoutParams = params
            text = if (price.startsWith("Rs")) price else "Rs. $price"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
        }
        card.addView(tvPrice)

        // Title Text
        val tvTitle = TextView(this).apply {
            id = View.generateViewId()
            val params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.BELOW, tvPrice.id)
            }
            layoutParams = params
            text = title
            textSize = 13f
            setTextColor(Color.BLACK)
        }
        card.addView(tvTitle)

        // Location Text
        val tvLocation = TextView(this).apply {
            val params = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.BELOW, tvTitle.id)
            }
            layoutParams = params
            text = location
            textSize = 12f
            setTextColor(Color.parseColor("#65676B"))
        }
        card.addView(tvLocation)

        gridMarketItems.addView(card)
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}