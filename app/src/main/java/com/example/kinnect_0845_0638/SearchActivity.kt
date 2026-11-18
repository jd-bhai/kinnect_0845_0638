package com.example.kinnect_0845_0638

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SearchActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var etSearchQuery: EditText
    private lateinit var containerSearchResults: LinearLayout
    private lateinit var tvNoResults: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        dbHelper = DatabaseHelper.getInstance(this)

        val mainLayout = findViewById<View>(R.id.mainRootLayout)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnClearSearch = findViewById<ImageButton>(R.id.btnClearSearch)
        etSearchQuery = findViewById(R.id.etSearchQuery)
        containerSearchResults = findViewById(R.id.containerSearchResults)
        tvNoResults = findViewById(R.id.tvNoResults)

        btnBack?.setOnClickListener { finish() }

        btnClearSearch?.setOnClickListener {
            etSearchQuery.setText("")
            containerSearchResults.removeAllViews()
            tvNoResults.visibility = View.VISIBLE
            tvNoResults.text = "Type name or username to search..."
        }

        etSearchQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    performSearch(query)
                } else {
                    containerSearchResults.removeAllViews()
                    tvNoResults.visibility = View.VISIBLE
                    tvNoResults.text = "Type name or username to search..."
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun performSearch(query: String) {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return
        containerSearchResults.removeAllViews()

        val results = dbHelper.searchUsers(query, currentUid)
        tvNoResults.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
        if (results.isEmpty()) {
            tvNoResults.text = "No user found matching \"$query\""
            return
        }

        for (user in results) {
            val fullName = "${user.firstName} ${user.lastName}".trim()
            val initials = UserHelper.getUserInitials(user.firstName, user.lastName)
            val isFriend = dbHelper.isFriend(currentUid, user.uid)
            val isSent = dbHelper.hasSentFriendRequest(currentUid, user.uid)

            addUserSearchResultRow(user.uid, fullName, user.username, initials, isFriend, isSent)
        }
    }

    private fun addUserSearchResultRow(
        targetUid: String,
        fullName: String,
        username: String,
        initials: String,
        isFriend: Boolean,
        isSent: Boolean
    ) {
        val rowLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                75.dpToPx()
            ).apply { setMargins(0, 0, 0, 8.dpToPx()) }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_rounded_card)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            setPadding(8.dpToPx(), 6.dpToPx(), 8.dpToPx(), 6.dpToPx())
            isClickable = true
            isFocusable = true

            setOnClickListener {
                val intent = Intent(this@SearchActivity, ViewUserProfileActivity::class.java).apply {
                    putExtra("TARGET_UID", targetUid)
                }
                startActivity(intent)
            }
        }

        val tvAvatar = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(48.dpToPx(), 48.dpToPx()).apply {
                marginEnd = 10.dpToPx()
            }
            text = initials
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.circle)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#0B5F63"))
        }

        val infoLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }

        val tvName = TextView(this).apply {
            text = if (fullName.isNotEmpty()) fullName else "User"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
        }

        val tvUserHandle = TextView(this).apply {
            text = if (username.isNotEmpty()) "@$username" else "@user"
            textSize = 12f
            setTextColor(Color.parseColor("#65676B"))
        }

        infoLayout.addView(tvName)
        infoLayout.addView(tvUserHandle)

        val btnAction = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(100.dpToPx(), 34.dpToPx()).apply {
                marginEnd = 4.dpToPx()
            }
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER

            if (isFriend) {
                text = "Message"
                setTextColor(Color.BLACK)
                setBackgroundResource(R.drawable.bg_button_outline)
                setOnClickListener {
                    val intent = Intent(this@SearchActivity, ChatActivity::class.java).apply {
                        putExtra("RECEIVER_UID", targetUid)
                        putExtra("RECEIVER_NAME", fullName)
                    }
                    startActivity(intent)
                }
            } else if (isSent) {
                text = "Sent"
                setTextColor(Color.WHITE)
                isEnabled = false
                setBackgroundResource(R.drawable.bg_rounded_card)
            } else {
                text = "Add Friend"
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.bg_button_teal)

                setOnClickListener {
                    sendFriendRequest(targetUid)
                    text = "Sent"
                    isEnabled = false
                    setBackgroundResource(R.drawable.bg_rounded_card)
                }
            }
        }

        rowLayout.addView(tvAvatar)
        rowLayout.addView(infoLayout)
        rowLayout.addView(btnAction)

        containerSearchResults.addView(rowLayout)
    }

    private fun sendFriendRequest(targetUid: String) {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return
        val success = dbHelper.sendFriendRequest(currentUid, targetUid)
        if (success) {
            Toast.makeText(this, "Friend request sent!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}