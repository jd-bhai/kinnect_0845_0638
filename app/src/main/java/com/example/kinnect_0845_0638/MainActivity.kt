package com.example.kinnect_0845_0638

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val splashDelay: Long = 5000 // 5 seconds delay requirement

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_activity)

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserSession()
        }, splashDelay)
    }

    private fun checkUserSession() {
        val currentUid = SessionManager.getCurrentUserUid(this)

        if (currentUid != null) {
            // User is already logged in -> Go directly to Home
            val intent = Intent(this, HomeFeedActivity::class.java)
            startActivity(intent)
        } else {
            // User is not logged in -> Go to Log in
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        finish()
    }
}