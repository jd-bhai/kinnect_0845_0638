package com.example.kinnect_0845_0638

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig

class VoiceCallActivity : AppCompatActivity() {

    companion object {
        private const val AGORA_APP_ID = "6f7694fccffb470bbc70bb634eaf2767"
        private const val AGORA_TOKEN = "007eJxTYOjNNNq8weurt0eKv7L0o0pb/w+dNreN65rrZvr6WTFntCgwmKWZm1mapCUnp6UlmZgbJCUlgwgzY5PUxDQjczPz7GMs2Q2BjAxXtRazMDJAIIjPyZCcmJMTn5ibm8nAAAB86iAH"
        private const val PERMISSION_REQ_ID = 23
        private const val CALL_TIMEOUT_MS = 15000L
    }

    private var channelName: String = "call_ammi"
    private var receiverName: String = "User"
    private var receiverInitials: String = "U"
    private var rtcEngine: RtcEngine? = null

    private lateinit var tvUserInitials: TextView
    private lateinit var tvUserName: TextView
    private lateinit var tvCallTimer: TextView
    private lateinit var btnMute: ImageButton
    private lateinit var btnSpeaker: ImageButton
    private lateinit var btnSwitchToVideo: ImageButton
    private lateinit var btnEndCall: ImageButton

    private var isMuted = false
    private var isSpeakerOn = true
    private var secondsElapsed = 0
    private var isCallConnected = false
    private var isEndingCall = false

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            try {
                if (isCallConnected && !isEndingCall) {
                    secondsElapsed++
                    val minutes = secondsElapsed / 60
                    val secs = secondsElapsed % 60
                    tvCallTimer.text = String.format("%02d:%02d", minutes, secs)
                    timerHandler.postDelayed(this, 1000)
                }
            } catch (_: Exception) {}
        }
    }

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        if (!isCallConnected && !isEndingCall) {
            Toast.makeText(this, "Call unanswered", Toast.LENGTH_SHORT).show()
            leaveCall()
        }
    }

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                try {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    isCallConnected = true
                    secondsElapsed = 0
                    tvCallTimer.setTextColor(Color.WHITE)
                    timerHandler.removeCallbacks(timerRunnable)
                    timerHandler.post(timerRunnable)
                } catch (_: Exception) {}
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                try {
                    isCallConnected = false
                    timerHandler.removeCallbacks(timerRunnable)
                    tvCallTimer.text = "Disconnected"
                    tvCallTimer.setTextColor(Color.parseColor("#D93025"))
                    Handler(Looper.getMainLooper()).postDelayed({ finish() }, 500)
                } catch (_: Exception) {}
            }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                try {
                    if (!isCallConnected) {
                        tvCallTimer.text = "Calling..."
                        tvCallTimer.setTextColor(Color.parseColor("#D1E8E6"))
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_call)

        channelName = intent.getStringExtra("CHANNEL_NAME") ?: "call_ammi"
        receiverName = intent.getStringExtra("RECEIVER_NAME") ?: "User"
        receiverInitials = intent.getStringExtra("RECEIVER_INITIALS") ?: "U"

        tvUserInitials = findViewById(R.id.tv_user_initials)
        tvUserName = findViewById(R.id.tv_user_name)
        tvCallTimer = findViewById(R.id.tv_call_timer)
        btnMute = findViewById(R.id.btn_mute)
        btnSpeaker = findViewById(R.id.btn_speaker)
        btnSwitchToVideo = findViewById(R.id.btn_switch_to_video)
        btnEndCall = findViewById(R.id.btn_end_call)

        tvUserInitials.text = receiverInitials
        tvUserName.text = receiverName
        tvCallTimer.text = "Calling..."
        tvCallTimer.setTextColor(Color.parseColor("#D1E8E6"))

        timeoutHandler.postDelayed(timeoutRunnable, CALL_TIMEOUT_MS)

        btnMute.setOnClickListener {
            try {
                isMuted = !isMuted
                rtcEngine?.muteLocalAudioStream(isMuted)
                btnMute.isActivated = isMuted
                Toast.makeText(this, if (isMuted) "Microphone Muted" else "Microphone Unmuted", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }

        btnSpeaker.setOnClickListener {
            try {
                isSpeakerOn = !isSpeakerOn
                rtcEngine?.setEnableSpeakerphone(isSpeakerOn)
                btnSpeaker.isActivated = isSpeakerOn
                Toast.makeText(this, if (isSpeakerOn) "Speakerphone On" else "Speakerphone Off", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }

        btnSwitchToVideo.setOnClickListener {
            leaveCall()
            val intent = Intent(this, VideoCallActivity::class.java).apply {
                putExtra("CHANNEL_NAME", channelName)
                putExtra("RECEIVER_NAME", receiverName)
            }
            startActivity(intent)
        }

        btnEndCall.setOnClickListener {
            leaveCall()
        }

        if (checkPermissions()) {
            initAgoraEngineAndJoinChannel()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQ_ID
            )
        }
    }

    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initAgoraEngineAndJoinChannel()
            } else {
                Toast.makeText(this, "Microphone permission required for voice call", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun initAgoraEngineAndJoinChannel() {
        try {
            val config = RtcEngineConfig()
            config.mContext = applicationContext
            config.mAppId = AGORA_APP_ID
            config.mEventHandler = rtcEventHandler
            config.mChannelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION

            rtcEngine = RtcEngine.create(config)
            rtcEngine?.disableVideo()
            rtcEngine?.enableAudio()
            rtcEngine?.setEnableSpeakerphone(true)

            val tokenToUse = intent.getStringExtra("AGORA_TOKEN")
            val finalToken = if (!tokenToUse.isNullOrEmpty()) tokenToUse else AGORA_TOKEN
            rtcEngine?.joinChannel(finalToken, channelName, "", 0)
        } catch (e: Exception) {
            Log.e("VoiceCallActivity", "Error initializing voice call: ${e.message}", e)
            Toast.makeText(this, "Unable to initialize voice call: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun leaveCall() {
        if (isEndingCall) return
        isEndingCall = true
        isCallConnected = false

        timeoutHandler.removeCallbacks(timeoutRunnable)
        timerHandler.removeCallbacks(timerRunnable)

        tvCallTimer.text = "Disconnected"
        tvCallTimer.setTextColor(Color.parseColor("#D93025"))

        try {
            rtcEngine?.leaveChannel()
        } catch (_: Exception) {}

        Handler(Looper.getMainLooper()).postDelayed({
            finish()
        }, 500)
    }

    override fun onDestroy() {
        super.onDestroy()
        isEndingCall = true
        isCallConnected = false
        timeoutHandler.removeCallbacks(timeoutRunnable)
        timerHandler.removeCallbacks(timerRunnable)
        try {
            rtcEngine?.leaveChannel()
            RtcEngine.destroy()
        } catch (_: Exception) {}
        rtcEngine = null
    }
}