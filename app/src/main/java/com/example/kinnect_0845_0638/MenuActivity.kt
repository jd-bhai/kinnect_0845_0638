package com.example.kinnect_0845_0638

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MenuActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tvProfileName: TextView
    private lateinit var tvInitialsAvatar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        dbHelper = DatabaseHelper.getInstance(this)

        val mainLayout = findViewById<View>(R.id.mainRootLayout)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        val cardProfile = findViewById<LinearLayout>(R.id.profile)
        tvProfileName = findViewById(R.id.tvProfileName)
        tvInitialsAvatar = findViewById(R.id.jw)
        val btnLogout = findViewById<TextView>(R.id.logout)

        val btnHome = findViewById<View>(R.id.home)
        val btnFriends = findViewById<View>(R.id.friends)
        val btnMarket = findViewById<View>(R.id.market)
        val btnMessenger = findViewById<View>(R.id.messenger)

        val cardMarketplace = findViewById<LinearLayout>(R.id.cardMarketplace)
        val cardFriends = findViewById<LinearLayout>(R.id.cardFriends)

        // 1. Profile Click -> Open ProfileActivity
        cardProfile?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // 2. Navigation Tabs & Shortcuts
        val openFriends = View.OnClickListener {
            val intent = Intent(this, FriendsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }

        val openMarketplace = View.OnClickListener {
            val intent = Intent(this, MarketplaceActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }

        btnHome?.setOnClickListener {
            val intent = Intent(this, HomeFeedActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }

        btnFriends?.setOnClickListener(openFriends)
        cardFriends?.setOnClickListener(openFriends)

        btnMarket?.setOnClickListener(openMarketplace)
        cardMarketplace?.setOnClickListener(openMarketplace)

        findViewById<View>(R.id.notification)?.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
        }

        btnMessenger?.setOnClickListener {
            val intent = Intent(this, ChatsActivity::class.java)
            startActivity(intent)
        }

        // 3. Logout Button Click
        btnLogout?.setOnClickListener {
            performLogout()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    private fun loadUserData() {
        val uid = SessionManager.getCurrentUserUid(this) ?: return
        val user = dbHelper.getUserByUid(uid)

        if (user != null) {
            val firstName = user.firstName
            val lastName = user.lastName
            val fullName = "$firstName $lastName".trim()

            tvProfileName.text = if (fullName.isNotEmpty()) fullName else "User"
            tvInitialsAvatar.text = UserHelper.getUserInitials(firstName, lastName)
        }
    }

    private fun performLogout() {
        SessionManager.logout(this)
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}