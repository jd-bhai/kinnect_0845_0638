package com.example.kinnect_0845_0638

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object SyncManager {

    fun performSync(context: Context, onComplete: (() -> Unit)? = null) {
        val dbHelper = DatabaseHelper.getInstance(context)
        val currentUid = SessionManager.getCurrentUserUid(context) ?: ""

        if (!NetworkManager.isOnline(context)) {
            onComplete?.invoke()
            return
        }

        Thread {
            try {
                // 1. Process Offline Sync Queue first
                val queue = dbHelper.getSyncQueue()
                for (item in queue) {
                    val processed = processQueueItem(item)
                    if (processed) {
                        dbHelper.removeFromSyncQueue(item.queueId)
                    }
                }

                // 2. Fetch Latest Posts from PHP API -> Cache in SQLite
                val postsResponse = ApiClient.get("posts.php", mapOf("action" to "get_all", "currentUid" to currentUid))
                if (!postsResponse.isNullOrEmpty()) {
                    val jsonObj = JSONObject(postsResponse)
                    if (jsonObj.optString("status") == "success") {
                        val postsArray = jsonObj.optJSONArray("posts") ?: JSONArray()
                        for (i in 0 until postsArray.length()) {
                            val pObj = postsArray.getJSONObject(i)
                            val postId = pObj.optString("postId")
                            val authorUid = pObj.optString("authorUid")
                            val caption = pObj.optString("caption")
                            val imageUri = pObj.optString("imageUri")
                            val timestamp = pObj.optLong("timestamp")
                            val firstName = pObj.optString("firstName")
                            val lastName = pObj.optString("lastName")
                            val profilePicUrl = pObj.optString("profilePicUrl")

                            if (authorUid.isNotEmpty()) {
                                dbHelper.insertUser(
                                    UserData(
                                        uid = authorUid,
                                        firstName = firstName,
                                        lastName = lastName,
                                        profilePicUrl = profilePicUrl
                                    )
                                )
                            }
                            dbHelper.createPost(authorUid, caption, imageUri)
                        }
                    }
                }

                // 3. Fetch Marketplace Items from PHP API -> Cache in SQLite
                val marketResponse = ApiClient.get("marketplace.php", mapOf("action" to "get_all"))
                if (!marketResponse.isNullOrEmpty()) {
                    val jsonObj = JSONObject(marketResponse)
                    if (jsonObj.optString("status") == "success") {
                        val itemsArray = jsonObj.optJSONArray("items") ?: JSONArray()
                        for (i in 0 until itemsArray.length()) {
                            val mObj = itemsArray.getJSONObject(i)
                            dbHelper.createMarketItem(
                                mObj.optString("sellerUid"),
                                mObj.optString("title"),
                                mObj.optString("price"),
                                mObj.optString("location"),
                                mObj.optString("description"),
                                mObj.optString("image")
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                onComplete?.invoke()
            }
        }.start()
    }

    private fun processQueueItem(item: SyncQueueData): Boolean {
        try {
            val json = JSONObject(item.payloadJson)
            val params = mutableMapOf<String, String>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                params[key] = json.optString(key)
            }

            return when (item.actionType) {
                "create_post" -> {
                    params["action"] = "create"
                    val res = ApiClient.post("posts.php", params)
                    res != null && JSONObject(res).optString("status") == "success"
                }
                "send_chat" -> {
                    params["action"] = "send"
                    val res = ApiClient.post("chats.php", params)
                    res != null && JSONObject(res).optString("status") == "success"
                }
                "react_post" -> {
                    params["action"] = "react"
                    val res = ApiClient.post("posts.php", params)
                    res != null && JSONObject(res).optString("status") == "success"
                }
                "add_comment" -> {
                    params["action"] = "add_comment"
                    val res = ApiClient.post("posts.php", params)
                    res != null && JSONObject(res).optString("status") == "success"
                }
                "create_market_item" -> {
                    params["action"] = "create"
                    val res = ApiClient.post("marketplace.php", params)
                    res != null && JSONObject(res).optString("status") == "success"
                }
                "create_story" -> {
                    params["action"] = "create"
                    val res = ApiClient.post("stories.php", params)
                    res != null && JSONObject(res).optString("status") == "success"
                }
                "send_friend_request" -> {
                    params["action"] = "send_request"
                    val res = ApiClient.post("friends.php", params)
                    res != null && JSONObject(res).optString("status") == "success"
                }
                else -> true
            }
        } catch (e: Exception) {
            return false
        }
    }

    fun enqueueOrSendPost(context: Context, authorUid: String, caption: String, imageUri: String) {
        val dbHelper = DatabaseHelper.getInstance(context)
        val postId = dbHelper.createPost(authorUid, caption, imageUri)

        val payload = JSONObject().apply {
            put("postId", postId)
            put("authorUid", authorUid)
            put("caption", caption)
            put("imageUri", imageUri)
            put("timestamp", System.currentTimeMillis())
        }

        if (NetworkManager.isOnline(context)) {
            Thread {
                val params = mapOf(
                    "action" to "create",
                    "postId" to postId,
                    "authorUid" to authorUid,
                    "caption" to caption,
                    "imageUri" to imageUri,
                    "timestamp" to System.currentTimeMillis().toString()
                )
                val res = ApiClient.post("posts.php", params)
                if (res == null) {
                    dbHelper.addToSyncQueue("create_post", payload.toString())
                }
            }.start()
        } else {
            dbHelper.addToSyncQueue("create_post", payload.toString())
        }
    }

    fun enqueueOrSendMessage(context: Context, senderUid: String, receiverUid: String, text: String) {
        val dbHelper = DatabaseHelper.getInstance(context)
        val messageId = dbHelper.sendMessage(senderUid, receiverUid, text)

        val payload = JSONObject().apply {
            put("messageId", messageId)
            put("senderUid", senderUid)
            put("receiverUid", receiverUid)
            put("text", text)
            put("timestamp", System.currentTimeMillis())
        }

        if (NetworkManager.isOnline(context)) {
            Thread {
                val params = mapOf(
                    "action" to "send",
                    "messageId" to messageId,
                    "senderUid" to senderUid,
                    "receiverUid" to receiverUid,
                    "text" to text,
                    "timestamp" to System.currentTimeMillis().toString()
                )
                val res = ApiClient.post("chats.php", params)
                if (res == null) {
                    dbHelper.addToSyncQueue("send_chat", payload.toString())
                }
            }.start()
        } else {
            dbHelper.addToSyncQueue("send_chat", payload.toString())
        }
    }
}