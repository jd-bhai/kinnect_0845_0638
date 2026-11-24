package com.example.kinnect_0845_0638

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class FriendsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var containerFriendRequests: LinearLayout
    private lateinit var containerSuggestions: LinearLayout
    private lateinit var tvRequestCountBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.friends_activity)

        dbHelper = DatabaseHelper.getInstance(this)

        val mainLayout = findViewById<View>(R.id.mainRootLayout)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        containerFriendRequests = findViewById(R.id.containerFriendRequests)
        containerSuggestions = findViewById(R.id.containerSuggestions)
        tvRequestCountBadge = findViewById(R.id.tvRequestCountBadge)

        setupNavigationTabs()
        loadFriendRequests()
        loadPeopleYouMayKnow()
    }

    override fun onResume() {
        super.onResume()
        SyncManager.performSync(this) {
            runOnUiThread {
                loadFriendRequests()
                loadPeopleYouMayKnow()
            }
        }
    }

    private fun loadFriendRequests() {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return
        containerFriendRequests.removeAllViews()

        val requests = dbHelper.getFriendRequestsForUser(currentUid)
        tvRequestCountBadge.text = requests.size.toString()

        for (sender in requests) {
            val fullName = "${sender.firstName} ${sender.lastName}".trim()
            val initials = UserHelper.getUserInitials(sender.firstName, sender.lastName)
            addFriendRequestRow(sender.uid, fullName, initials)
        }
    }

    private fun addFriendRequestRow(senderUid: String, fullName: String, initials: String) {
        val rowLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                90.dpToPx()
            ).apply { setMargins(0, 0, 0, 10.dpToPx()) }
            orientation = LinearLayout.HORIZONTAL
        }

        val tvAvatar = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                70.dpToPx(),
                70.dpToPx()
            ).apply { setMargins(10.dpToPx(), 10.dpToPx(), 10.dpToPx(), 10.dpToPx()) }
            text = initials
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.circle)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#0B5F63"))
        }

        val midLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 5f)
            orientation = LinearLayout.VERTICAL
        }

        val tvName = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0,
                1f
            )
            text = if (fullName.isNotEmpty()) fullName else "User"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.BOTTOM
        }

        val tvMutuals = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0,
                1f
            )
            text = "Mutual connections available"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#65676B"))
        }

        val btnContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                2f
            )
            orientation = LinearLayout.HORIZONTAL
        }

        val btnConfirm = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2f).apply {
                marginEnd = 10.dpToPx()
                setMargins(0, 5.dpToPx(), 10.dpToPx(), 5.dpToPx())
            }
            text = "Confirm"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0B5F63"))
            setOnClickListener { acceptFriendRequest(senderUid) }
        }

        val btnDelete = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 2f).apply {
                setMargins(0, 5.dpToPx(), 0, 5.dpToPx())
            }
            text = "Delete"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#DAD5B8"))
            setOnClickListener { deleteFriendRequest(senderUid) }
        }

        btnContainer.addView(btnConfirm)
        btnContainer.addView(btnDelete)

        midLayout.addView(tvName)
        midLayout.addView(tvMutuals)
        midLayout.addView(btnContainer)

        val timeLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }
        val tvTime = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            text = "Recent"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(Color.parseColor("#65676B"))
        }
        timeLayout.addView(tvTime)

        rowLayout.addView(tvAvatar)
        rowLayout.addView(midLayout)
        rowLayout.addView(timeLayout)

        containerFriendRequests.addView(rowLayout)
    }

    private fun loadPeopleYouMayKnow() {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return
        containerSuggestions.removeAllViews()

        val allUsers = dbHelper.getAllUsersExcept(currentUid)
        for (user in allUsers) {
            val isFriend = dbHelper.isFriend(currentUid, user.uid)
            val hasSent = dbHelper.hasSentFriendRequest(currentUid, user.uid)
            val hasReceived = dbHelper.hasSentFriendRequest(user.uid, currentUid)

            if (!isFriend && !hasSent && !hasReceived) {
                val fullName = "${user.firstName} ${user.lastName}".trim()
                val initials = UserHelper.getUserInitials(user.firstName, user.lastName)
                addSuggestionRow(user.uid, fullName, initials)
            }
        }
    }

    private fun addSuggestionRow(targetUid: String, fullName: String, initials: String) {
        val rowLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                80.dpToPx()
            ).apply { setMargins(0, 0, 0, 8.dpToPx()) }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvAvatar = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(54.dpToPx(), 54.dpToPx()).apply {
                setMargins(8.dpToPx(), 0, 12.dpToPx(), 0)
            }
            text = initials
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.circle)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#34495E"))
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

        infoLayout.addView(tvName)

        val btnAdd = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(110.dpToPx(), 36.dpToPx()).apply {
                marginEnd = 8.dpToPx()
            }
            text = "Add Friend"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0B5F63"))

            setOnClickListener {
                val senderUid = SessionManager.getCurrentUserUid(this@FriendsActivity) ?: ""
                SyncManager.enqueueOrSendFriendRequest(this@FriendsActivity, senderUid, targetUid)
                text = "Sent"
                isEnabled = false
                setBackgroundColor(Color.GRAY)
            }
        }

        rowLayout.addView(tvAvatar)
        rowLayout.addView(infoLayout)
        rowLayout.addView(btnAdd)

        containerSuggestions.addView(rowLayout)
    }

    private fun acceptFriendRequest(senderUid: String) {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return
        SyncManager.enqueueOrAcceptFriendRequest(this, currentUid, senderUid)
        Toast.makeText(this, "Friend request accepted!", Toast.LENGTH_SHORT).show()
        loadFriendRequests()
        loadPeopleYouMayKnow()
    }

    private fun deleteFriendRequest(senderUid: String) {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return
        val success = dbHelper.deleteFriendRequest(currentUid, senderUid)
        if (success) {
            Toast.makeText(this, "Friend request deleted", Toast.LENGTH_SHORT).show()
            loadFriendRequests()
            loadPeopleYouMayKnow()
        }
    }

    private fun setupNavigationTabs() {
        findViewById<View>(R.id.tabHome)?.setOnClickListener {
            val intent = Intent(this, HomeFeedActivity::class.java)
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.tabMenu)?.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}