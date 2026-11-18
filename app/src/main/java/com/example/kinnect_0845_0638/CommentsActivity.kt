package com.example.kinnect_0845_0638

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CommentsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper

    private lateinit var containerComments: LinearLayout
    private lateinit var tvNoComments: TextView
    private lateinit var tvReactionCountSummary: TextView
    private lateinit var etComment: EditText
    private lateinit var layoutReplyBanner: LinearLayout
    private lateinit var tvReplyingTo: TextView
    private lateinit var btnCancelReply: TextView

    private var postId: String = ""
    private var activeReplyCommentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        dbHelper = DatabaseHelper.getInstance(this)

        val mainLayout = findViewById<View>(R.id.mainRootLayout)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        postId = intent.getStringExtra("POST_ID") ?: ""

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        containerComments = findViewById(R.id.containerComments)
        tvNoComments = findViewById(R.id.tvNoComments)
        tvReactionCountSummary = findViewById(R.id.tvReactionCountSummary)
        etComment = findViewById(R.id.etComment)
        layoutReplyBanner = findViewById(R.id.layoutReplyBanner)
        tvReplyingTo = findViewById(R.id.tvReplyingTo)
        btnCancelReply = findViewById(R.id.btnCancelReply)
        val btnSendComment = findViewById<ImageButton>(R.id.btnSendComment)

        btnBack?.setOnClickListener { finish() }

        btnCancelReply?.setOnClickListener {
            activeReplyCommentId = null
            layoutReplyBanner.visibility = View.GONE
            etComment.hint = "Write a comment..."
        }

        btnSendComment?.setOnClickListener {
            val text = etComment.text.toString().trim()
            if (text.isNotEmpty() && postId.isNotEmpty()) {
                sendCommentToSQLite(text)
            }
        }

        if (postId.isNotEmpty()) {
            loadPostDetailsAndComments()
        }
    }

    private fun loadPostDetailsAndComments() {
        val count = dbHelper.getPostReactionsCount(postId)
        tvReactionCountSummary.text = when (count) {
            0 -> "No reactions yet"
            1 -> "1 reaction"
            else -> "$count reactions"
        }

        containerComments.removeAllViews()

        val comments = dbHelper.getPostComments(postId)
        tvNoComments.visibility = if (comments.isEmpty()) View.VISIBLE else View.GONE

        for (comment in comments) {
            val nameParts = comment.authorName.split(" ")
            val fn = nameParts.getOrNull(0) ?: ""
            val ln = nameParts.getOrNull(1) ?: ""
            val initials = UserHelper.getUserInitials(fn, ln)

            addCommentRow(comment.commentId, comment.authorName, initials, comment.text, comment.timestamp)
        }
    }

    private fun sendCommentToSQLite(commentText: String) {
        val currentUid = SessionManager.getCurrentUserUid(this) ?: return
        val currentReplyTargetId = activeReplyCommentId ?: ""

        val user = dbHelper.getUserByUid(currentUid)
        val authorName = if (user != null) "${user.firstName} ${user.lastName}".trim() else "User"

        dbHelper.addPostComment(postId, currentReplyTargetId, currentUid, authorName, commentText)

        etComment.setText("")
        activeReplyCommentId = null
        layoutReplyBanner.visibility = View.GONE
        etComment.hint = "Write a comment..."

        loadPostDetailsAndComments()
    }

    private fun addCommentRow(
        commentId: String,
        authorName: String,
        initials: String,
        commentText: String,
        timestamp: Long
    ) {
        val parentLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 14.dpToPx()) }
            orientation = LinearLayout.VERTICAL
        }

        val rowLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL
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

        val contentLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }

        val bubbleLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_rounded_card)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E0E0E0"))
            setPadding(10.dpToPx(), 8.dpToPx(), 12.dpToPx(), 8.dpToPx())
        }

        val tvAuthor = TextView(this).apply {
            text = authorName
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
        }

        val tvBody = TextView(this).apply {
            text = commentText
            textSize = 13f
            setTextColor(Color.BLACK)
        }

        bubbleLayout.addView(tvAuthor)
        bubbleLayout.addView(tvBody)

        val actionRow = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(4.dpToPx(), 4.dpToPx(), 0, 4.dpToPx()) }
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvReply = TextView(this).apply {
            text = "Reply"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#65676B"))
            setPadding(0, 0, 12.dpToPx(), 0)
            setOnClickListener {
                activeReplyCommentId = commentId
                tvReplyingTo.text = "Replying to $authorName"
                layoutReplyBanner.visibility = View.VISIBLE
                etComment.hint = "Write a reply..."
                etComment.requestFocus()

                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.showSoftInput(etComment, InputMethodManager.SHOW_IMPLICIT)
            }
        }

        val tvLike = TextView(this).apply {
            text = "Like"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#65676B"))
            setPadding(0, 0, 12.dpToPx(), 0)
        }

        val tvTime = TextView(this).apply {
            text = getTimeAgo(timestamp)
            textSize = 12f
            setTextColor(Color.parseColor("#65676B"))
        }

        actionRow.addView(tvReply)
        actionRow.addView(tvLike)
        actionRow.addView(tvTime)

        contentLayout.addView(bubbleLayout)
        contentLayout.addView(actionRow)

        rowLayout.addView(tvAvatar)
        rowLayout.addView(contentLayout)

        parentLayout.addView(rowLayout)

        // Nested Replies
        val replies = dbHelper.getCommentReplies(postId, commentId)
        if (replies.isNotEmpty()) {
            val repliesContainer = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(35.dpToPx(), 6.dpToPx(), 0, 0) }
                orientation = LinearLayout.VERTICAL
            }

            for (reply in replies) {
                val rParts = reply.authorName.split(" ")
                val rFn = rParts.getOrNull(0) ?: ""
                val rLn = rParts.getOrNull(1) ?: ""
                val rInitials = UserHelper.getUserInitials(rFn, rLn)

                val replyRow = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 0, 0, 8.dpToPx()) }
                    orientation = LinearLayout.HORIZONTAL
                }

                val tvRNavatar = TextView(this).apply {
                    layoutParams = LinearLayout.LayoutParams(32.dpToPx(), 32.dpToPx()).apply {
                        marginEnd = 8.dpToPx()
                    }
                    text = rInitials
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                    gravity = Gravity.CENTER
                    setBackgroundResource(R.drawable.circle)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#34495E"))
                }

                val rContent = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    orientation = LinearLayout.VERTICAL
                }

                val rBubble = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    orientation = LinearLayout.VERTICAL
                    setBackgroundResource(R.drawable.bg_rounded_card)
                    backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E0E0E0"))
                    setPadding(8.dpToPx(), 6.dpToPx(), 10.dpToPx(), 6.dpToPx())
                }

                val tvRAuthor = TextView(this).apply {
                    text = reply.authorName
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.BLACK)
                }

                val tvRBody = TextView(this).apply {
                    text = reply.text
                    textSize = 12f
                    setTextColor(Color.BLACK)
                }

                rBubble.addView(tvRAuthor)
                rBubble.addView(tvRBody)

                val rActionRow = LinearLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(4.dpToPx(), 2.dpToPx(), 0, 0) }
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }

                val tvRReply = TextView(this).apply {
                    text = "Reply"
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#65676B"))
                    setPadding(0, 0, 10.dpToPx(), 0)
                }

                val tvRLike = TextView(this).apply {
                    text = "Like"
                    textSize = 12f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#65676B"))
                    setPadding(0, 0, 10.dpToPx(), 0)
                }

                val tvRTime = TextView(this).apply {
                    text = getTimeAgo(reply.timestamp)
                    textSize = 11f
                    setTextColor(Color.parseColor("#65676B"))
                }

                rActionRow.addView(tvRReply)
                rActionRow.addView(tvRLike)
                rActionRow.addView(tvRTime)

                rContent.addView(rBubble)
                rContent.addView(rActionRow)

                replyRow.addView(tvRNavatar)
                replyRow.addView(rContent)

                repliesContainer.addView(replyRow)
            }

            parentLayout.addView(repliesContainer)
        }

        containerComments.addView(parentLayout)
    }

    private fun getTimeAgo(timeMs: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timeMs
        if (diff < 0) return "Just now"

        val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(diff)
        val days = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diff)

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