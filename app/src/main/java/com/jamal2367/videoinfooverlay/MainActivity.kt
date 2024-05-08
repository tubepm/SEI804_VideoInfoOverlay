package com.jamal2367.videoinfooverlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
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
import java.util.Locale

class MainActivity : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var overlayTextView: TextView
    private lateinit var overlayTextView2: TextView
    private lateinit var sharedPreferences: SharedPreferences

    private var standardKeyCode: Int = KeyEvent.KEYCODE_BOOKMARK
    private var overlayView: View? = null
    private var lastKeyDownTime: Long = 0

    private val handler = Handler(Looper.getMainLooper())
    private val selectedCodeKey = "selected_code_key"
    private val longPressKey = "long_press_key"
    private val emptyLineKey = "empty_line_key"
    private val textSizeKey = "text_size_key"
    private val textColorKey = "text_color_key"
    private val backgroundColorKey = "background_color_key"

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // No command
    }

    override fun onInterrupt() {
        // No command
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val isLongPress = sharedPreferences.getBoolean(longPressKey, false)

        if (event.keyCode == standardKeyCode) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                lastKeyDownTime = System.currentTimeMillis()
                return true
            } else if (event.action == KeyEvent.ACTION_UP) {
                val currentTime = System.currentTimeMillis()
                val pressDuration = currentTime - lastKeyDownTime

                if (pressDuration >= 750) {
                    if (isLongPress) {
                        if (overlayView != null) {
                            removeOverlay()
                            Log.d("TAG", "Overlay removed")
                        } else {
                            createOverlay()
                            Log.d("TAG", "Overlay started")
                        }
                    }
                } else {
                    if (!isLongPress) {
                        if (overlayView != null) {
                            removeOverlay()
                            Log.d("TAG", "Overlay removed")
                        } else {
                            createOverlay()
                            Log.d("TAG", "Overlay started")
                        }
                    }
                }
                return true
            }
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
        overlayTextView2 = overlayView!!.findViewById(R.id.overlayTextView2)

        updateOverlayTextSize()
        updateOverlayTextColor()
        updateOverlayBackgroundColor()

        params.gravity = Gravity.TOP or Gravity.END

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlayView, params)
        handler.postDelayed(updateData, 750)
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

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        val keyCodesArray = resources.getStringArray(R.array.key_codes)
        val selectedKeyCodeString = sharedPreferences.getString(selectedCodeKey, keyCodesArray[0])

        val index = keyCodesArray.indexOf(selectedKeyCodeString)

        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        standardKeyCode = when (index) {
            0 -> KeyEvent.KEYCODE_BOOKMARK
            1 -> KeyEvent.KEYCODE_GUIDE
            2 -> KeyEvent.KEYCODE_PROG_RED
            3 -> KeyEvent.KEYCODE_PROG_GREEN
            4 -> KeyEvent.KEYCODE_PROG_YELLOW
            5 -> KeyEvent.KEYCODE_PROG_BLUE
            6 -> KeyEvent.KEYCODE_0
            7 -> KeyEvent.KEYCODE_1
            8 -> KeyEvent.KEYCODE_2
            9 -> KeyEvent.KEYCODE_3
            10 -> KeyEvent.KEYCODE_4
            11 -> KeyEvent.KEYCODE_5
            12 -> KeyEvent.KEYCODE_6
            13 -> KeyEvent.KEYCODE_7
            14 -> KeyEvent.KEYCODE_8
            15 -> KeyEvent.KEYCODE_9
            else -> KeyEvent.KEYCODE_BOOKMARK
        }

        sharedPreferences.edit().putString(selectedCodeKey, selectedKeyCodeString).apply()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == selectedCodeKey) {
            showPreferenceChangedDialog()
        }

        if (key == textSizeKey) {
            updateOverlayTextSize()
        }

        if (key == textColorKey) {
            updateOverlayTextColor()
        }

        if (key == backgroundColorKey) {
            updateOverlayBackgroundColor()
        }
    }

    private fun showPreferenceChangedDialog() {
        Toast.makeText(this, getString(R.string.accessibility_info1), Toast.LENGTH_LONG).show()
        Toast.makeText(this, getString(R.string.accessibility_info2), Toast.LENGTH_LONG).show()
    }

    private val updateData = object : Runnable {
        override fun run() {
            val isEmptyLine = sharedPreferences.getBoolean(emptyLineKey, true)

            val videoFormat = getSystemProperty("sys.nes.info.video_format")
            val videoResolution = getSystemProperty("sys.nes.info.video_resolution")
            val frameRate = getSystemProperty("sys.nes.info.frame_rate")
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
                if (isEmptyLine) {
                    appendLine("⠀ ")
                }

                if (displayResolution.isNotEmpty()) {
                    val modifiedDisplayResolution = when (displayResolution.trim()) {
                        "2160p60hz" -> "3840 x 2160, 60Hz"
                        "2160p50hz" -> "3840 x 2160, 50Hz"
                        "2160p30hz" -> "3840 x 2160, 30Hz"
                        "2160p25hz" -> "3840 x 2160, 25Hz"
                        "2160p24hz" -> "3840 x 2160, 24Hz"

                        "1080p60hz" -> "1920 x 1080, 60Hz"
                        "1080p50hz" -> "1920 x 1080, 50Hz"
                        "1080p30hz" -> "1920 x 1080, 30Hz"
                        "1080p25hz" -> "1920 x 1080, 25Hz"
                        "1080p24hz" -> "1920 x 1080, 24Hz"

                        "1080i60hz" -> "1920 x 1080i, 60Hz"
                        "1080i50hz" -> "1920 x 1080i, 50Hz"
                        "1080i30hz" -> "1920 x 1080i, 30Hz"
                        "1080i25hz" -> "1920 x 1080i, 25Hz"
                        "1080i24hz" -> "1920 x 1080i, 24Hz"

                        "720p60hz" -> "1280 x 720, 60Hz"
                        "720p50hz" -> "1280 x 720, 50Hz"
                        "720p30hz" -> "1280 x 720, 30Hz"
                        "720p25hz" -> "1280 x 720, 25Hz"
                        "720p24hz" -> "1280 x 720, 24Hz"

                        "720i60hz" -> "1280 x 720i, 60Hz"
                        "720i50hz" -> "1280 x 720i, 50Hz"
                        "720i30hz" -> "1280 x 720i, 30Hz"
                        "720i25hz" -> "1280 x 720i, 25Hz"
                        "720i24hz" -> "1280 x 720i, 24Hz"

                        "576p60hz" -> "1024 x 576, 60Hz"
                        "576p50hz" -> "1024 x 576, 50Hz"
                        "576p30hz" -> "1024 x 576, 30Hz"
                        "576p25hz" -> "1024 x 576, 25Hz"
                        "576p24hz" -> "1024 x 576, 24Hz"

                        "576i60hz" -> "1024 x 576i, 60Hz"
                        "576i50hz" -> "1024 x 576i, 50Hz"
                        "576i30hz" -> "1024 x 576i, 30Hz"
                        "576i25hz" -> "1024 x 576i, 25Hz"
                        "576i24hz" -> "1024 x 576i, 24Hz"

                        "480p60hz" -> "854 × 480, 60Hz"
                        "480p50hz" -> "854 × 480, 50Hz"
                        "480p30hz" -> "854 × 480, 30Hz"
                        "480p25hz" -> "854 × 480, 25Hz"
                        "480p24hz" -> "854 × 480, 24Hz"

                        "480i60hz" -> "854 × 480i, 60Hz"
                        "480i50hz" -> "854 × 480i, 50Hz"
                        "480i30hz" -> "854 × 480i, 30Hz"
                        "480i25hz" -> "854 × 480i, 25Hz"
                        "480i24hz" -> "854 × 480i, 24Hz"
                        else -> displayResolution
                    }
                    appendLine(modifiedDisplayResolution)
                }

                if (videoResolution.isNotEmpty()) {
                    var videoInfo = videoResolution

                    val frameRateUpperCase = frameRate.uppercase(Locale.ROOT)
                    val frameRateFormatted = frameRateUpperCase.replace("\\s".toRegex(), "")
                    videoInfo += if (frameRateFormatted.isNotEmpty()) ", $frameRateFormatted" else ""

                    appendLine(videoInfo)
                }

                if (videoFormat.isNotEmpty()) {
                    val modifiedVideoFormat = when (videoFormat.trim()) {
                        "amvdec_avs_v4l" -> "AVS"
                        "amvdec_avs2_v4l" -> "AVS2"
                        "amvdec_avs2_fb_v4l" -> "AVS2"
                        "amvdec_avs3_v4l" -> "AVS3"
                        "amvdec_mavs_v4l" -> "AVS Multi"
                        "amvdec_h264_v4l" -> "H.264"
                        "amvdec_mh264_v4l" -> "H.264 Multi"
                        "amvdec_h265_v4l" -> "H.265"
                        "amvdec_h265_fb_v4l" -> "H.265"
                        "amvdec_mmjpeg_v4l" -> "Motion JPEG"
                        "amvdec_mmpeg12_v4l" -> "MPEG 1/2"
                        "amvdec_mmpeg4_v4l" -> "MPEG 4"
                        "amvdec_av1_v4l" -> "AV1"
                        "amvdec_av1_fb_v4l" -> "AV1"
                        "amvdec_av1_t5d_v4l" -> "AV1"
                        "amvdec_vp9_v4l" -> "VP9"
                        "amvdec_vp9_fb_v4l" -> "VP9"
                        "amvdec_avs_v4" -> "AVS"
                        "amvdec_avs2_v4" -> "AVS2"
                        "amvdec_avs2_fb_v4" -> "AVS2"
                        "amvdec_avs3_v4" -> "AVS3"
                        "amvdec_mavs_v4" -> "AVS Multi"
                        "amvdec_h264_v4" -> "H.264"
                        "amvdec_mh264_v4" -> "H.264 Multi"
                        "amvdec_h265_v4" -> "H.265"
                        "amvdec_h265_fb_v4" -> "H.265"
                        "amvdec_mmjpeg_v4" -> "Motion JPEG"
                        "amvdec_mmpeg12_v4" -> "MPEG 1/2"
                        "amvdec_mmpeg4_v4" -> "MPEG 4"
                        "amvdec_av1_v4" -> "AV1"
                        "amvdec_av1_fb_v4" -> "AV1"
                        "amvdec_av1_t5d_v4" -> "AV1"
                        "amvdec_vp9_v4" -> "VP9"
                        "amvdec_vp9_fb_v4" -> "VP9"
                        "ammvdec_avs_v4" -> "AVS"
                        "ammvdec_avs2_v4" -> "AVS2"
                        "ammvdec_avs2_fb_v4" -> "AVS2"
                        "ammvdec_avs3_v4" -> "AVS3"
                        "ammvdec_mavs_v4" -> "AVS Multi"
                        "ammvdec_h264_v4" -> "H.264"
                        "ammvdec_mh264_v4" -> "H.264 Multi"
                        "ammvdec_h265_v4" -> "H.265"
                        "ammvdec_h265_fb_v4" -> "H.265"
                        "ammvdec_mmjpeg_v4" -> "Motion JPEG"
                        "ammvdec_mmpeg12_v4" -> "MPEG 1/2"
                        "ammvdec_mmpeg4_v4" -> "MPEG 4"
                        "ammvdec_av1_v4" -> "AV1"
                        "ammvdec_av1_fb_v4" -> "AV1"
                        "ammvdec_av1_t5d_v4" -> "AV1"
                        "ammvdec_vp9_v4" -> "VP9"
                        "ammvdec_vp9_fb_v4" -> "VP9"
                        "amvdec_avs" -> "AVS"
                        "amvdec_avs2" -> "AVS2"
                        "amvdec_avs2_fb" -> "AVS2"
                        "amvdec_avs3" -> "AVS3"
                        "amvdec_mavs" -> "AVS Multi"
                        "amvdec_h264" -> "H.264"
                        "amvdec_mh264" -> "H.264 Multi"
                        "amvdec_h265" -> "H.265"
                        "amvdec_h265_fb" -> "H.265"
                        "amvdec_mmjpeg" -> "Motion JPEG"
                        "amvdec_mmpeg12" -> "MPEG 1/2"
                        "amvdec_mmpeg4" -> "MPEG 4"
                        "amvdec_av1" -> "AV1"
                        "amvdec_av1_fb" -> "AV1"
                        "amvdec_av1_t5d" -> "AV1"
                        "amvdec_vp9" -> "VP9"
                        "amvdec_vp9_fb" -> "VP9"
                        "ammvdec_mpeg12" -> "MPEG 1/2"
                        "ammvdec_mpeg4" -> "MPEG 4"
                        "ammvdec_h264" -> "H.264"
                        "ammvdec_mjpeg" -> "Motion JPEG"
                        "ammvdec_vc1" -> "VC1"
                        "ammvdec_avs" -> "AVS"
                        "ammvdec_yuv" -> "YUV"
                        "ammvdec_h264mvc" -> "H.264 MVC"
                        "ammvdec_h264_4k2k" -> "H.264 4K/2K"
                        "ammvdec_h265" -> "H.265"
                        "amvenc_avc" -> "AVC"
                        "ammvdec_vp9" -> "VP9"
                        "ammvdec_avs2" -> "AVS2"
                        "ammvdec_av1" -> "AV1"
                        else -> videoFormat
                    }
                    appendLine(modifiedVideoFormat)
                }

                if (isEmptyLine) {
                    appendLine()
                    appendLine()
                }

                if (digitalAudioFormat.isNotEmpty()) {
                    val modifiedDigitalAudioFormat = when (digitalAudioFormat.trim()) {
                        "Auto" -> getString(R.string.auto)
                        "Passthrough" -> getString(R.string.passthrough)
                        "Manual" -> getString(R.string.manual)
                        else -> digitalAudioFormat
                    }
                    appendLine(modifiedDigitalAudioFormat)
                }

                if (audioMode.isNotEmpty()) {
                    val modifiedAudioMode = when (audioMode.trim()) {
                        "AC3" -> "Dolby Digital"
                        "AC4" -> "Dolby AC-4"
                        "EAC3" -> "Dolby Digital+"
                        "MULTI PCM" -> "Multi PCM"
                        "PCM HIGH SR" -> "PCM High SR"
                        "TRUE HD" -> "Dolby TrueHD"
                        "DTS HD" -> "DTS-HD"
                        "DTS HD MA" -> "DTS-HD Master Audio"
                        "MAT" -> "Dolby MAT"
                        "DDP ATMOS" -> "Dolby Digital+ (Atmos)"
                        "TRUE HD ATMOS" -> "Dolby TrueHD (Atmos)"
                        "AC4 ATMOS" -> "Dolby AC-4 (Atmos)"
                        "DTS EXPRESS" -> "DTS Express"
                        else -> audioMode
                    }
                    appendLine(modifiedAudioMode)
                }

                if (isEmptyLine) {
                    appendLine()
                    appendLine()
                }

                if (colorSpace.isNotEmpty()) {
                    val modifiedColorSpace = when (colorSpace.trim()) {
                        "default" -> "YCbCr 4:2:2 (10 Bit)"
                        "YCbCr422 12bit" -> "YCbCr 4:2:2 (12 Bit)"
                        "YCbCr444 8bit" -> "YCbCr 4:4:4 (8 Bit)"
                        else -> colorSpace
                    }
                    appendLine(modifiedColorSpace)
                }

                if (hdrPriority.isNotEmpty()) {
                    appendLine(hdrPriority)
                }

                if (hdrStatus.isNotEmpty()) {
                    val modifiedHdrStatus = when (hdrStatus.trim()) {
                        "HDR10-GAMMA_ST2084" -> "HDR10"
                        "HDR10-GAMMA_HLG" -> "HLG"
                        "HDR10Plus-VSIF" -> "HDR10+"
                        "DolbyVision-Lowlatency" -> "Dolby Vision (Low Latency)"
                        "DolbyVision-Std" -> "Dolby Vision (Standard)"
                        else -> hdrStatus
                    }
                    appendLine(modifiedHdrStatus)
                }

                if (hdrPolicy.isNotEmpty()) {
                    val modifiedHdrPolicy = when (hdrPolicy.trim()) {
                        "Follow Source" -> getString(R.string.follow_source)
                        "Follow Sink" -> getString(R.string.follow_sink)
                        else -> hdrPolicy
                    }
                    appendLine(modifiedHdrPolicy)
                }

                if (isEmptyLine) {
                    appendLine()
                    appendLine()
                }

                if (cpuUsage.isNotEmpty()) {
                    val formattedCpuUsage = cpuUsage.replace(Regex("(\\d+)%"), "$1 %")
                    appendLine(formattedCpuUsage)
                }

                if (memoryUsage.isNotEmpty()) {
                    appendLine(memoryUsage)
                }

                if (getConnectionState().isNotEmpty()) {
                    val modifiedgetConnectionState = when (getConnectionState().trim()) {
                        "WIFI" -> getString(R.string.wifi)
                        "Ethernet" -> getString(R.string.ethernet)
                        else -> getConnectionState()
                    }

                    if (connectionSpeed.isNotEmpty()) {
                        val formattedSpeed = connectionSpeed.replace(Regex("(\\d)([A-Za-z])"), "$1 $2")
                        val connectionInfo = "$modifiedgetConnectionState / $formattedSpeed"
                        if (modifiedgetConnectionState != getString(R.string.no_connectivity)) {
                            appendLine(connectionInfo)
                        } else {
                            appendLine(getString(R.string.no_connectivity))
                        }
                    }
                }

                if (appName.isNotEmpty()) {
                    appendLine(appName)
                }
            }

            val overlayText2 = buildString {
                if (isEmptyLine) {
                    appendLine(getString(R.string.video))
                }

                if (displayResolution.isNotEmpty()) {
                    appendLine(getString(R.string.display_resolution))
                }

                if (videoResolution.isNotEmpty()) {
                    appendLine(getString(R.string.video_resolution))
                }

                if (videoFormat.isNotEmpty()) {
                    appendLine(getString(R.string.video_format))
                }

                if (isEmptyLine) {
                    appendLine()
                    appendLine(getString(R.string.audio))
                }

                if (digitalAudioFormat.isNotEmpty()) {
                    appendLine(getString(R.string.audio_format))
                }

                if (audioMode.isNotEmpty()) {
                    appendLine(getString(R.string.audio_mode))
                }

                if (isEmptyLine) {
                    appendLine()
                    appendLine(getString(R.string.display))
                }

                if (colorSpace.isNotEmpty()) {
                    appendLine(getString(R.string.color_space))
                }

                if (hdrPriority.isNotEmpty()) {
                    appendLine(getString(R.string.hdr_priority))
                }

                if (hdrStatus.isNotEmpty()) {
                    appendLine(getString(R.string.hdr_status))
                }

                if (hdrPolicy.isNotEmpty()) {
                    appendLine(getString(R.string.hdr_policy))
                }

                if (isEmptyLine) {
                    appendLine()
                    appendLine(getString(R.string.other))
                }

                if (cpuUsage.isNotEmpty()) {
                    appendLine(getString(R.string.cpu_usage))
                }

                if (memoryUsage.isNotEmpty()) {
                    appendLine(getString(R.string.memory_usage))
                }

                if (getConnectionState().isNotEmpty()) {
                    appendLine(getString(R.string.connection))

                }

                if (appName.isNotEmpty()) {
                    appendLine(getString(R.string.app_name_tv))
                }
            }

            overlayTextView.text = overlayText.trim()
            overlayTextView2.text = overlayText2.trim()

            // Update output every 0.75 second
            handler.postDelayed(this, 750)
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

    private fun updateOverlayTextSize() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textSizeKey = sharedPreferences.getString("text_size_key", "12") ?: "12"
            val textSize = textSizeKey.toFloat()

            overlayTextView.textSize = textSize
            overlayTextView2.textSize = textSize
        }
    }


    private fun updateOverlayTextColor() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textColorKey = sharedPreferences.getString("text_color_key", "#FFFFFF") ?: "#FFFFFF"
            val textColor = Color.parseColor(textColorKey)
            overlayTextView.setTextColor(textColor)
            overlayTextView2.setTextColor(textColor)
        }
    }

    private fun updateOverlayBackgroundColor() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val backgroundColorKey = sharedPreferences.getString("background_color_key", "#E6000000") ?: "#E6000000"
            val backgroundColor = Color.parseColor(backgroundColorKey)

            // Erstellen Sie separate Hintergrunddrawables für jede TextView
            val backgroundDrawable1 = GradientDrawable()
            val backgroundDrawable2 = GradientDrawable()

            backgroundDrawable1.setColor(backgroundColor)
            backgroundDrawable1.cornerRadii = floatArrayOf(0f, 0f, 28.toFloat(), 28.toFloat(), 28.toFloat(), 28.toFloat(), 0f, 0f)

            backgroundDrawable2.setColor(backgroundColor)
            backgroundDrawable2.cornerRadii = floatArrayOf(28.toFloat(), 28.toFloat(), 0f, 0f, 0f, 0f, 28.toFloat(), 28.toFloat())

            overlayTextView.background = backgroundDrawable1
            overlayTextView2.background = backgroundDrawable2
        }
    }



    @Suppress("DEPRECATION")
    fun getConnectionState(): String {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo

        return if (networkInfo != null && networkInfo.isConnected) {
            networkInfo.typeName
        } else {
            getString(R.string.no_connectivity)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }
}
