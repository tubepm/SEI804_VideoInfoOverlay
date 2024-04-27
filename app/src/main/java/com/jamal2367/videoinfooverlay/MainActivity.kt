package com.jamal2367.videoinfooverlay

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import java.io.IOException

class MainActivity : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val clickDelay: Long = 500
    private var lastClickTime: Long = 0
    private var overlayView: View? = null
    private lateinit var overlayTextView: TextView

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // No command
    }

    override fun onInterrupt() {
        // No command
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.END

        when (event.keyCode) {
            KeyEvent.KEYCODE_BOOKMARK -> {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - lastClickTime
                lastClickTime = currentTime

                if (elapsedTime < clickDelay) {
                    if (overlayView != null) {
                        removeOverlay()
                        Log.d("TAG", "Overlay removed")
                    } else {
                        overlayView = View.inflate(this, R.layout.activity_main, null)
                        overlayTextView = overlayView!!.findViewById(R.id.overlayTextView)

                        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                        windowManager.addView(overlayView, params)
                        handler.postDelayed(updateData, 1000)

                        Log.d("TAG", "Overlay started")
                    }
                }
                return true
            }
        }

        return super.onKeyEvent(event)
    }



    private fun removeOverlay() {
        if (overlayView != null) {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.removeView(overlayView)

            overlayView = null
            handler.removeCallbacks(updateData)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("TAG", "onServiceConnected")
    }

    private val updateData = object : Runnable {
        override fun run() {
            val videoFormat = getSystemProperty("sys.nes.info.video_format")
            val videoResolution = getSystemProperty("sys.nes.info.video_resolution")
            val frameRate = getSystemProperty("sys.nes.info.frame_rate")
            val frameCount = getSystemProperty("sys.nes.info.frame_count")
            val displayResolution = getSystemProperty("sys.nes.info.display_resolution")
            val colorSpace = getSystemProperty("sys.nes.info.color_space")
            val hdrStatus = getSystemProperty("sys.nes.info.hdr_status")
            val hdrPolicy = getSystemProperty("sys.nes.info.hdr_policy")
            val hdrPriority = getSystemProperty("sys.nes.info.hdr_priority")
            val digitalAudioFormat = getSystemProperty("sys.nes.info.digital_audio_format")
            val audioMode = getSystemProperty("sys.nes.info.audio_mode")
            val appName = getSystemProperty("sys.nes.info.app_name")
            val cpuUsage = getSystemProperty("sys.nes.info.cpu_usage")
            val memoryUsage = getSystemProperty("sys.nes.info.memory_usage")
            val connectionSpeed = getSystemProperty("sys.nes.info.connection_speed")

            val overlayText = buildString {
                if (videoFormat.isNotEmpty()) {
                    appendLine(getString(R.string.video_format, videoFormat))
                }
                if (videoResolution.isNotEmpty()) {
                    appendLine(getString(R.string.video_resolution, videoResolution))
                }
                if (displayResolution.isNotEmpty()) {
                    appendLine(getString(R.string.display_resolution, displayResolution))
                }
                if (frameRate.isNotEmpty()) {
                    appendLine(getString(R.string.frame_rate, frameRate))
                }
                if (frameCount.isNotEmpty()) {
                    appendLine(getString(R.string.frame_count, frameCount))
                }
                if (hdrStatus.isNotEmpty()) {
                    appendLine(getString(R.string.hdr_status, hdrStatus))
                }
                if (colorSpace.isNotEmpty()) {
                    appendLine(getString(R.string.color_space, colorSpace))
                }
                if (hdrPolicy.isNotEmpty()) {
                    appendLine(getString(R.string.hdr_policy, hdrPolicy))
                }
                if (hdrPriority.isNotEmpty()) {
                    appendLine(getString(R.string.hdr_priority, hdrPriority))
                }
                if (digitalAudioFormat.isNotEmpty()) {
                    appendLine(getString(R.string.digital_audio_format, digitalAudioFormat))
                }
                if (audioMode.isNotEmpty()) {
                    appendLine(getString(R.string.audio_mode, audioMode))
                }
                if (cpuUsage.isNotEmpty()) {
                    appendLine(getString(R.string.cpu_usage, cpuUsage))
                }
                if (memoryUsage.isNotEmpty()) {
                    appendLine(getString(R.string.memory_usage, memoryUsage))
                }
                if (connectionSpeed.isNotEmpty()) {
                    appendLine(getString(R.string.connection_speed, connectionSpeed))
                }
                if (appName.isNotEmpty()) {
                    appendLine(getString(R.string.app_name_tv, appName))
                }
            }
            overlayTextView.text = overlayText.trim()

            // Update output every 1 second
            handler.postDelayed(this, 1000)
        }
    }

    private fun getSystemProperty(propertyName: String): String {
        return try {
            val process = Runtime.getRuntime().exec("getprop $propertyName")
            process.inputStream.bufferedReader().use { it.readLine() ?: "" }
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }
}
