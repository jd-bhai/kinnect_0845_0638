package com.example.kinnect_0845_0638

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.concurrent.TimeUnit

class HomeFeedActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var containerContacts: LinearLayout
    private lateinit var containerPosts: LinearLayout
    private lateinit var layoutEmptyFeed: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.homefeed_activity)

        dbHelper = DatabaseHelper.getInstance(this)

        val mainLayout = findViewById<View>(R.id.mainRootLayout)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        containerContacts = findViewById(R.id.containerContacts)
        containerPosts = findViewById(R.id.containerPosts)
        layoutEmptyFeed = findViewById(R.id.layoutEmptyFeed)

        val tvUserInitialsHeader = findViewById<TextView>(R.id.tvUserInitialsHeader)
        val tvStoryMyInitials = findViewById<TextView>(R.id.tvStoryMyInitials)
        val tvCreatePostPrompt = findViewById<TextView>(R.id.tvCreatePostPrompt)

        tvUserInitialsHeader?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        setUserOnlineStatus(true)

        loadCurrentUserData(tvUserInitialsHeader, tvStoryMyInitials, tvCreatePostPrompt)
        loadFriendsOnlyStories()
        loadPostsFromSQLite()
        setupNavigationListeners()

        SyncManager.performSync(this) {
            runOnUiThread {
                loadPostsFromSQLite()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadFriendsOnlyStories()
        loadPostsFromSQLite()
        SyncManager.performSync(this) {
            runOnUiThread {
                loadPostsFromSQLite()
            }
        }
    }

    private fun loadCurrentUserData(
        tvHeader: TextView?,
        tvStory: TextView?,
        tvPrompt: TextView?
    ) {
        val uid = SessionManager.getCurrentUserUid(this) ?: return
        val user = dbHelper.getUserByUid(uid) ?: return
        val firstName = user.firstName
        val lastName = user.lastName

        val initials = UserHelper.getUserInitials(firstName, lastName)

        tvHeader?.text = initials
        tvStory?.text = initials

        if (firstName.isNotEmpty() && tvPrompt != null) {
            tvPrompt.text = "What's on your mind, $firstName?"
        }
    }

    private fun loadFriendsOnlyStories() {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return
        containerContacts.removeAllViews()

        val friends = dbHelper.getFriendsForUser(currentUid)
        for (friend in friends) {
            val fullName = "${friend.firstName} ${friend.lastName}".trim()
            val initials = UserHelper.getUserInitials(friend.firstName, friend.lastName)
            addContactCardView(friend.uid, fullName, initials)
        }
    }

    private fun loadPostsFromSQLite() {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: ""
        containerPosts.removeAllViews()

        val posts = dbHelper.getAllPosts(currentUid)
        layoutEmptyFeed.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE

        for (post in posts) {
            val author = dbHelper.getUserByUid(post.authorUid)
            val firstName = author?.firstName ?: ""
            val lastName = author?.lastName ?: ""
            val fullName = "$firstName $lastName".trim()
            val initials = UserHelper.getUserInitials(firstName, lastName)

            renderPostCard(
                postId = post.postId,
                authorUid = post.authorUid,
                authorName = fullName,
                initials = initials,
                captionText = post.caption,
                imageUriStr = post.imageUri,
                timestamp = post.timestamp,
                reactionsCount = post.reactionsCount,
                userReaction = post.userReaction
            )
        }
    }

    private fun renderPostCard(
        postId: String,
        authorUid: String,
        authorName: String,
        initials: String,
        captionText: String,
        imageUriStr: String,
        timestamp: Long,
        reactionsCount: Int,
        userReaction: String
    ) {
        val existingCard = containerPosts.findViewWithTag<View>(postId)
        if (existingCard != null) {
            containerPosts.removeView(existingCard)
        }

        val postCard = LinearLayout(this).apply {
            tag = postId
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12.dpToPx()) }
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(14.dpToPx(), 14.dpToPx(), 14.dpToPx(), 14.dpToPx())
        }

        // Header Row
        val headerLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isClickable = true
            isFocusable = true
        }

        headerLayout.setOnClickListener {
            val currentUid = SessionManager.getCurrentUserUid(this)
            if (authorUid == currentUid) {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, ViewUserProfileActivity::class.java).apply {
                    putExtra("TARGET_UID", authorUid)
                }
                startActivity(intent)
            }
        }

        val tvAvatar = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(42.dpToPx(), 42.dpToPx()).apply {
                marginEnd = 10.dpToPx()
            }
            text = initials
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.circle)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#0B5F63"))
        }

        val nameTimeLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }

        val tvName = TextView(this).apply {
            text = if (authorName.isNotEmpty()) authorName else "User"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1C1E21"))
        }

        val tvTimeAgo = TextView(this).apply {
            text = getTimeAgo(timestamp)
            textSize = 12f
            setTextColor(Color.parseColor("#65676B"))
        }

        nameTimeLayout.addView(tvName)
        nameTimeLayout.addView(tvTimeAgo)

        headerLayout.addView(tvAvatar)
        headerLayout.addView(nameTimeLayout)
        postCard.addView(headerLayout)

        // Caption
        if (captionText.isNotEmpty()) {
            val tvCaption = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 10.dpToPx(), 0, 10.dpToPx()) }
                text = captionText
                textSize = 14f
                setTextColor(Color.parseColor("#1C1E21"))
            }
            postCard.addView(tvCaption)
        }

        // Image
        if (imageUriStr.isNotEmpty()) {
            val ivPostImage = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    220.dpToPx()
                ).apply { setMargins(0, 4.dpToPx(), 0, 10.dpToPx()) }
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            postCard.addView(ivPostImage)

            Thread {
                var imageLoaded = false

                if (!imageUriStr.startsWith("content://") && !imageUriStr.startsWith("http")) {
                    try {
                        val cleanBase64 = imageUriStr.replace("\n", "").replace("\r", "").trim()
                        val decodedBytes = Base64.decode(cleanBase64, Base64.NO_WRAP)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        if (bitmap != null) {
                            runOnUiThread {
                                ivPostImage.setImageBitmap(bitmap)
                            }
                            imageLoaded = true
                        }
                    } catch (_: Exception) {}
                }

                if (!imageLoaded) {
                    try {
                        val uri = Uri.parse(imageUriStr)
                        val inputStream = contentResolver.openInputStream(uri)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream?.close()
                        if (bitmap != null) {
                            runOnUiThread {
                                ivPostImage.setImageBitmap(bitmap)
                            }
                        }
                    } catch (_: Throwable) {}
                }
            }.start()
        }

        // Reactions Count Row
        val reactionCountRow = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 4.dpToPx(), 0, 4.dpToPx()) }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val ivReactionIcon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(16.dpToPx(), 16.dpToPx()).apply {
                marginEnd = 6.dpToPx()
            }
            val iconRes = when (userReaction) {
                "heart" -> R.drawable.heart
                "like" -> R.drawable.like
                "face" -> R.drawable.face
                "emoji" -> R.drawable.emoji
                "star" -> R.drawable.star
                "highlight" -> R.drawable.highlight
                else -> R.drawable.heart
            }
            setImageResource(iconRes)
            setColorFilter(Color.parseColor("#D93025"))
        }

        val tvReactionCount = TextView(this).apply {
            text = when (reactionsCount) {
                0 -> "No reactions yet"
                1 -> "1 reaction"
                else -> "$reactionsCount reactions"
            }
            textSize = 12f
            setTextColor(Color.parseColor("#65676B"))
        }

        reactionCountRow.addView(ivReactionIcon)
        reactionCountRow.addView(tvReactionCount)
        postCard.addView(reactionCountRow)

        // Divider
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1.dpToPx()
            ).apply { setMargins(0, 6.dpToPx(), 0, 6.dpToPx()) }
            setBackgroundColor(Color.parseColor("#E4E6EB"))
        }
        postCard.addView(divider)

        // Action Buttons (Like with Popup, Comment, Share)
        val actionsLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                36.dpToPx()
            )
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val commentBtn = createActionButton("Comment", R.drawable.messenger).apply {
            setOnClickListener {
                val intent = Intent(this@HomeFeedActivity, CommentsActivity::class.java).apply {
                    putExtra("POST_ID", postId)
                }
                startActivity(intent)
            }
        }

        actionsLayout.addView(createLikeActionButton(postId, userReaction))
        actionsLayout.addView(commentBtn)
        actionsLayout.addView(createActionButton("Share", R.drawable.share))

        postCard.addView(actionsLayout)

        containerPosts.addView(postCard)
    }

    private fun createActionButton(label: String, iconResId: Int): LinearLayout {
        val actionItem = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
        }

        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(18.dpToPx(), 18.dpToPx()).apply {
                marginEnd = 6.dpToPx()
            }
            setImageResource(iconResId)
            setColorFilter(Color.parseColor("#65676B"))
        }

        val tvLabel = TextView(this).apply {
            text = label
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#65676B"))
        }

        actionItem.addView(icon)
        actionItem.addView(tvLabel)

        actionItem.setOnClickListener {
            Toast.makeText(this, "$label tapped", Toast.LENGTH_SHORT).show()
        }

        return actionItem
    }

    private fun createLikeActionButton(postId: String, userReaction: String): LinearLayout {
        val actionItem = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
        }

        val iconResAndColor = when (userReaction) {
            "heart" -> Pair(R.drawable.heart, "#D93025")
            "like" -> Pair(R.drawable.like, "#0B5F63")
            "face" -> Pair(R.drawable.face, "#F2994A")
            "emoji" -> Pair(R.drawable.emoji, "#F2C94C")
            "star" -> Pair(R.drawable.star, "#27AE60")
            "highlight" -> Pair(R.drawable.highlight, "#9B51E0")
            else -> Pair(R.drawable.like, "#65676B")
        }

        val icon = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(18.dpToPx(), 18.dpToPx()).apply {
                marginEnd = 6.dpToPx()
            }
            setImageResource(iconResAndColor.first)
            setColorFilter(Color.parseColor(iconResAndColor.second))
        }

        val tvLabel = TextView(this).apply {
            text = if (userReaction.isNotEmpty()) "Liked" else "Like"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(if (userReaction.isNotEmpty()) iconResAndColor.second else "#65676B"))
        }

        actionItem.addView(icon)
        actionItem.addView(tvLabel)

        actionItem.setOnClickListener {
            val currentUid = SessionManager.getCurrentUserUid(this) ?: return@setOnClickListener
            if (userReaction.isNotEmpty()) {
                dbHelper.removePostReaction(postId, currentUid)
            } else {
                dbHelper.setPostReaction(postId, currentUid, "heart")
            }
            loadPostsFromSQLite()
        }

        actionItem.setOnLongClickListener {
            showReactionPopup(actionItem, postId, userReaction)
            true
        }

        return actionItem
    }

    private fun showReactionPopup(anchorView: View, postId: String, currentReaction: String) {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return

        val popupView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundResource(R.drawable.bg_rounded_card)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            setPadding(12.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())
            elevation = 16f
        }

        val reactions = listOf(
            Triple("heart", R.drawable.heart, "#D93025"),
            Triple("like", R.drawable.like, "#0B5F63"),
            Triple("face", R.drawable.face, "#F2994A"),
            Triple("emoji", R.drawable.emoji, "#F2C94C"),
            Triple("star", R.drawable.star, "#27AE60"),
            Triple("highlight", R.drawable.highlight, "#9B51E0")
        )

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 16f
            isOutsideTouchable = true
        }

        for ((type, iconRes, colorHex) in reactions) {
            val emojiBtn = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(36.dpToPx(), 36.dpToPx()).apply {
                    marginEnd = 8.dpToPx()
                }
                setImageResource(iconRes)
                setColorFilter(Color.parseColor(colorHex))
                setPadding(4.dpToPx(), 4.dpToPx(), 4.dpToPx(), 4.dpToPx())
                isClickable = true
                isFocusable = true

                setOnClickListener {
                    if (currentReaction == type) {
                        dbHelper.removePostReaction(postId, currentUid)
                    } else {
                        dbHelper.setPostReaction(postId, currentUid, type)
                    }
                    loadPostsFromSQLite()
                    popupWindow.dismiss()
                }
            }
            popupView.addView(emojiBtn)
        }

        popupWindow.showAsDropDown(anchorView, 0, -80.dpToPx())
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
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> "${days / 7}w ago"
        }
    }

    private fun addContactCardView(friendUid: String, fullName: String, initials: String) {
        val cardLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(100.dpToPx(), LinearLayout.LayoutParams.MATCH_PARENT).apply {
                marginEnd = 10.dpToPx()
            }
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_rounded_card)
            isClickable = true
            isFocusable = true
        }

        val topRel = RelativeLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 2.2f)
            setBackgroundColor(Color.parseColor("#34495E"))
        }

        val tvAvatar = TextView(this).apply {
            val params = RelativeLayout.LayoutParams(40.dpToPx(), 40.dpToPx()).apply {
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
                addRule(RelativeLayout.ALIGN_PARENT_START)
                setMargins(8.dpToPx(), 8.dpToPx(), 0, 0)
            }
            layoutParams = params
            text = initials
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.circle)
        }
        topRel.addView(tvAvatar)

        val bottomRel = RelativeLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f)
            setBackgroundColor(Color.BLACK)
            background.alpha = 180
        }

        val tvName = TextView(this).apply {
            val params = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
            layoutParams = params
            text = if (fullName.isNotEmpty()) fullName else "Friend"
            textSize = 11f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        bottomRel.addView(tvName)

        cardLayout.addView(topRel)
        cardLayout.addView(bottomRel)

        cardLayout.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java).apply {
                putExtra("RECEIVER_UID", friendUid)
                putExtra("RECEIVER_NAME", if (fullName.isNotEmpty()) fullName else "Friend")
            }
            startActivity(intent)
        }

        containerContacts.addView(cardLayout)
    }

    private fun setupNavigationListeners() {
        findViewById<View>(R.id.tabMenu)?.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.tabFriends)?.setOnClickListener {
            val intent = Intent(this, FriendsActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.tabMarketplace)?.setOnClickListener {
            val intent = Intent(this, MarketplaceActivity::class.java)
            startActivity(intent)
        }

        val openCreatePost = View.OnClickListener {
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.tvCreatePostPrompt)?.setOnClickListener(openCreatePost)
        findViewById<View>(R.id.btnPhotoPicker)?.setOnClickListener(openCreatePost)

        findViewById<View>(R.id.btnPlusCreateStory)?.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.tvStoryMyInitials)?.setOnClickListener {
            val currentUid = SessionManager.getCurrentUserUid(this) ?: return@setOnClickListener
            val story = dbHelper.getLatestUserStory(currentUid)
            if (story != null) {
                val intent = Intent(this@HomeFeedActivity, YourStoryActivity::class.java).apply {
                    putExtra("STORY_ID", story.storyId)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this@HomeFeedActivity, "No active story found. Tap + to create one!", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<View>(R.id.tabNotifications)?.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnSearch)?.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        findViewById<View>(R.id.btnMessenger)?.setOnClickListener {
            val intent = Intent(this, ChatsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setUserOnlineStatus(isOnline: Boolean) {
        val uid = SessionManager.getCurrentUserUid(this) ?: return
        dbHelper.setOnlineStatus(uid, isOnline)
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}