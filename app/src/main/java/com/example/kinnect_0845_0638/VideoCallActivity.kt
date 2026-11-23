package com.example.kinnect_0845_0638

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
        private const val AGORA_APP_ID = "fcee5fa337474679a3dc7fda7774fa96"
        private const val PERMISSION_REQ_ID = 22
    }

    private var channelName: String = "kinnect_call"
    private var rtcEngine: RtcEngine? = null

    private lateinit var flLocalVideo: FrameLayout
    private lateinit var flRemoteVideo: FrameLayout
    private lateinit var tvCallStatus: TextView
    private lateinit var btnMute: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var btnEndCall: ImageButton

    private var isMuted = false
    private var localSurfaceView: SurfaceView? = null
    private var remoteSurfaceView: SurfaceView? = null

    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                tvCallStatus.text = "Connected"
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                tvCallStatus.text = "User disconnected"
                flRemoteVideo.removeAllViews()
            }
        }

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                tvCallStatus.text = "Waiting for other user..."
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_call)

        channelName = intent.getStringExtra("CHANNEL_NAME") ?: "kinnect_call"

        flLocalVideo = findViewById(R.id.fl_local_video)
        flRemoteVideo = findViewById(R.id.fl_remote_video)
        tvCallStatus = findViewById(R.id.tv_call_status)
        btnMute = findViewById(R.id.btn_mute)
        btnSwitchCamera = findViewById(R.id.btn_switch_camera)
        btnEndCall = findViewById(R.id.btn_end_call)

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

            rtcEngine?.joinChannel(null, channelName, "", 0)
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
        remoteSurfaceView = SurfaceView(baseContext)
        flRemoteVideo.addView(remoteSurfaceView)

        rtcEngine?.setupRemoteVideo(
            VideoCanvas(remoteSurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        )
    }

    private fun leaveCall() {
        rtcEngine?.stopPreview()
        rtcEngine?.leaveChannel()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        rtcEngine?.stopPreview()
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
    }
}