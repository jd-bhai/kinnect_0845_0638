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
    val isOnline: Boolean = false
)

data class PostData(
    val postId: String = "",
    val authorUid: String = "",
    val caption: String = "",
    val imageUri: String = "",
    val timestamp: Long = System.currentTimeMillis()
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

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "kinnect_sqlite.db"
        private const val DATABASE_VERSION = 1

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
                isOnline INTEGER DEFAULT 0
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

        db.execSQL(createUsersTable)
        db.execSQL(createFriendsTable)
        db.execSQL(createFriendRequestsTable)
        db.execSQL(createPostsTable)
        db.execSQL(createMarketplaceTable)
        db.execSQL(createStoriesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        db.execSQL("DROP TABLE IF EXISTS friends")
        db.execSQL("DROP TABLE IF EXISTS friendRequests")
        db.execSQL("DROP TABLE IF EXISTS posts")
        db.execSQL("DROP TABLE IF EXISTS marketplace")
        db.execSQL("DROP TABLE IF EXISTS stories")
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
            isOnline = cursor.getInt(cursor.getColumnIndexOrThrow("isOnline")) == 1
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
        return result != -1L
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

    // --- POSTS ---

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

    fun getAllPosts(): List<PostData> {
        val db = readableDatabase
        val cursor = db.query("posts", null, null, null, null, null, "timestamp DESC")
        val posts = mutableListOf<PostData>()
        while (cursor.moveToNext()) {
            posts.add(
                PostData(
                    postId = cursor.getString(cursor.getColumnIndexOrThrow("postId")),
                    authorUid = cursor.getString(cursor.getColumnIndexOrThrow("authorUid")),
                    caption = cursor.getString(cursor.getColumnIndexOrThrow("caption")) ?: "",
                    imageUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri")) ?: "",
                    timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
                )
            )
        }
        cursor.close()
        return posts
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
}