package com.jamal2367.videoinfooverlay

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
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
import android.widget.Toast
import androidx.preference.PreferenceManager
import java.io.IOException

class MainActivity : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private lateinit var overlayTextView: TextView

    private var preference: String = "selected_key_code"
    private var selectedKeyCode: Int = KeyEvent.KEYCODE_BOOKMARK

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // No command
    }

    override fun onInterrupt() {
        // No command
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == selectedKeyCode && event.action == KeyEvent.ACTION_DOWN) {
            if (overlayView != null) {
                removeOverlay()
                Log.d("TAG", "Overlay removed")
            } else {
                createOverlay()
                Log.d("TAG", "Overlay started")
            }
            return true
        }
        return super.onKeyEvent(event)
    }

    private fun createOverlay() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        overlayView = View.inflate(this, R.layout.activity_main, null)
        overlayTextView = overlayView!!.findViewById(R.id.overlayTextView)

        params.gravity = Gravity.TOP or Gravity.END

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlayView, params)
        handler.postDelayed(updateData, 1000)
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

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val keyCodesArray = resources.getStringArray(R.array.key_codes)
        val selectedKeyCodeString = sharedPreferences.getString(preference, keyCodesArray[0])

        val index = keyCodesArray.indexOf(selectedKeyCodeString)

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        selectedKeyCode = when (index) {
            0 -> KeyEvent.KEYCODE_BOOKMARK
            1 -> KeyEvent.KEYCODE_GUIDE
            2 -> KeyEvent.KEYCODE_PROG_RED
            3 -> KeyEvent.KEYCODE_PROG_GREEN
            4 -> KeyEvent.KEYCODE_PROG_YELLOW
            5 -> KeyEvent.KEYCODE_PROG_BLUE
            else -> KeyEvent.KEYCODE_BOOKMARK
        }

        sharedPreferences.edit().putString(preference, selectedKeyCodeString).apply()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == preference) {
            showPreferenceChangedDialog()
        }
    }

    private fun showPreferenceChangedDialog() {
        Toast.makeText(this, getString(R.string.accessibility_info), Toast.LENGTH_LONG).show()
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
                if (colorSpace.isNotEmpty()) {
                    appendLine(getString(R.string.color_space, colorSpace))
                }
                if (hdrPriority.isNotEmpty()) {
                    appendLine(getString(R.string.hdr_priority, hdrPriority))
                }
                if (hdrStatus.isNotEmpty()) {
                    appendLine(getString(R.string.hdr_status, hdrStatus))
                }
                if (hdrPolicy.isNotEmpty()) {
                    val modifiedHdrPolicy = when (hdrPolicy.trim()) {
                        "Follow Source" -> getString(R.string.follow_source)
                        "Follow Sink" -> getString(R.string.follow_sink)
                        else -> hdrPolicy
                    }
                    appendLine(getString(R.string.hdr_policy, modifiedHdrPolicy))
                }
                if (digitalAudioFormat.isNotEmpty()) {
                    val modifiedDigitalAudioFormat = when (digitalAudioFormat.trim()) {
                        "Auto" -> getString(R.string.auto)
                        "Passthrough" -> getString(R.string.passthrough)
                        "Manual" -> getString(R.string.manual)
                        else -> digitalAudioFormat
                    }
                    appendLine(getString(R.string.audio_format, modifiedDigitalAudioFormat))
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

    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }
}
