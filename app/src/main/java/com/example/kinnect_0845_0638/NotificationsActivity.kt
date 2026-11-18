package com.example.kinnect_0845_0638

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.TimeUnit

class NotificationsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var containerNew: LinearLayout
    private lateinit var containerEarlier: LinearLayout
    private lateinit var tvNoNotifications: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        dbHelper = DatabaseHelper.getInstance(this)

        val mainLayout = findViewById<View>(R.id.mainRootLayout)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        containerNew = findViewById(R.id.containerNotificationsNew)
        containerEarlier = findViewById(R.id.containerNotificationsEarlier)
        tvNoNotifications = findViewById(R.id.tvNoNotifications)

        // Setup navigation tabs
        setupNavigationTabs()
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }

    private fun loadNotifications() {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return
        containerNew.removeAllViews()
        containerEarlier.removeAllViews()

        val notifs = dbHelper.getNotificationsForUser(currentUid)
        tvNoNotifications.visibility = if (notifs.isEmpty()) View.VISIBLE else View.GONE

        val now = System.currentTimeMillis()
        val oneHourMs = 3600 * 1000L

        for (notif in notifs) {
            val nameParts = notif.senderName.split(" ")
            val firstName = nameParts.getOrNull(0) ?: ""
            val lastName = nameParts.getOrNull(1) ?: ""
            val initials = UserHelper.getUserInitials(firstName, lastName)

            val fullText = "${notif.senderName} ${notif.message}"
            val timeAgo = getTimeAgo(notif.timestamp)
            val isNew = (now - notif.timestamp) < oneHourMs

            addNotificationRow(
                notifId = notif.notifId,
                senderUid = notif.senderUid,
                senderName = notif.senderName,
                initials = initials,
                fullText = fullText,
                timeAgo = timeAgo,
                type = notif.type,
                isNew = isNew
            )
        }
    }

    private fun addNotificationRow(
        notifId: String,
        senderUid: String,
        senderName: String,
        initials: String,
        fullText: String,
        timeAgo: String,
        type: String,
        isNew: Boolean
    ) {
        val rowLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                75.dpToPx()
            ).apply { setMargins(0, 0, 0, 8.dpToPx()) }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_rounded_card)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (isNew) Color.parseColor("#B2E8E4") else Color.WHITE
            )
            setPadding(8.dpToPx(), 6.dpToPx(), 8.dpToPx(), 6.dpToPx())
            isClickable = true
            isFocusable = true
        }

        val tvAvatar = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(50.dpToPx(), 50.dpToPx()).apply {
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

        val textLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }

        val tvMessage = TextView(this).apply {
            text = fullText
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            maxLines = 2
        }

        val tvTime = TextView(this).apply {
            text = timeAgo
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0B5F63"))
        }

        textLayout.addView(tvMessage)
        textLayout.addView(tvTime)

        val btnMenu = ImageButton(this).apply {
            layoutParams = LinearLayout.LayoutParams(32.dpToPx(), 32.dpToPx())
            setBackgroundColor(Color.TRANSPARENT)
            setImageResource(R.drawable.meatballs_menu)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        rowLayout.addView(tvAvatar)
        rowLayout.addView(textLayout)
        rowLayout.addView(btnMenu)

        rowLayout.setOnClickListener {
            if (type == "chat") {
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("RECEIVER_UID", senderUid)
                    putExtra("RECEIVER_NAME", senderName)
                }
                startActivity(intent)
            } else if (type == "friend_request") {
                val intent = Intent(this, FriendsActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, ViewUserProfileActivity::class.java).apply {
                    putExtra("TARGET_UID", senderUid)
                }
                startActivity(intent)
            }
        }

        if (isNew) {
            containerNew.addView(rowLayout, 0)
        } else {
            containerEarlier.addView(rowLayout, 0)
        }
    }

    private fun setupNavigationTabs() {
        findViewById<View>(R.id.search)?.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.home)?.setOnClickListener {
            val intent = Intent(this, HomeFeedActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.friends)?.setOnClickListener {
            val intent = Intent(this, FriendsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.market)?.setOnClickListener {
            val intent = Intent(this, MarketplaceActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }

        findViewById<View>(R.id.menu)?.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.messenger)?.setOnClickListener {
            val intent = Intent(this, ChatsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun getTimeAgo(timeMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timeMs
        if (diff < 0) return "Just now"

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m"
            hours < 24 -> "${hours}h"
            days < 7 -> "${days}d"
            else -> "${days / 7}w"
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}