package com.example.kinnect_0845_0638

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

data class UserData(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val birthday: String = "",
    val gender: String = "",
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val bio: String = "",
    val website: String = "",
    val phone: String = "",
    val profilePicUrl: String = "",
    val coverPhotoUrl: String = "",
    val isOnline: Boolean = false,
    val fcmToken: String = ""
)

data class PostData(
    val postId: String = "",
    val authorUid: String = "",
    val caption: String = "",
    val imageUri: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val reactionsCount: Int = 0,
    val userReaction: String = ""
)

data class CommentData(
    val commentId: String = "",
    val postId: String = "",
    val parentCommentId: String = "",
    val authorUid: String = "",
    val authorName: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatMessageData(
    val messageId: String = "",
    val chatRoomId: String = "",
    val senderUid: String = "",
    val receiverUid: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "sent",
    val reaction: String = "",
    val isEdited: Boolean = false
)

data class NotificationData(
    val notifId: String = "",
    val receiverUid: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val targetUid: String = "",
    val type: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class MarketItemData(
    val itemId: String = "",
    val sellerUid: String = "",
    val title: String = "",
    val price: String = "",
    val location: String = "",
    val description: String = "",
    val image: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class StoryData(
    val storyId: String = "",
    val userId: String = "",
    val caption: String = "",
    val mediaUri: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class SyncQueueData(
    val queueId: String = "",
    val actionType: String = "",
    val payloadJson: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "kinnect_full.db"
        private const val DATABASE_VERSION = 2

        @Volatile
        private var INSTANCE: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseHelper(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE users (
                uid TEXT PRIMARY KEY,
                firstName TEXT,
                lastName TEXT,
                birthday TEXT,
                gender TEXT,
                email TEXT UNIQUE,
                password TEXT,
                username TEXT,
                bio TEXT,
                website TEXT,
                phone TEXT,
                profilePicUrl TEXT,
                coverPhotoUrl TEXT,
                isOnline INTEGER DEFAULT 0,
                fcmToken TEXT
            )
        """.trimIndent()

        val createFriendsTable = """
            CREATE TABLE friends (
                userUid TEXT,
                friendUid TEXT,
                PRIMARY KEY (userUid, friendUid)
            )
        """.trimIndent()

        val createFriendRequestsTable = """
            CREATE TABLE friendRequests (
                receiverUid TEXT,
                senderUid TEXT,
                PRIMARY KEY (receiverUid, senderUid)
            )
        """.trimIndent()

        val createPostsTable = """
            CREATE TABLE posts (
                postId TEXT PRIMARY KEY,
                authorUid TEXT,
                caption TEXT,
                imageUri TEXT,
                timestamp INTEGER
            )
        """.trimIndent()

        val createPostReactionsTable = """
            CREATE TABLE postReactions (
                postId TEXT,
                userUid TEXT,
                type TEXT,
                PRIMARY KEY (postId, userUid)
            )
        """.trimIndent()

        val createPostCommentsTable = """
            CREATE TABLE postComments (
                commentId TEXT PRIMARY KEY,
                postId TEXT,
                parentCommentId TEXT,
                authorUid TEXT,
                authorName TEXT,
                text TEXT,
                timestamp INTEGER
            )
        """.trimIndent()

        val createChatsTable = """
            CREATE TABLE chats (
                messageId TEXT PRIMARY KEY,
                chatRoomId TEXT,
                senderUid TEXT,
                receiverUid TEXT,
                text TEXT,
                timestamp INTEGER,
                status TEXT,
                reaction TEXT,
                isEdited INTEGER DEFAULT 0
            )
        """.trimIndent()

        val createNotificationsTable = """
            CREATE TABLE notifications (
                notifId TEXT PRIMARY KEY,
                receiverUid TEXT,
                senderUid TEXT,
                senderName TEXT,
                targetUid TEXT,
                type TEXT,
                message TEXT,
                timestamp INTEGER,
                isRead INTEGER DEFAULT 0
            )
        """.trimIndent()

        val createMarketplaceTable = """
            CREATE TABLE marketplace (
                itemId TEXT PRIMARY KEY,
                sellerUid TEXT,
                title TEXT,
                price TEXT,
                location TEXT,
                description TEXT,
                image TEXT,
                timestamp INTEGER
            )
        """.trimIndent()

        val createStoriesTable = """
            CREATE TABLE stories (
                storyId TEXT PRIMARY KEY,
                userId TEXT,
                caption TEXT,
                mediaUri TEXT,
                timestamp INTEGER
            )
        """.trimIndent()

        val createSyncQueueTable = """
            CREATE TABLE sync_queue (
                queueId TEXT PRIMARY KEY,
                actionType TEXT,
                payloadJson TEXT,
                timestamp INTEGER
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createFriendsTable)
        db.execSQL(createFriendRequestsTable)
        db.execSQL(createPostsTable)
        db.execSQL(createPostReactionsTable)
        db.execSQL(createPostCommentsTable)
        db.execSQL(createChatsTable)
        db.execSQL(createNotificationsTable)
        db.execSQL(createMarketplaceTable)
        db.execSQL(createStoriesTable)
        db.execSQL(createSyncQueueTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS friends")
        db.execSQL("DROP TABLE IF EXISTS friendRequests")
        db.execSQL("DROP TABLE IF EXISTS posts")
        db.execSQL("DROP TABLE IF EXISTS postReactions")
        db.execSQL("DROP TABLE IF EXISTS postComments")
        db.execSQL("DROP TABLE IF EXISTS chats")
        db.execSQL("DROP TABLE IF EXISTS notifications")
        db.execSQL("DROP TABLE IF EXISTS marketplace")
        db.execSQL("DROP TABLE IF EXISTS stories")
        db.execSQL("DROP TABLE IF EXISTS sync_queue")
        onCreate(db)
    }

    // --- USER OPERATIONS ---

    fun insertUser(user: UserData): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("uid", user.uid.ifEmpty { UUID.randomUUID().toString() })
            put("firstName", user.firstName)
            put("lastName", user.lastName)
            put("birthday", user.birthday)
            put("gender", user.gender)
            put("email", user.email)
            put("password", user.password)
            put("username", user.username)
            put("bio", user.bio)
            put("website", user.website)
            put("phone", user.phone)
            put("profilePicUrl", user.profilePicUrl)
            put("coverPhotoUrl", user.coverPhotoUrl)
            put("isOnline", if (user.isOnline) 1 else 0)
            put("fcmToken", user.fcmToken)
        }
        val result = db.insertWithOnConflict("users", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return result != -1L
    }

    fun getUserByEmail(email: String): UserData? {
        val db = readableDatabase
        val cursor = db.query(
            "users", null, "email = ?", arrayOf(email), null, null, null
        )
        var user: UserData? = null
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor)
        }
        cursor.close()
        return user
    }

    fun getUserByUid(uid: String): UserData? {
        val db = readableDatabase
        val cursor = db.query(
            "users", null, "uid = ?", arrayOf(uid), null, null, null
        )
        var user: UserData? = null
        if (cursor.moveToFirst()) {
            user = cursorToUser(cursor)
        }
        cursor.close()
        return user
    }

    fun updateUserProfile(uid: String, updates: Map<String, Any>): Boolean {
        val db = writableDatabase
        val values = ContentValues()
        for ((key, value) in updates) {
            when (value) {
                is String -> values.put(key, value)
                is Int -> values.put(key, value)
                is Long -> values.put(key, value)
                is Boolean -> values.put(key, if (value) 1 else 0)
            }
        }
        val count = db.update("users", values, "uid = ?", arrayOf(uid))
        return count > 0
    }

    fun setOnlineStatus(uid: String, isOnline: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("isOnline", if (isOnline) 1 else 0)
        }
        db.update("users", values, "uid = ?", arrayOf(uid))
    }

    fun getAllUsersExcept(currentUid: String): List<UserData> {
        val db = readableDatabase
        val cursor = db.query("users", null, "uid != ?", arrayOf(currentUid), null, null, null)
        val users = mutableListOf<UserData>()
        while (cursor.moveToNext()) {
            users.add(cursorToUser(cursor))
        }
        cursor.close()
        return users
    }

    fun searchUsers(query: String, currentUid: String): List<UserData> {
        val db = readableDatabase
        val sql = "SELECT * FROM users WHERE (firstName LIKE ? OR lastName LIKE ? OR username LIKE ? OR email LIKE ?)"
        val pattern = "%$query%"
        val cursor = db.rawQuery(sql, arrayOf(pattern, pattern, pattern, pattern))
        val users = mutableListOf<UserData>()
        while (cursor.moveToNext()) {
            users.add(cursorToUser(cursor))
        }
        cursor.close()
        return users
    }

    private fun cursorToUser(cursor: android.database.Cursor): UserData {
        return UserData(
            uid = cursor.getString(cursor.getColumnIndexOrThrow("uid")),
            firstName = cursor.getString(cursor.getColumnIndexOrThrow("firstName")) ?: "",
            lastName = cursor.getString(cursor.getColumnIndexOrThrow("lastName")) ?: "",
            birthday = cursor.getString(cursor.getColumnIndexOrThrow("birthday")) ?: "",
            gender = cursor.getString(cursor.getColumnIndexOrThrow("gender")) ?: "",
            email = cursor.getString(cursor.getColumnIndexOrThrow("email")) ?: "",
            password = cursor.getString(cursor.getColumnIndexOrThrow("password")) ?: "",
            username = cursor.getString(cursor.getColumnIndexOrThrow("username")) ?: "",
            bio = cursor.getString(cursor.getColumnIndexOrThrow("bio")) ?: "",
            website = cursor.getString(cursor.getColumnIndexOrThrow("website")) ?: "",
            phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")) ?: "",
            profilePicUrl = cursor.getString(cursor.getColumnIndexOrThrow("profilePicUrl")) ?: "",
            coverPhotoUrl = cursor.getString(cursor.getColumnIndexOrThrow("coverPhotoUrl")) ?: "",
            isOnline = cursor.getInt(cursor.getColumnIndexOrThrow("isOnline")) == 1,
            fcmToken = cursor.getString(cursor.getColumnIndexOrThrow("fcmToken")) ?: ""
        )
    }

    // --- FRIENDS & FRIEND REQUESTS ---

    fun sendFriendRequest(senderUid: String, receiverUid: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("receiverUid", receiverUid)
            put("senderUid", senderUid)
        }
        val result = db.insertWithOnConflict("friendRequests", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        if (result != -1L) {
            val sender = getUserByUid(senderUid)
            val senderName = if (sender != null) "${sender.firstName} ${sender.lastName}".trim() else "Someone"
            addNotification(
                receiverUid = receiverUid,
                senderUid = senderUid,
                senderName = senderName,
                targetUid = receiverUid,
                type = "friend_request",
                message = "sent you a friend request."
            )
            return true
        }
        return false
    }

    fun isFriend(userUid: String, friendUid: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            "friends", null, "userUid = ? AND friendUid = ?", arrayOf(userUid, friendUid), null, null, null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun hasSentFriendRequest(senderUid: String, receiverUid: String): Boolean {
        val db = readableDatabase
        val cursor = db.query(
            "friendRequests", null, "senderUid = ? AND receiverUid = ?", arrayOf(senderUid, receiverUid), null, null, null
        )
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    fun acceptFriendRequest(currentUid: String, senderUid: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val v1 = ContentValues().apply {
                put("userUid", currentUid)
                put("friendUid", senderUid)
            }
            val v2 = ContentValues().apply {
                put("userUid", senderUid)
                put("friendUid", currentUid)
            }
            db.insertWithOnConflict("friends", null, v1, SQLiteDatabase.CONFLICT_REPLACE)
            db.insertWithOnConflict("friends", null, v2, SQLiteDatabase.CONFLICT_REPLACE)
            db.delete("friendRequests", "receiverUid = ? AND senderUid = ?", arrayOf(currentUid, senderUid))
            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            return false
        } finally {
            db.endTransaction()
        }
    }

    fun deleteFriendRequest(currentUid: String, senderUid: String): Boolean {
        val db = writableDatabase
        return db.delete("friendRequests", "receiverUid = ? AND senderUid = ?", arrayOf(currentUid, senderUid)) > 0
    }

    fun unfriendUser(currentUid: String, targetUid: String): Boolean {
        val db = writableDatabase
        db.delete("friends", "userUid = ? AND friendUid = ?", arrayOf(currentUid, targetUid))
        db.delete("friends", "userUid = ? AND friendUid = ?", arrayOf(targetUid, currentUid))
        return true
    }

    fun getFriendRequestsForUser(currentUid: String): List<UserData> {
        val db = readableDatabase
        val query = """
            SELECT u.* FROM users u 
            INNER JOIN friendRequests fr ON u.uid = fr.senderUid 
            WHERE fr.receiverUid = ?
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(currentUid))
        val senders = mutableListOf<UserData>()
        while (cursor.moveToNext()) {
            senders.add(cursorToUser(cursor))
        }
        cursor.close()
        return senders
    }

    fun getFriendsForUser(currentUid: String): List<UserData> {
        val db = readableDatabase
        val query = """
            SELECT u.* FROM users u 
            INNER JOIN friends f ON u.uid = f.friendUid 
            WHERE f.userUid = ?
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(currentUid))
        val friends = mutableListOf<UserData>()
        while (cursor.moveToNext()) {
            friends.add(cursorToUser(cursor))
        }
        cursor.close()
        return friends
    }

    // --- POSTS, REACTIONS & COMMENTS ---

    fun createPost(authorUid: String, caption: String, imageUri: String): String {
        val db = writableDatabase
        val postId = UUID.randomUUID().toString()
        val values = ContentValues().apply {
            put("postId", postId)
            put("authorUid", authorUid)
            put("caption", caption)
            put("imageUri", imageUri)
            put("timestamp", System.currentTimeMillis())
        }
        db.insert("posts", null, values)
        return postId
    }

    fun insertOrUpdatePost(postId: String, authorUid: String, caption: String, imageUri: String, timestamp: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("postId", if (postId.isNotEmpty()) postId else UUID.randomUUID().toString())
            put("authorUid", authorUid)
            put("caption", caption)
            put("imageUri", imageUri)
            put("timestamp", if (timestamp > 0) timestamp else System.currentTimeMillis())
        }
        db.insertWithOnConflict("posts", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllPosts(currentUid: String = ""): List<PostData> {
        val db = readableDatabase
        val cursor = db.query("posts", null, null, null, null, null, "timestamp DESC")
        val posts = mutableListOf<PostData>()
        while (cursor.moveToNext()) {
            val postId = cursor.getString(cursor.getColumnIndexOrThrow("postId"))
            val authorUid = cursor.getString(cursor.getColumnIndexOrThrow("authorUid"))
            val caption = cursor.getString(cursor.getColumnIndexOrThrow("caption")) ?: ""
            val imageUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri")) ?: ""
            val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
            val reactionsCount = getPostReactionsCount(postId)
            val userReaction = if (currentUid.isNotEmpty()) getUserPostReaction(postId, currentUid) else ""

            posts.add(
                PostData(
                    postId = postId,
                    authorUid = authorUid,
                    caption = caption,
                    imageUri = imageUri,
                    timestamp = timestamp,
                    reactionsCount = reactionsCount,
                    userReaction = userReaction
                )
            )
        }
        cursor.close()
        return posts
    }

    fun getPostsByAuthor(authorUid: String): List<PostData> {
        val db = readableDatabase
        val cursor = db.query("posts", null, "authorUid = ?", arrayOf(authorUid), null, null, "timestamp DESC")
        val posts = mutableListOf<PostData>()
        while (cursor.moveToNext()) {
            val postId = cursor.getString(cursor.getColumnIndexOrThrow("postId"))
            val caption = cursor.getString(cursor.getColumnIndexOrThrow("caption")) ?: ""
            val imageUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri")) ?: ""
            val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))

            posts.add(
                PostData(
                    postId = postId,
                    authorUid = authorUid,
                    caption = caption,
                    imageUri = imageUri,
                    timestamp = timestamp
                )
            )
        }
        cursor.close()
        return posts
    }

    fun setPostReaction(postId: String, userUid: String, type: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("postId", postId)
            put("userUid", userUid)
            put("type", type)
        }
        db.insertWithOnConflict("postReactions", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removePostReaction(postId: String, userUid: String) {
        val db = writableDatabase
        db.delete("postReactions", "postId = ? AND userUid = ?", arrayOf(postId, userUid))
    }

    fun getUserPostReaction(postId: String, userUid: String): String {
        val db = readableDatabase
        val cursor = db.query("postReactions", arrayOf("type"), "postId = ? AND userUid = ?", arrayOf(postId, userUid), null, null, null)
        var type = ""
        if (cursor.moveToFirst()) {
            type = cursor.getString(cursor.getColumnIndexOrThrow("type")) ?: ""
        }
        cursor.close()
        return type
    }

    fun getPostReactionsCount(postId: String): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM postReactions WHERE postId = ?", arrayOf(postId))
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        return count
    }

    fun addPostComment(postId: String, parentCommentId: String, authorUid: String, authorName: String, text: String): String {
        val db = writableDatabase
        val commentId = UUID.randomUUID().toString()
        val values = ContentValues().apply {
            put("commentId", commentId)
            put("postId", postId)
            put("parentCommentId", parentCommentId)
            put("authorUid", authorUid)
            put("authorName", authorName)
            put("text", text)
            put("timestamp", System.currentTimeMillis())
        }
        db.insert("postComments", null, values)
        return commentId
    }

    fun getPostComments(postId: String): List<CommentData> {
        val db = readableDatabase
        val cursor = db.query("postComments", null, "postId = ? AND (parentCommentId IS NULL OR parentCommentId = '')", arrayOf(postId), null, null, "timestamp ASC")
        val comments = mutableListOf<CommentData>()
        while (cursor.moveToNext()) {
            comments.add(cursorToComment(cursor))
        }
        cursor.close()
        return comments
    }

    fun getCommentReplies(postId: String, parentCommentId: String): List<CommentData> {
        val db = readableDatabase
        val cursor = db.query("postComments", null, "postId = ? AND parentCommentId = ?", arrayOf(postId, parentCommentId), null, null, "timestamp ASC")
        val replies = mutableListOf<CommentData>()
        while (cursor.moveToNext()) {
            replies.add(cursorToComment(cursor))
        }
        cursor.close()
        return replies
    }

    private fun cursorToComment(cursor: android.database.Cursor): CommentData {
        return CommentData(
            commentId = cursor.getString(cursor.getColumnIndexOrThrow("commentId")),
            postId = cursor.getString(cursor.getColumnIndexOrThrow("postId")),
            parentCommentId = cursor.getString(cursor.getColumnIndexOrThrow("parentCommentId")) ?: "",
            authorUid = cursor.getString(cursor.getColumnIndexOrThrow("authorUid")),
            authorName = cursor.getString(cursor.getColumnIndexOrThrow("authorName")) ?: "",
            text = cursor.getString(cursor.getColumnIndexOrThrow("text")) ?: "",
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
        )
    }

    // --- CHATS / MESSAGING ---

    fun sendMessage(senderUid: String, receiverUid: String, text: String): String {
        val db = writableDatabase
        val messageId = UUID.randomUUID().toString()
        val chatRoomId = if (senderUid < receiverUid) "${senderUid}_${receiverUid}" else "${receiverUid}_${senderUid}"

        val values = ContentValues().apply {
            put("messageId", messageId)
            put("chatRoomId", chatRoomId)
            put("senderUid", senderUid)
            put("receiverUid", receiverUid)
            put("text", text)
            put("timestamp", System.currentTimeMillis())
            put("status", "sent")
            put("reaction", "")
            put("isEdited", 0)
        }
        db.insert("chats", null, values)

        // Add Notification for receiver
        val sender = getUserByUid(senderUid)
        val senderName = if (sender != null) "${sender.firstName} ${sender.lastName}".trim() else "Someone"
        addNotification(
            receiverUid = receiverUid,
            senderUid = senderUid,
            senderName = senderName,
            targetUid = receiverUid,
            type = "chat",
            message = "sent you a message: \"$text\""
        )

        return messageId
    }

    fun editMessage(messageId: String, newText: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("text", newText)
            put("isEdited", 1)
        }
        db.update("chats", values, "messageId = ?", arrayOf(messageId))
    }

    fun deleteMessage(messageId: String) {
        val db = writableDatabase
        db.delete("chats", "messageId = ?", arrayOf(messageId))
    }

    fun setMessageReaction(messageId: String, reaction: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("reaction", reaction)
        }
        db.update("chats", values, "messageId = ?", arrayOf(messageId))
    }

    fun markIncomingMessagesAsRead(chatRoomId: String, currentUid: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("status", "read")
        }
        db.update("chats", values, "chatRoomId = ? AND receiverUid = ? AND status != 'read'", arrayOf(chatRoomId, currentUid))
    }

    fun getChatMessages(chatRoomId: String): List<ChatMessageData> {
        val db = readableDatabase
        val cursor = db.query("chats", null, "chatRoomId = ?", arrayOf(chatRoomId), null, null, "timestamp ASC")
        val messages = mutableListOf<ChatMessageData>()
        while (cursor.moveToNext()) {
            messages.add(
                ChatMessageData(
                    messageId = cursor.getString(cursor.getColumnIndexOrThrow("messageId")),
                    chatRoomId = cursor.getString(cursor.getColumnIndexOrThrow("chatRoomId")),
                    senderUid = cursor.getString(cursor.getColumnIndexOrThrow("senderUid")),
                    receiverUid = cursor.getString(cursor.getColumnIndexOrThrow("receiverUid")),
                    text = cursor.getString(cursor.getColumnIndexOrThrow("text")) ?: "",
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                    status = cursor.getString(cursor.getColumnIndexOrThrow("status")) ?: "sent",
                    reaction = cursor.getString(cursor.getColumnIndexOrThrow("reaction")) ?: "",
                    isEdited = cursor.getInt(cursor.getColumnIndexOrThrow("isEdited")) == 1
                )
            )
        }
        cursor.close()
        return messages
    }

    // --- NOTIFICATIONS ---

    fun addNotification(
        receiverUid: String,
        senderUid: String,
        senderName: String,
        targetUid: String,
        type: String,
        message: String
    ): String {
        val db = writableDatabase
        val notifId = UUID.randomUUID().toString()
        val values = ContentValues().apply {
            put("notifId", notifId)
            put("receiverUid", receiverUid)
            put("senderUid", senderUid)
            put("senderName", senderName)
            put("targetUid", targetUid)
            put("type", type)
            put("message", message)
            put("timestamp", System.currentTimeMillis())
            put("isRead", 0)
        }
        db.insert("notifications", null, values)
        return notifId
    }

    fun getNotificationsForUser(currentUid: String): List<NotificationData> {
        val db = readableDatabase
        val cursor = db.query("notifications", null, "receiverUid = ?", arrayOf(currentUid), null, null, "timestamp DESC")
        val notifs = mutableListOf<NotificationData>()
        while (cursor.moveToNext()) {
            notifs.add(
                NotificationData(
                    notifId = cursor.getString(cursor.getColumnIndexOrThrow("notifId")),
                    receiverUid = cursor.getString(cursor.getColumnIndexOrThrow("receiverUid")),
                    senderUid = cursor.getString(cursor.getColumnIndexOrThrow("senderUid")),
                    senderName = cursor.getString(cursor.getColumnIndexOrThrow("senderName")) ?: "",
                    targetUid = cursor.getString(cursor.getColumnIndexOrThrow("targetUid")) ?: "",
                    type = cursor.getString(cursor.getColumnIndexOrThrow("type")) ?: "",
                    message = cursor.getString(cursor.getColumnIndexOrThrow("message")) ?: "",
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp")),
                    isRead = cursor.getInt(cursor.getColumnIndexOrThrow("isRead")) == 1
                )
            )
        }
        cursor.close()
        return notifs
    }

    // --- MARKETPLACE ---

    fun createMarketItem(sellerUid: String, title: String, price: String, location: String, description: String, image: String): String {
        val db = writableDatabase
        val itemId = UUID.randomUUID().toString()
        val values = ContentValues().apply {
            put("itemId", itemId)
            put("sellerUid", sellerUid)
            put("title", title)
            put("price", price)
            put("location", location)
            put("description", description)
            put("image", image)
            put("timestamp", System.currentTimeMillis())
        }
        db.insert("marketplace", null, values)
        return itemId
    }

    fun getAllMarketItems(): List<MarketItemData> {
        val db = readableDatabase
        val cursor = db.query("marketplace", null, null, null, null, null, "timestamp DESC")
        val items = mutableListOf<MarketItemData>()
        while (cursor.moveToNext()) {
            items.add(
                MarketItemData(
                    itemId = cursor.getString(cursor.getColumnIndexOrThrow("itemId")),
                    sellerUid = cursor.getString(cursor.getColumnIndexOrThrow("sellerUid")),
                    title = cursor.getString(cursor.getColumnIndexOrThrow("title")) ?: "",
                    price = cursor.getString(cursor.getColumnIndexOrThrow("price")) ?: "",
                    location = cursor.getString(cursor.getColumnIndexOrThrow("location")) ?: "",
                    description = cursor.getString(cursor.getColumnIndexOrThrow("description")) ?: "",
                    image = cursor.getString(cursor.getColumnIndexOrThrow("image")) ?: "",
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                )
            )
        }
        cursor.close()
        return items
    }

    // --- STORIES ---

    fun createStory(userId: String, caption: String, mediaUri: String): String {
        val db = writableDatabase
        val storyId = UUID.randomUUID().toString()
        val values = ContentValues().apply {
            put("storyId", storyId)
            put("userId", userId)
            put("caption", caption)
            put("mediaUri", mediaUri)
            put("timestamp", System.currentTimeMillis())
        }
        db.insert("stories", null, values)
        return storyId
    }

    fun getLatestUserStory(userId: String): StoryData? {
        val db = readableDatabase
        val cursor = db.query("stories", null, "userId = ?", arrayOf(userId), null, null, "timestamp DESC", "1")
        var story: StoryData? = null
        if (cursor.moveToFirst()) {
            story = StoryData(
                storyId = cursor.getString(cursor.getColumnIndexOrThrow("storyId")),
                userId = cursor.getString(cursor.getColumnIndexOrThrow("userId")),
                caption = cursor.getString(cursor.getColumnIndexOrThrow("caption")) ?: "",
                mediaUri = cursor.getString(cursor.getColumnIndexOrThrow("mediaUri")) ?: "",
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
            )
        }
        cursor.close()
        return story
    }

    fun getStoryById(storyId: String): StoryData? {
        val db = readableDatabase
        val cursor = db.query("stories", null, "storyId = ?", arrayOf(storyId), null, null, null)
        var story: StoryData? = null
        if (cursor.moveToFirst()) {
            story = StoryData(
                storyId = cursor.getString(cursor.getColumnIndexOrThrow("storyId")),
                userId = cursor.getString(cursor.getColumnIndexOrThrow("userId")),
                caption = cursor.getString(cursor.getColumnIndexOrThrow("caption")) ?: "",
                mediaUri = cursor.getString(cursor.getColumnIndexOrThrow("mediaUri")) ?: "",
                timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
            )
        }
        cursor.close()
        return story
    }

    // --- OFFLINE SYNC QUEUE ---

    fun addToSyncQueue(actionType: String, payloadJson: String): String {
        val db = writableDatabase
        val queueId = UUID.randomUUID().toString()
        val values = ContentValues().apply {
            put("queueId", queueId)
            put("actionType", actionType)
            put("payloadJson", payloadJson)
            put("timestamp", System.currentTimeMillis())
        }
        db.insert("sync_queue", null, values)
        return queueId
    }

    fun getSyncQueue(): List<SyncQueueData> {
        val db = readableDatabase
        val cursor = db.query("sync_queue", null, null, null, null, null, "timestamp ASC")
        val queue = mutableListOf<SyncQueueData>()
        while (cursor.moveToNext()) {
            queue.add(
                SyncQueueData(
                    queueId = cursor.getString(cursor.getColumnIndexOrThrow("queueId")),
                    actionType = cursor.getString(cursor.getColumnIndexOrThrow("actionType")) ?: "",
                    payloadJson = cursor.getString(cursor.getColumnIndexOrThrow("payloadJson")) ?: "",
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                )
            )
        }
        cursor.close()
        return queue
    }

    fun removeFromSyncQueue(queueId: String): Boolean {
        val db = writableDatabase
        return db.delete("sync_queue", "queueId = ?", arrayOf(queueId)) > 0
    }
}