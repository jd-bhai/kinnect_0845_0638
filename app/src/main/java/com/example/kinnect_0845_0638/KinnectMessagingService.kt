package com.example.kinnect_0845_0638

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class KinnectMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "kinnect_push_channel"
        private const val CHANNEL_NAME = "Kinnect Push Notifications"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val currentUid = SessionManager.getCurrentUserUid(this)
        if (currentUid != null) {
            DatabaseHelper.getInstance(this).updateUserProfile(currentUid, mapOf("fcmToken" to token))
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Kinnect"

        val message = remoteMessage.notification?.body
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["body"]
            ?: "New notification received"

        val type = remoteMessage.data["type"] ?: ""
        val senderUid = remoteMessage.data["senderUid"] ?: ""
        val senderName = remoteMessage.data["senderName"] ?: "Someone"

        showNotification(title, message, type, senderUid, senderName)
    }

    private fun showNotification(
        title: String,
        message: String,
        type: String,
        senderUid: String,
        senderName: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kinnect push notification channel"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = when (type) {
            "chat" -> Intent(this, ChatActivity::class.java).apply {
                putExtra("RECEIVER_UID", senderUid)
                putExtra("RECEIVER_NAME", senderName)
            }
            "friend_request" -> Intent(this, FriendsActivity::class.java)
            else -> Intent(this, HomeFeedActivity::class.java)
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}