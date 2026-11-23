package com.example.kinnect_0845_0638

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.View
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
import io.agora.rtc2.video.VideoCanvas

class VideoCallActivity : AppCompatActivity() {

    companion object {
        private const val AGORA_APP_ID = "6f7694fccffb470bbc70bb634eaf2767"
        private const val AGORA_TOKEN = "007eJxTYOjNNNq8weurt0eKv7L0o0pb/w+dNreN65rrZvr6WTFntCgwmKWZm1mapCUnp6UlmZgbJCUlgwgzY5PUxDQjczPz7GMs2Q2BjAxXtRazMDJAIIjPyZCcmJMTn5ibm8nAAAB86iAH"
        private const val PERMISSION_REQ_ID = 22
        private const val CALL_TIMEOUT_MS = 15000L
    }

    private var channelName: String = "call_ammi"
    private var rtcEngine: RtcEngine? = null

    private lateinit var flLocalVideo: FrameLayout
    private lateinit var flRemoteVideo: FrameLayout
    private lateinit var tvCallStatus: TextView
    private lateinit var tvVideoCallTimer: TextView
    private lateinit var btnMute: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var btnEndCall: ImageButton

    private var isMuted = false
    private var isCallConnected = false
    private var isEndingCall = false
    private var secondsElapsed = 0
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isCallConnected && !isEndingCall) {
                secondsElapsed++
                val minutes = secondsElapsed / 60
                val secs = secondsElapsed % 60
                tvVideoCallTimer.text = String.format("%02d:%02d", minutes, secs)
                timerHandler.postDelayed(this, 1000)
            }
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
                timeoutHandler.removeCallbacks(timeoutRunnable)
                isCallConnected = true
                tvCallStatus.text = "Connected"
                tvCallStatus.setTextColor(Color.parseColor("#27AE60"))
                tvVideoCallTimer.visibility = View.VISIBLE
                secondsElapsed = 0
                timerHandler.removeCallbacks(timerRunnable)
                timerHandler.post(timerRunnable)
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                isCallConnected = false
                timerHandler.removeCallbacks(timerRunnable)
                tvVideoCallTimer.visibility = View.GONE
                tvCallStatus.text = "Disconnected"
                tvCallStatus.setTextColor(Color.parseColor("#D93025"))
                flRemoteVideo.removeAllViews()
                Handler(Looper.getMainLooper()).postDelayed({ finish() }, 500)
            }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                if (!isCallConnected) {
                    tvCallStatus.text = "Calling..."
                    tvCallStatus.setTextColor(Color.parseColor("#D1E8E6"))
                }
            }
        }

        override fun onError(err: Int) {
            runOnUiThread {
                if (err == 110) {
                    Toast.makeText(this@VideoCallActivity, "Token Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        channelName = intent.getStringExtra("CHANNEL_NAME") ?: "call_ammi"
        val receiverName = intent.getStringExtra("RECEIVER_NAME") ?: "User"

        flLocalVideo = findViewById(R.id.fl_local_video)
        flRemoteVideo = findViewById(R.id.fl_remote_video)
        tvCallStatus = findViewById(R.id.tv_call_status)
        tvVideoCallTimer = findViewById(R.id.tv_video_call_timer)
        btnMute = findViewById(R.id.btn_mute)
        btnSwitchCamera = findViewById(R.id.btn_switch_camera)
        btnEndCall = findViewById(R.id.btn_end_call)

        val tvCallUserName = findViewById<TextView>(R.id.tv_call_user_name)
        tvCallUserName?.text = receiverName
        tvCallStatus.text = "Calling..."
        tvCallStatus.setTextColor(Color.parseColor("#D1E8E6"))
        tvVideoCallTimer.visibility = View.GONE

        timeoutHandler.postDelayed(timeoutRunnable, CALL_TIMEOUT_MS)

        btnMute.setOnClickListener {
            isMuted = !isMuted
            rtcEngine?.muteLocalAudioStream(isMuted)
            btnMute.isActivated = isMuted
            Toast.makeText(this, if (isMuted) "Microphone Muted" else "Microphone Unmuted", Toast.LENGTH_SHORT).show()
        }

        btnSwitchCamera.setOnClickListener {
            rtcEngine?.switchCamera()
        }

        btnEndCall.setOnClickListener {
            leaveCall()
        }

        if (checkPermissions()) {
            initAgoraEngineAndJoinChannel()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQ_ID
            )
        }
    }

    private fun checkPermissions(): Boolean {
        val cameraGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val audioGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        return cameraGranted && audioGranted
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQ_ID) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initAgoraEngineAndJoinChannel()
            } else {
                Toast.makeText(this, "Camera and Microphone permissions required for video call", Toast.LENGTH_LONG).show()
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
            rtcEngine?.enableVideo()

            setupLocalVideo()

            val tokenToUse = intent.getStringExtra("AGORA_TOKEN") ?: AGORA_TOKEN
            rtcEngine?.joinChannel(tokenToUse, channelName, "", 0)
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing call: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupLocalVideo() {
        localSurfaceView = SurfaceView(baseContext)
        localSurfaceView?.setZOrderMediaOverlay(true)
        flLocalVideo.addView(localSurfaceView)

        rtcEngine?.setupLocalVideo(
            VideoCanvas(localSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        )
        rtcEngine?.startPreview()
    }

    private fun setupRemoteVideo(uid: Int) {
        if (flRemoteVideo.childCount >= 1) {
            flRemoteVideo.removeAllViews()
        }
        remoteSurfaceView = SurfaceView(baseContext).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        flRemoteVideo.addView(remoteSurfaceView)

        rtcEngine?.setupRemoteVideo(
            VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        )
    }

    private fun leaveCall() {
        if (isEndingCall) return
        isEndingCall = true
        isCallConnected = false

        timeoutHandler.removeCallbacks(timeoutRunnable)
        timerHandler.removeCallbacks(timerRunnable)

        tvVideoCallTimer.visibility = View.GONE
        tvCallStatus.text = "Disconnected"
        tvCallStatus.setTextColor(Color.parseColor("#D93025"))

        rtcEngine?.stopPreview()
        rtcEngine?.leaveChannel()
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
        rtcEngine?.stopPreview()
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
    }
}