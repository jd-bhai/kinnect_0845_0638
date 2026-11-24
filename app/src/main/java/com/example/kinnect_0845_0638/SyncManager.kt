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
                            val authorUid = pObj.optString("authorUid")
                            val caption = pObj.optString("caption")
                            val imageUri = pObj.optString("imageUri")
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

                // 3. Fetch Friend Requests & Friends from PHP API -> Cache in SQLite
                if (currentUid.isNotEmpty()) {
                    val reqsResponse = ApiClient.get("friends.php", mapOf("action" to "get_requests", "currentUid" to currentUid))
                    if (!reqsResponse.isNullOrEmpty()) {
                        val jsonObj = JSONObject(reqsResponse)
                        if (jsonObj.optString("status") == "success") {
                            val reqsArray = jsonObj.optJSONArray("requests") ?: JSONArray()
                            for (i in 0 until reqsArray.length()) {
                                val rObj = reqsArray.getJSONObject(i)
                                val senderUid = rObj.optString("uid")
                                if (senderUid.isNotEmpty()) {
                                    dbHelper.insertUser(
                                        UserData(
                                            uid = senderUid,
                                            firstName = rObj.optString("firstName"),
                                            lastName = rObj.optString("lastName"),
                                            email = rObj.optString("email")
                                        )
                                    )
                                    dbHelper.sendFriendRequest(senderUid, currentUid)
                                }
                            }
                        }
                    }

                    val friendsResponse = ApiClient.get("friends.php", mapOf("action" to "get_friends", "currentUid" to currentUid))
                    if (!friendsResponse.isNullOrEmpty()) {
                        val jsonObj = JSONObject(friendsResponse)
                        if (jsonObj.optString("status") == "success") {
                            val friendsArray = jsonObj.optJSONArray("friends") ?: JSONArray()
                            for (i in 0 until friendsArray.length()) {
                                val fObj = friendsArray.getJSONObject(i)
                                val friendUid = fObj.optString("uid")
                                if (friendUid.isNotEmpty()) {
                                    dbHelper.insertUser(
                                        UserData(
                                            uid = friendUid,
                                            firstName = fObj.optString("firstName"),
                                            lastName = fObj.optString("lastName"),
                                            email = fObj.optString("email")
                                        )
                                    )
                                    dbHelper.acceptFriendRequest(currentUid, friendUid)
                                }
                            }
                        }
                    }

                    // 4. Fetch Notifications from PHP API -> Cache in SQLite
                    val notifResponse = ApiClient.get("notifications.php", mapOf("action" to "get", "currentUid" to currentUid))
                    if (!notifResponse.isNullOrEmpty()) {
                        val jsonObj = JSONObject(notifResponse)
                        if (jsonObj.optString("status") == "success") {
                            val notifArray = jsonObj.optJSONArray("notifications") ?: JSONArray()
                            for (i in 0 until notifArray.length()) {
                                val nObj = notifArray.getJSONObject(i)
                                dbHelper.addNotification(
                                    receiverUid = nObj.optString("receiverUid"),
                                    senderUid = nObj.optString("senderUid"),
                                    senderName = nObj.optString("senderName"),
                                    targetUid = nObj.optString("targetUid"),
                                    type = nObj.optString("type"),
                                    message = nObj.optString("message")
                                )
                            }
                        }
                    }
                }

                // 5. Fetch Marketplace Items from PHP API -> Cache in SQLite
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

    fun enqueueOrSendFriendRequest(context: Context, senderUid: String, receiverUid: String) {
        val dbHelper = DatabaseHelper.getInstance(context)
        dbHelper.sendFriendRequest(senderUid, receiverUid)

        val payload = JSONObject().apply {
            put("senderUid", senderUid)
            put("receiverUid", receiverUid)
        }

        if (NetworkManager.isOnline(context)) {
            Thread {
                val params = mapOf(
                    "action" to "send_request",
                    "senderUid" to senderUid,
                    "receiverUid" to receiverUid
                )
                val res = ApiClient.post("friends.php", params)
                if (res == null) {
                    dbHelper.addToSyncQueue("send_friend_request", payload.toString())
                }
            }.start()
        } else {
            dbHelper.addToSyncQueue("send_friend_request", payload.toString())
        }
    }

    fun enqueueOrAcceptFriendRequest(context: Context, currentUid: String, senderUid: String) {
        val dbHelper = DatabaseHelper.getInstance(context)
        dbHelper.acceptFriendRequest(currentUid, senderUid)

        if (NetworkManager.isOnline(context)) {
            Thread {
                val params = mapOf(
                    "action" to "accept_request",
                    "currentUid" to currentUid,
                    "senderUid" to senderUid
                )
                ApiClient.post("friends.php", params)
            }.start()
        }
    }
}