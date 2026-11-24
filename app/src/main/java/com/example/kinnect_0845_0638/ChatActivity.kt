package com.example.kinnect_0845_0638

import android.content.Intent
import android.database.ContentObserver
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ChatActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var tvAvatarInitials: TextView
    private lateinit var tvReceiverName: TextView
    private lateinit var etMessage: EditText
    private lateinit var containerMessages: LinearLayout
    private lateinit var scrollViewChat: ScrollView

    private var receiverUid: String = ""
    private var receiverName: String = ""
    private var chatRoomId: String = ""
    private var editingMessageId: String? = null
    private var screenshotObserver: ContentObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        dbHelper = DatabaseHelper.getInstance(this)

        val mainLayout = findViewById<View>(R.id.main)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        receiverUid = intent.getStringExtra("RECEIVER_UID") ?: ""
        receiverName = intent.getStringExtra("RECEIVER_NAME") ?: "User"

        val currentUid = SessionManager.getCurrentUserUid(this) ?: ""
        if (currentUid.isNotEmpty() && receiverUid.isNotEmpty()) {
            chatRoomId = if (currentUid < receiverUid) "${currentUid}_${receiverUid}" else "${receiverUid}_${currentUid}"
        }

        val btnBack = findViewById<ImageButton>(R.id.backButton)
        val voiceCallBtn = findViewById<ImageButton>(R.id.voiceCallButton)
        val videoCallBtn = findViewById<ImageButton>(R.id.videoCallButton)
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials)
        tvReceiverName = findViewById(R.id.tvReceiverName)
        val tvReceiverStatus = findViewById<TextView>(R.id.tvReceiverStatus)
        etMessage = findViewById(R.id.etMessage)
        containerMessages = findViewById(R.id.containerMessages)
        scrollViewChat = findViewById(R.id.scrollViewChat)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)

        btnBack?.setOnClickListener { finish() }

        // Update Active Status
        val receiverUser = dbHelper.getUserByUid(receiverUid)
        if (receiverUser != null && receiverUser.isOnline) {
            tvReceiverStatus?.text = "Active now"
            tvReceiverStatus?.setTextColor(Color.parseColor("#27AE60"))
        } else {
            tvReceiverStatus?.text = "Offline"
            tvReceiverStatus?.setTextColor(Color.parseColor("#888888"))
        }

        val cleanChannelName = if (receiverName.isNotEmpty()) {
            "call_${receiverName.replace(" ", "_").lowercase()}"
        } else {
            "kinnect_call"
        }

        val nameParts = receiverName.split(" ")
        val firstName = nameParts.getOrNull(0) ?: ""
        val lastName = nameParts.getOrNull(1) ?: ""
        val initials = UserHelper.getUserInitials(firstName, lastName)

        // 1. Voice Call Button -> Launches VoiceCallActivity
        voiceCallBtn?.setOnClickListener {
            val intent = Intent(this, VoiceCallActivity::class.java).apply {
                putExtra("CHANNEL_NAME", cleanChannelName)
                putExtra("RECEIVER_NAME", receiverName)
                putExtra("RECEIVER_INITIALS", initials)
            }
            startActivity(intent)
        }

        // 2. Video Call Button -> Launches VideoCallActivity
        videoCallBtn?.setOnClickListener {
            val intent = Intent(this, VideoCallActivity::class.java).apply {
                putExtra("CHANNEL_NAME", cleanChannelName)
                putExtra("RECEIVER_NAME", receiverName)
            }
            startActivity(intent)
        }

        // Setup Header Title & Initials
        tvReceiverName.text = receiverName
        tvAvatarInitials.text = initials

        // Send or Edit Message
        btnSend?.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty() && chatRoomId.isNotEmpty()) {
                if (editingMessageId != null) {
                    dbHelper.editMessage(editingMessageId!!, text)
                    editingMessageId = null
                    etMessage.setText("")
                    Toast.makeText(this, "Message edited", Toast.LENGTH_SHORT).show()
                } else {
                    sendMessageToSQLite(currentUid, text)
                }
                loadMessages()
            }
        }

        if (chatRoomId.isNotEmpty()) {
            dbHelper.markIncomingMessagesAsRead(chatRoomId, currentUid)
            loadMessages()
        }

        // Setup Screenshot Detection
        setupScreenshotDetection()
    }

    private fun setupScreenshotDetection() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val callback = ScreenCaptureCallback {
                    onScreenshotTaken()
                }
                registerScreenCaptureCallback(mainExecutor, callback)
            } else {
                setupContentObserverScreenshotDetection()
            }
        } catch (_: Throwable) {
            setupContentObserverScreenshotDetection()
        }
    }

    private fun setupContentObserverScreenshotDetection() {
        screenshotObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                if (uri != null && uri.toString().lowercase().contains("screenshot")) {
                    onScreenshotTaken()
                }
            }
        }
        try {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                screenshotObserver!!
            )
        } catch (_: Exception) {}
    }

    private fun onScreenshotTaken() {
        Toast.makeText(this, "📸 Screenshot taken of this chat!", Toast.LENGTH_LONG).show()
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return
        if (receiverUid.isNotEmpty()) {
            dbHelper.addNotification(
                receiverUid = receiverUid,
                senderUid = currentUid,
                senderName = "Chat Security",
                targetUid = receiverUid,
                type = "chat",
                message = "took a screenshot of your chat conversation!"
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val currentUid = SessionManager.getCurrentUserUid(this) ?: ""
        if (chatRoomId.isNotEmpty() && currentUid.isNotEmpty()) {
            dbHelper.markIncomingMessagesAsRead(chatRoomId, currentUid)
            loadMessages()
        }
        val tvReceiverStatus = findViewById<TextView>(R.id.tvReceiverStatus)
        val receiverUser = dbHelper.getUserByUid(receiverUid)
        if (receiverUser != null && receiverUser.isOnline) {
            tvReceiverStatus?.text = "Active now"
            tvReceiverStatus?.setTextColor(Color.parseColor("#27AE60"))
        } else {
            tvReceiverStatus?.text = "Offline"
            tvReceiverStatus?.setTextColor(Color.parseColor("#888888"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        screenshotObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (_: Exception) {}
        }
    }

    private fun sendMessageToSQLite(senderUid: String, messageText: String) {
        SyncManager.enqueueOrSendMessage(this, senderUid, receiverUid, messageText)
        etMessage.setText("")
    }

    private fun loadMessages() {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return
        if (chatRoomId.isEmpty()) return

        containerMessages.removeAllViews()
        val messages = dbHelper.getChatMessages(chatRoomId)

        for (msg in messages) {
            val isSentByMe = (msg.senderUid == currentUid)
            addMessageBubble(msg.messageId, msg.text, isSentByMe, msg.status, msg.reaction, msg.isEdited)
        }

        scrollViewChat.post {
            scrollViewChat.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun addMessageBubble(
        msgId: String,
        messageText: String,
        isSentByMe: Boolean,
        status: String = "sent",
        reaction: String = "",
        isEdited: Boolean = false
    ) {
        val bubbleLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 4.dpToPx(), 0, 4.dpToPx()) }
            gravity = if (isSentByMe) Gravity.END else Gravity.START
        }

        val messageContainer = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = if (isSentByMe) Gravity.END else Gravity.START
        }

        val bubbleAndReactionRow = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvMessage = TextView(this).apply {
            text = messageText
            textSize = 14f
            setTextColor(if (isSentByMe) Color.WHITE else Color.BLACK)
            setBackgroundResource(R.drawable.oval)
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (isSentByMe) Color.parseColor("#0B5F63") else Color.parseColor("#E4E6EB")
            )
            setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())
            isClickable = true
            isFocusable = true
        }

        if (reaction.isNotEmpty()) {
            val reactionIcon = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(20.dpToPx(), 20.dpToPx()).apply {
                    setMargins(4.dpToPx(), 0, 4.dpToPx(), 0)
                }
                val (iconRes, colorHex) = getReactionResAndColor(reaction)
                setImageResource(iconRes)
                setColorFilter(Color.parseColor(colorHex))
            }
            if (isSentByMe) {
                bubbleAndReactionRow.addView(reactionIcon, 0)
                bubbleAndReactionRow.addView(tvMessage)
            } else {
                bubbleAndReactionRow.addView(tvMessage)
                bubbleAndReactionRow.addView(reactionIcon)
            }
        } else {
            bubbleAndReactionRow.addView(tvMessage)
        }

        messageContainer.addView(bubbleAndReactionRow)

        val subtextRow = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(4.dpToPx(), 2.dpToPx(), 4.dpToPx(), 0) }
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isSentByMe) Gravity.END else Gravity.START
        }

        if (isEdited) {
            val tvEdited = TextView(this).apply {
                text = "edited"
                textSize = 10f
                setTextColor(Color.parseColor("#888888"))
                setPadding(0, 0, 4.dpToPx(), 0)
            }
            subtextRow.addView(tvEdited)
        }

        if (isSentByMe) {
            val statusCircle = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(10.dpToPx(), 10.dpToPx()).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
                if (status.lowercase() == "read") {
                    setBackgroundResource(R.drawable.circle)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#888888"))
                } else {
                    setBackgroundResource(R.drawable.ic_circle_outline)
                    backgroundTintList = null
                }
            }
            subtextRow.addView(statusCircle)
        }

        messageContainer.addView(subtextRow)

        tvMessage.setOnLongClickListener {
            showActionMenuPopup(tvMessage, msgId, messageText, isSentByMe, reaction)
            true
        }

        bubbleLayout.addView(messageContainer)
        containerMessages.addView(bubbleLayout)
    }

    private fun showActionMenuPopup(
        anchorView: View,
        msgId: String,
        messageText: String,
        isSentByMe: Boolean,
        currentReaction: String
    ) {
        val popupView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_rounded_card)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())
            elevation = 16f
        }

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 16f
            isOutsideTouchable = true
        }

        val tvDelete = TextView(this).apply {
            text = "Delete"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#D93025"))
            setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())

            setOnClickListener {
                dbHelper.deleteMessage(msgId)
                Toast.makeText(this@ChatActivity, "Message deleted", Toast.LENGTH_SHORT).show()
                loadMessages()
                popupWindow.dismiss()
            }
        }

        val tvEdit = TextView(this).apply {
            text = "Edit"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0B5F63"))
            setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())

            setOnClickListener {
                editingMessageId = msgId
                etMessage.setText(messageText)
                etMessage.setSelection(messageText.length)
                etMessage.requestFocus()
                Toast.makeText(this@ChatActivity, "Editing message...", Toast.LENGTH_SHORT).show()
                popupWindow.dismiss()
            }
        }

        val tvReact = TextView(this).apply {
            text = "React"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1C1E21"))
            setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())

            setOnClickListener {
                popupWindow.dismiss()
                showChatReactionPopup(anchorView, msgId, currentReaction)
            }
        }

        popupView.addView(tvDelete)
        if (isSentByMe) {
            popupView.addView(tvEdit)
        }
        popupView.addView(tvReact)

        popupWindow.showAsDropDown(anchorView, 0, -60.dpToPx())
    }

    private fun showChatReactionPopup(anchorView: View, msgId: String, currentReaction: String) {
        val popupView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_rounded_card)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())
            elevation = 16f
        }

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 16f
            isOutsideTouchable = true
        }

        val reactions = listOf(
            Triple("heart", R.drawable.heart, "#D93025"),
            Triple("like", R.drawable.like, "#0B5F63"),
            Triple("face", R.drawable.face, "#F2994A"),
            Triple("emoji", R.drawable.emoji, "#F2C94C"),
            Triple("star", R.drawable.star, "#27AE60"),
            Triple("highlight", R.drawable.highlight, "#9B51E0")
        )

        for ((type, iconRes, colorHex) in reactions) {
            val emojiBtn = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(32.dpToPx(), 32.dpToPx()).apply {
                    marginEnd = 6.dpToPx()
                }
                setImageResource(iconRes)
                setColorFilter(Color.parseColor(colorHex))
                setPadding(2.dpToPx(), 2.dpToPx(), 2.dpToPx(), 2.dpToPx())
                isClickable = true
                isFocusable = true

                setOnClickListener {
                    if (currentReaction == type) {
                        dbHelper.setMessageReaction(msgId, "")
                    } else {
                        dbHelper.setMessageReaction(msgId, type)
                    }
                    loadMessages()
                    popupWindow.dismiss()
                }
            }
            popupView.addView(emojiBtn)
        }

        popupWindow.showAsDropDown(anchorView, 0, -70.dpToPx())
    }

    private fun getReactionResAndColor(type: String): Pair<Int, String> {
        return when (type) {
            "heart" -> Pair(R.drawable.heart, "#D93025")
            "like" -> Pair(R.drawable.like, "#0B5F63")
            "face" -> Pair(R.drawable.face, "#F2994A")
            "emoji" -> Pair(R.drawable.emoji, "#F2C94C")
            "star" -> Pair(R.drawable.star, "#27AE60")
            "highlight" -> Pair(R.drawable.highlight, "#9B51E0")
            else -> Pair(R.drawable.heart, "#D93025")
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}