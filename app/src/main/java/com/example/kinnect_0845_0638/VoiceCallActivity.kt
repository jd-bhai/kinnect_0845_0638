package com.example.kinnect_0845_0638

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
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
        private const val AGORA_APP_ID = "fcee5fa337474679a3dc7fda7774fa96"
        private const val PERMISSION_REQ_ID = 23
    }

    private var channelName: String = "kinnect_call"
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

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isCallConnected) {
                secondsElapsed++
                val minutes = secondsElapsed / 60
                val secs = secondsElapsed % 60
                tvCallTimer.text = String.format("%02d:%02d", minutes, secs)
                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                isCallConnected = true
                secondsElapsed = 0
                timerHandler.post(timerRunnable)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                isCallConnected = false
                tvCallTimer.text = "Call Ended"
                timerHandler.removeCallbacks(timerRunnable)
                Handler(Looper.getMainLooper()).postDelayed({ finish() }, 1500)
            }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                tvCallTimer.text = "Calling..."
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_call)

        channelName = intent.getStringExtra("CHANNEL_NAME") ?: "kinnect_call"
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

        btnMute.setOnClickListener {
            isMuted = !isMuted
            rtcEngine?.muteLocalAudioStream(isMuted)
            btnMute.isActivated = isMuted
            Toast.makeText(this, if (isMuted) "Microphone Muted" else "Microphone Unmuted", Toast.LENGTH_SHORT).show()
        }

        btnSpeaker.setOnClickListener {
            isSpeakerOn = !isSpeakerOn
            rtcEngine?.setEnableSpeakerphone(isSpeakerOn)
            btnSpeaker.isActivated = isSpeakerOn
            Toast.makeText(this, if (isSpeakerOn) "Speakerphone On" else "Speakerphone Off", Toast.LENGTH_SHORT).show()
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

            rtcEngine?.joinChannel(null, channelName, "", 0)
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing voice call: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun leaveCall() {
        isCallConnected = false
        timerHandler.removeCallbacks(timerRunnable)
        rtcEngine?.leaveChannel()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        isCallConnected = false
        timerHandler.removeCallbacks(timerRunnable)
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
    }
}