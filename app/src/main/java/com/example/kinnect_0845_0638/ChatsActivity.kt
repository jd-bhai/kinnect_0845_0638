package com.example.kinnect_0845_0638

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ChatsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var containerActiveFriends: LinearLayout
    private lateinit var containerChatsList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chats)

        dbHelper = DatabaseHelper.getInstance(this)

        val mainLayout = findViewById<View>(R.id.mainRootLayout)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        containerActiveFriends = findViewById(R.id.containerActiveFriends)
        containerChatsList = findViewById(R.id.containerChatsList)

        val btnBack = findViewById<ImageButton>(R.id.backToHome)
        btnBack?.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadFriendsAndChats()
    }

    private fun loadFriendsAndChats() {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return
        containerActiveFriends.removeAllViews()
        containerChatsList.removeAllViews()

        val friends = dbHelper.getFriendsForUser(currentUid)
        if (friends.isEmpty()) {
            loadGeneralUserSuggestions(currentUid)
            return
        }

        for (friend in friends) {
            val fullName = "${friend.firstName} ${friend.lastName}".trim()
            val initials = UserHelper.getUserInitials(friend.firstName, friend.lastName)

            addActiveFriendItem(friend.uid, fullName, initials)
            addChatListItem(friend.uid, fullName, initials)
        }
    }

    private fun loadGeneralUserSuggestions(currentUid: String) {
        var allUsers = dbHelper.getAllUsersExcept(currentUid)
        if (allUsers.isEmpty()) {
            dbHelper.insertUser(
                UserData(
                    uid = "demo_user_1",
                    firstName = "Ammi",
                    lastName = "",
                    email = "ammi@gmail.com",
                    username = "ammi",
                    isOnline = true
                )
            )
            dbHelper.insertUser(
                UserData(
                    uid = "demo_user_2",
                    firstName = "Kinnect",
                    lastName = "Partner",
                    email = "partner@gmail.com",
                    username = "partner",
                    isOnline = true
                )
            )
            allUsers = dbHelper.getAllUsersExcept(currentUid)
        }

        for (user in allUsers) {
            val fullName = "${user.firstName} ${user.lastName}".trim()
            val initials = UserHelper.getUserInitials(user.firstName, user.lastName)

            addActiveFriendItem(user.uid, fullName, initials)
            addChatListItem(user.uid, fullName, initials)
        }
    }

    private fun addActiveFriendItem(friendUid: String, fullName: String, initials: String) {
        val layout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(70.dpToPx(), LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                marginEnd = 12.dpToPx()
            }
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isClickable = true
            isFocusable = true
        }

        val tvAvatar = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(54.dpToPx(), 54.dpToPx())
            text = initials
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.circle)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#0B5F63"))
        }

        val tvName = TextView(this).apply {
            text = if (fullName.isNotEmpty()) fullName.split(" ")[0] else "User"
            textSize = 12f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            maxLines = 1
        }

        layout.addView(tvAvatar)
        layout.addView(tvName)

        layout.setOnClickListener {
            openChatActivity(friendUid, fullName)
        }

        containerActiveFriends.addView(layout)
    }

    private fun addChatListItem(friendUid: String, fullName: String, initials: String) {
        val rowLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                70.dpToPx()
            ).apply { setMargins(0, 0, 0, 8.dpToPx()) }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
        }

        val tvAvatar = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(50.dpToPx(), 50.dpToPx()).apply {
                marginEnd = 12.dpToPx()
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
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
        }

        val tvSubtitle = TextView(this).apply {
            text = "Tap to start chatting..."
            textSize = 13f
            setTextColor(Color.parseColor("#65676B"))
        }

        infoLayout.addView(tvName)
        infoLayout.addView(tvSubtitle)

        rowLayout.addView(tvAvatar)
        rowLayout.addView(infoLayout)

        rowLayout.setOnClickListener {
            openChatActivity(friendUid, fullName)
        }

        containerChatsList.addView(rowLayout)
    }

    private fun openChatActivity(friendUid: String, fullName: String) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra("RECEIVER_UID", friendUid)
            putExtra("RECEIVER_NAME", if (fullName.isNotEmpty()) fullName else "User")
        }
        startActivity(intent)
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}