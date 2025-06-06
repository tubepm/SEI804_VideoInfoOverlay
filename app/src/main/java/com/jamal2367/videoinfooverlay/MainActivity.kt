package com.jamal2367.videoinfooverlay

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.preference.PreferenceManager
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbCrypto
import com.tananaev.adblib.AdbStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.ref.WeakReference
import java.net.Socket
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.text.format.DateFormat as AndroidDateFormat


class MainActivity : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var overlayTextView: TextView
    private lateinit var overlayTextView2: TextView
    private lateinit var sharedPreferences: SharedPreferences

    private var standardKeyCode: Int = KeyEvent.KEYCODE_BOOKMARK
    private var overlayView: View? = null
    private var lastKeyDownTime: Long = 0
    private var serviceBinder: IBinder? = null
    private var serviceConnection: ServiceConnection? = null
    private val lock = Any()
    private val handler = Handler(Looper.getMainLooper())
    private var connection: AdbConnection? = null
    private var stream: AdbStream? = null
    private var myAsyncTask: MyAsyncTask? = null
    private val ipAddress = "0.0.0.0"
    private var requestKey= "request_key"
    private var celFahrKey= "celsius_fahrenheit_key"
    private val publicKeyName: String = "public.key"
    private val privateKeyName: String = "private.key"
    private val selectedCodeKey = "selected_code_key"
    private val longPressKey = "long_press_key"
    private val hideLeftOverlay = "hide_left_overlay_key"
    private val roundedCornerOverallLeftKey = "rounded_corner_overall_left_key"
    private val roundedCornerOverallRightKey = "rounded_corner_overall_right_key"
    private val marginWidthKey = "margin_width_key"
    private val marginHeightKey = "margin_height_key"
    private val marginBothKey = "margin_both_key"
    private val textSizeKey = "text_size_key"
    private val textPaddingKey = "text_padding_key"
    private val textColorLeftKey = "text_color_left_key"
    private val textColorRightKey = "text_color_right_key"
    private val textAlignLeftKey = "text_align_left_key"
    private val textAlignRightKey = "text_align_right_key"
    private val backgroundColorLeftKey = "background_color_left_key"
    private val backgroundColorRightKey = "background_color_right_key"
    private val backgroundAlphaLeftKey = "background_alpha_left_key"
    private val backgroundAlphaRightKey = "background_alpha_right_key"
    private val roundedCornersLeftKey = "rounded_corners_left_key"
    private val roundedCornersRightKey = "rounded_corners_right_key"
    private val textFontKey = "text_font_key"
    private val textSecondsKey = "text_seconds_key"
    private val textMbpsKey = "text_mbps_key"
    private val emptyLineKey = "empty_line_key"
    private val emptyTitleKey = "empty_title_key"
    private val textHideVideoTitleKey = "pref_hide_video_title_key"
    private val textHideDisplayResolutionKey = "pref_hide_display_resolution_key"
    private val textHideVideoResolutionKey = "pref_hide_video_resolution_key"
    private val textHideVideoFormatKey = "pref_hide_video_format_key"
    private val textHideEmptyVideoAudioLineKey = "pref_hide_empty_video_audio_line_key"
    private val textHideAudioTitleKey = "pref_hide_audio_title_key"
    private val textHideAudioModeKey = "pref_hide_audio_mode_key"
    private val textHideAudioFormatKey = "pref_hide_audio_format_key"
    private val textHideEmptyAudioDisplayLineKey = "pref_hide_empty_audio_display_line_key"
    private val textHideDisplayTitleKey = "pref_hide_display_title_key"
    private val textHideHdrStatusKey = "pref_hide_hdr_status_key"
    private val textHideHdrPriorityKey = "pref_hide_hdr_priority_key"
    private val textHideHdrPolicyKey = "pref_hide_hdr_policy_key"
    private val textHideColorSpaceKey = "pref_hide_color_space_key"
    private val textHideEmptyDisplayProcessorLineKey = "pref_hide_empty_display_processor_line_key"
    private val textHideProcessorTitleKey = "pref_hide_processor_title_key"
    private val textHideCpuLoadKey = "pref_hide_cpu_load_key"
    private val textHideCpuClockKey = "pref_hide_cpu_clock_key"
    private val textHideCpuTemperatureKey = "pref_hide_cpu_temperature_key"
    private val textHideCpuGovernorKey = "pref_hide_cpu_governor_key"
    private val textHideEmptyProcessorOtherLineKey = "pref_hide_empty_processor_other_line_key"
    private val textHideOtherTitleKey = "pref_hide_other_title_key"
    private val textHideTimeKey = "pref_hide_time_key"
    private val textHideMemoryUsageKey = "pref_hide_memory_usage_key"
    private val textHideConnectionKey = "pref_hide_connection_key"
    private val textHideAppNameKey = "pref_hide_app_name_key"
    private val textHideAppMemoryUsageKey = "pref_hide_app_memory_usage_key"

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
    }

    override fun onInterrupt() {
    }

    private fun ensureService(): IBinder? {
        synchronized(lock) {
            if (serviceConnection == null) {
                serviceConnection = object : ServiceConnection {
                    override fun onNullBinding(componentName: ComponentName) {
                        synchronized(lock) {
                            Log.d("TAG", "NES service is not supported")
                        }
                    }

                    override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
                        synchronized(lock) {
                            Log.d("TAG", "NES service connected")
                            serviceBinder = iBinder

                            startTvBugTracker()
                            updateOverlayKeyButton()
                        }
                    }

                    override fun onServiceDisconnected(componentName: ComponentName) {
                        synchronized(lock) {
                            Log.d("TAG", "NES service disconnected")
                            serviceBinder = null
                        }
                    }
                }

                val intent = Intent().setClassName("com.nes.tvbugtracker", "com.nes.tvbugtracker.MainService")
                if (!this.bindService(intent, serviceConnection!!, 1)) {
                    Log.d("TAG", "NES service not available")
                }
            }

            return serviceBinder
        }
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

    private fun isUsbDebuggingEnabled(): Boolean {
        return Settings.Global.getInt(contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
    }

    private fun createOverlay() {
        val isHideLeftOverlay = sharedPreferences.getBoolean(hideLeftOverlay, false)
        val isHideCpuTemperature = sharedPreferences.getBoolean(textHideCpuTemperatureKey, true)
        val isHideAppMemoryUsage = sharedPreferences.getBoolean(textHideAppMemoryUsageKey, true)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        if (isUsbDebuggingEnabled()) {
            if (!isHideCpuTemperature || !isHideAppMemoryUsage) {
                onKeyCE()
            } else {
                connection = null
                stream = null

                myAsyncTask = MyAsyncTask(this)
                myAsyncTask?.cancel()
            }
        }

        overlayView = View.inflate(this, R.layout.activity_main, null)
        overlayTextView = overlayView!!.findViewById(R.id.overlayTextView)

        if (!isHideLeftOverlay) {
            overlayTextView2 = overlayView!!.findViewById(R.id.overlayTextView2)
        } else {
            overlayTextView2 = overlayView!!.findViewById(R.id.overlayTextView2)
            overlayTextView2.visibility = View.GONE
        }

        updateOverlayMarginWidth()
        updateOverlayMarginHeight()
        updateOverlayMarginBoth()
        updateOverlayTextPadding()
        updateOverlayTextSize()
        updateOverlayLeftTextColor()
        updateOverlayRightTextColor()
        updateOverlayLeftTextAlign()
        updateOverlayRightTextAlign()
        updateOverlayLeftBackground()
        updateOverlayRightBackground()
        updateOverlayTextFont()

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(overlayView, params)

        handler.postDelayed(updateData, 750)
    }

    private fun removeOverlay() {
        if (overlayView != null) {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.removeView(overlayView)

            overlayView = null
        }
    }

    private fun startTvBugTracker() {
        val intent = Intent("com.nes.action.SHOW_DEBUG_VIEW")

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.putExtra("invisible", 1)
        startActivity(intent)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("TAG", "onServiceConnected")

        ensureService()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == selectedCodeKey) {
            updateOverlayKeyButton()
        }

        if (key == marginWidthKey) {
            updateOverlayMarginWidth()
        }

        if (key == marginHeightKey) {
            updateOverlayMarginHeight()
        }

        if (key == marginBothKey) {
            updateOverlayMarginBoth()
        }

        if (key == textPaddingKey) {
            updateOverlayTextPadding()
        }

        if (key == textSizeKey) {
            updateOverlayTextSize()
        }

        if (key == textColorLeftKey) {
            updateOverlayLeftTextColor()
        }

        if (key == textColorRightKey) {
            updateOverlayRightTextColor()
        }

        if (key == textAlignLeftKey) {
            updateOverlayLeftTextAlign()
        }

        if (key == textAlignRightKey) {
            updateOverlayRightTextAlign()
        }

        if (key == backgroundColorLeftKey || key == roundedCornersLeftKey || key == backgroundAlphaLeftKey) {
            updateOverlayLeftBackground()
        }

        if (key == backgroundColorRightKey || key == roundedCornersRightKey || key == backgroundAlphaRightKey) {
            updateOverlayRightBackground()
        }

        if (key == textFontKey) {
            updateOverlayTextFont()
        }

        if (key == hideLeftOverlay) {
            val isHideLeftOverlay = sharedPreferences?.getBoolean(hideLeftOverlay, false) ?: false

            if (isHideLeftOverlay) {
                sharedPreferences?.edit()?.putBoolean(emptyTitleKey, false)?.apply()
            }
        }
    }

    private val updateData: Runnable by lazy {
        Runnable {
            CoroutineScope(Dispatchers.Main).launch {
                val isEmptyLine = sharedPreferences.getBoolean(emptyLineKey, false)
                val isTitleLine = sharedPreferences.getBoolean(emptyTitleKey, false)
                val isHideVideoTitle = sharedPreferences.getBoolean(textHideVideoTitleKey, false)
                val isHideDisplayResolution = sharedPreferences.getBoolean(textHideDisplayResolutionKey, false)
                val isHideVideoResolution = sharedPreferences.getBoolean(textHideVideoResolutionKey, false)
                val isHideVideoFormat = sharedPreferences.getBoolean(textHideVideoFormatKey, false)
                val isHideEmptyVideoAudioLine = sharedPreferences.getBoolean(textHideEmptyVideoAudioLineKey, false)
                val isHideAudioTitle = sharedPreferences.getBoolean(textHideAudioTitleKey, false)
                val isHideAudioMode = sharedPreferences.getBoolean(textHideAudioModeKey, false)
                val isHideAudioFormat = sharedPreferences.getBoolean(textHideAudioFormatKey, false)
                val isHideEmptyAudioDisplayLine = sharedPreferences.getBoolean(textHideEmptyAudioDisplayLineKey, false)
                val isHideDisplayTitle = sharedPreferences.getBoolean(textHideDisplayTitleKey, false)
                val isHideHdrStatus = sharedPreferences.getBoolean(textHideHdrStatusKey, false)
                val isHideHdrPriority = sharedPreferences.getBoolean(textHideHdrPriorityKey, false)
                val isHideHdrPolicy = sharedPreferences.getBoolean(textHideHdrPolicyKey, false)
                val isHideColorSpace = sharedPreferences.getBoolean(textHideColorSpaceKey, false)
                val isHideEmptyDisplayProcessorLine = sharedPreferences.getBoolean(textHideEmptyDisplayProcessorLineKey, false)
                val isHideProcessorTitle = sharedPreferences.getBoolean(textHideProcessorTitleKey, false)
                val isHideCpuLoad = sharedPreferences.getBoolean(textHideCpuLoadKey, false)
                val isHideCpuClock = sharedPreferences.getBoolean(textHideCpuClockKey, false)
                val isHideCpuTemperature = sharedPreferences.getBoolean(textHideCpuTemperatureKey, true)
                val isHideCpuGovernor = sharedPreferences.getBoolean(textHideCpuGovernorKey, false)
                val isHideEmptyProcessorOtherLine = sharedPreferences.getBoolean(textHideEmptyProcessorOtherLineKey, false)
                val isHideOtherTitle = sharedPreferences.getBoolean(textHideOtherTitleKey, false)
                val isHideTime = sharedPreferences.getBoolean(textHideTimeKey, false)
                val isHideMemoryUsage = sharedPreferences.getBoolean(textHideMemoryUsageKey, false)
                val isHideConnection = sharedPreferences.getBoolean(textHideConnectionKey, false)
                val isHideAppName = sharedPreferences.getBoolean(textHideAppNameKey, false)
                val isHideAppMemoryUsage = sharedPreferences.getBoolean(textHideAppMemoryUsageKey, true)
                val isRequest = sharedPreferences.getBoolean(requestKey, false)
                val isCelFahr = sharedPreferences.getBoolean(celFahrKey, false)
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
                val cpuCurrPerc = printCpuCurrPerc()
                val cpuCurrMaxPerc = printCpuCurrMaxPerc()
                val cpuCurr = printCpuCurr()
                val cpuCurrMax = printCpuCurrMax()
                val cpuPercentage = printCpuPercentage()
                val cpuUsage = getSystemProperty("sys.nes.info.cpu_usage")
                val cpuGovernor = printCpuGovernor()
                val memoryUsage = getSystemProperty("sys.nes.info.memory_usage")
                val memoryPercentage = formatMemoryUsagePercentage(memoryUsage)
                val memoryMb = formatMemoryUsageMB(memoryUsage)
                val memoryMbPercentage = formatMemoryUsageMBPercentage(memoryUsage)
                val memoryGb = formatMemoryUsageGB(memoryUsage)
                val memoryGbPercentage = formatMemoryUsageGBPercentage(memoryUsage)
                val connectionSpeed = getSystemProperty("sys.nes.info.connection_speed")
                val localTime = getCurrentTimeFormatted(applicationContext)
                val cpuTemp = getHardwarePropertiesCelsius()
                val appMemoryUsage = getAppMemoryUsage()

                val overlayText = buildString {
                    if (!isHideVideoTitle) {
                        if (!isTitleLine) {
                            appendLine("\u200E")
                        }
                    }

                    if (!isHideDisplayResolution) {
                        if (displayResolution.isNotEmpty()) {
                            val modifiedDisplayResolution = when (displayResolution.trim()) {
                                "2160p60hz" -> "3840 x 2160, 60hz"
                                "2160p50hz" -> "3840 x 2160, 50hz"
                                "2160p30hz" -> "3840 x 2160, 30hz"
                                "2160p25hz" -> "3840 x 2160, 25hz"
                                "2160p24hz" -> "3840 x 2160, 24hz"
                                "1080p60hz" -> "1920 x 1080, 60hz"
                                "1080p50hz" -> "1920 x 1080, 50hz"
                                "1080p30hz" -> "1920 x 1080, 30hz"
                                "1080p25hz" -> "1920 x 1080, 25hz"
                                "1080p24hz" -> "1920 x 1080, 24hz"
                                "1080i60hz" -> "1920 x 1080i, 60hz"
                                "1080i50hz" -> "1920 x 1080i, 50hz"
                                "1080i30hz" -> "1920 x 1080i, 30hz"
                                "1080i25hz" -> "1920 x 1080i, 25hz"
                                "1080i24hz" -> "1920 x 1080i, 24hz"
                                "720p60hz" -> "1280 x 720, 60hz"
                                "720p50hz" -> "1280 x 720, 50hz"
                                "720p30hz" -> "1280 x 720, 30hz"
                                "720p25hz" -> "1280 x 720, 25hz"
                                "720p24hz" -> "1280 x 720, 24hz"
                                "720i60hz" -> "1280 x 720i, 60hz"
                                "720i50hz" -> "1280 x 720i, 50hz"
                                "720i30hz" -> "1280 x 720i, 30hz"
                                "720i25hz" -> "1280 x 720i, 25hz"
                                "720i24hz" -> "1280 x 720i, 24hz"
                                "576p60hz" -> "1024 x 576, 60hz"
                                "576p50hz" -> "1024 x 576, 50hz"
                                "576p30hz" -> "1024 x 576, 30hz"
                                "576p25hz" -> "1024 x 576, 25hz"
                                "576p24hz" -> "1024 x 576, 24hz"
                                "576i60hz" -> "1024 x 576i, 60hz"
                                "576i50hz" -> "1024 x 576i, 50hz"
                                "576i30hz" -> "1024 x 576i, 30hz"
                                "576i25hz" -> "1024 x 576i, 25hz"
                                "576i24hz" -> "1024 x 576i, 24hz"
                                "480p60hz" -> "854 × 480, 60hz"
                                "480p50hz" -> "854 × 480, 50hz"
                                "480p30hz" -> "854 × 480, 30hz"
                                "480p25hz" -> "854 × 480, 25hz"
                                "480p24hz" -> "854 × 480, 24hz"
                                "480i60hz" -> "854 × 480i, 60hz"
                                "480i50hz" -> "854 × 480i, 50hz"
                                "480i30hz" -> "854 × 480i, 30hz"
                                "480i25hz" -> "854 × 480i, 25hz"
                                "480i24hz" -> "854 × 480i, 24hz"
                                "smpte24hz" -> "SMPTE, 24hz"
                                else -> displayResolution
                            }
                            appendLine(modifiedDisplayResolution)
                        }
                    }

                    if (!isHideVideoResolution) {
                        if (videoResolution.isNotEmpty()) {
                            var videoInfo = videoResolution

                            val frameRateFormatted = frameRate.replace("\\s".toRegex(), "")
                            videoInfo += if (frameRateFormatted.isNotEmpty()) ", $frameRateFormatted" else ""

                            appendLine(videoInfo)
                        }
                    }

                    if (!isHideVideoFormat) {
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
                                "amvdec_h264-00" -> "H.264"
                                "amvdec_mh264_v4" -> "H.264 Multi"
                                "amvdec_h265_v4" -> "H.265"
                                "amvdec_h265_fb_v4" -> "H.265"
                                "amvdec_mmjpeg_v4" -> "Motion JPEG"
                                "amvdec_mmpeg12_v4" -> "MPEG 1/2"
                                "amvdec_mmpeg4_v4" -> "MPEG 4"
                                "amvdec_av1_v4" -> "AV1"
                                "amvdec_av1_fb_v4" -> "AV1"
                                "amvdec_av1_t5d_v4" -> "AV1"
                                "amvdec_vc1" -> "VC1"
                                "amvdec_vp9_v4" -> "VP9"
                                "amvdec_vp9_fb_v4" -> "VP9"
                                "ammvdec_avs_v4" -> "AVS"
                                "ammvdec_avs2_v4" -> "AVS2"
                                "ammvdec_avs2_fb_v4" -> "AVS2"
                                "ammvdec_avs3_v4" -> "AVS3"
                                "ammvdec_mavs_v4" -> "AVS Multi"
                                "ammvdec_h264_v4" -> "H.264"
                                "ammvdec_h264-00" -> "H.264"
                                "ammvdec_mh264_v4" -> "H.264 Multi"
                                "ammvdec_h265_v4" -> "H.265"
                                "ammvdec_h265_fb_v4" -> "H.265"
                                "ammvdec_mmjpeg_v4" -> "Motion JPEG"
                                "ammvdec_mmpeg12_v4" -> "MPEG 1/2"
                                "ammvdec_mmpeg4_v4" -> "MPEG 4"
                                "ammvdec_av1_v4" -> "AV1"
                                "ammvdec_av1_fb_v4" -> "AV1"
                                "ammvdec_av1_t5d_v4" -> "AV1"
                                "ammvdec_vc1" -> "VC1"
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
                    }

                    if (!isHideEmptyVideoAudioLine && !isEmptyLine) {
                        appendLine()
                    }

                    if (!isHideAudioTitle) {
                        if (!isTitleLine) {
                            appendLine("\u200E")
                        }
                    }

                    if (!isHideAudioMode) {
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
                    }

                    if (!isHideAudioFormat) {
                        if (digitalAudioFormat.isNotEmpty()) {
                            val modifiedDigitalAudioFormat = when (digitalAudioFormat.trim()) {
                                "Auto" -> getString(R.string.auto)
                                "Passthrough" -> getString(R.string.passthrough)
                                "Manual" -> getString(R.string.manual)
                                else -> digitalAudioFormat
                            }
                            appendLine(modifiedDigitalAudioFormat)
                        }
                    }

                    if (!isHideEmptyAudioDisplayLine && !isEmptyLine) {
                        appendLine()
                    }

                    if (!isHideDisplayTitle) {
                        if (!isTitleLine) {
                            appendLine("\u200E")
                        }
                    }

                    if (!isHideHdrStatus) {
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
                    }

                    if (!isHideHdrPriority) {
                        if (hdrPriority.isNotEmpty()) {
                            appendLine(hdrPriority)
                        }
                    }

                    if (!isHideHdrPolicy) {
                        if (hdrPolicy.isNotEmpty()) {
                            val modifiedHdrPolicy = when (hdrPolicy.trim()) {
                                "Follow Source" -> getString(R.string.follow_source)
                                "Follow Sink" -> getString(R.string.follow_sink)
                                else -> hdrPolicy
                            }
                            appendLine(modifiedHdrPolicy)
                        }
                    }

                    if (!isHideColorSpace) {
                        if (colorSpace.isNotEmpty()) {
                            val modifiedColorSpace = when (colorSpace.trim()) {
                                "default" -> "YCbCr 4:2:2 (10 Bit)"
                                "YCbCr422 8bit" -> "YCbCr 4:2:2 (8 Bit)"
                                "YCbCr422 10bit" -> "YCbCr 4:2:2 (10 Bit)"
                                "YCbCr422 12bit" -> "YCbCr 4:2:2 (12 Bit)"
                                "YCbCr420 8bit" -> "YCbCr 4:2:0 (8 Bit)"
                                "YCbCr420 10bit" -> "YCbCr 4:2:0 (10 Bit)"
                                "YCbCr420 12bit" -> "YCbCr 4:2:0 (12 Bit)"
                                "YCbCr444 8bit" -> "YCbCr 4:4:4 (8 Bit)"
                                "YCbCr444 10bit" -> "YCbCr 4:4:4 (10 Bit)"
                                "YCbCr444 12bit" -> "YCbCr 4:4:4 (12 Bit)"
                                "RGB 8bit" -> "RGB (8 Bit)"
                                "RGB 10bit" -> "RGB (10 Bit)"
                                "RGB 12bit" -> "RGB (12 Bit)"
                                else -> colorSpace
                            }
                            appendLine(modifiedColorSpace)
                        }
                    }

                    if (!isHideEmptyDisplayProcessorLine && !isEmptyLine) {
                        appendLine()
                    }

                    if (!isHideProcessorTitle) {
                        if (!isTitleLine) {
                            appendLine("\u200E")
                        }
                    }

                    if (!isHideCpuLoad) {
                        if (cpuUsage.isNotEmpty()) {
                            appendLine(cpuUsage)
                        }
                    }

                    if (!isHideCpuClock) {
                        val cpuClockPreference = sharedPreferences.getString("cpu_clock_key", "currentPercentage")
                        val displayText = when (cpuClockPreference) {
                            "percentage" -> cpuPercentage
                            "current" -> cpuCurr
                            "currentPercentage" -> cpuCurrPerc
                            "currentMaximal" -> cpuCurrMax
                            "currentMaximalPerc" -> cpuCurrMaxPerc
                            else -> cpuCurrPerc
                        }

                        appendLine(displayText)
                    }

                    if (isUsbDebuggingEnabled()) {
                        if (!isHideCpuTemperature) {
                            if (cpuTemp.isNotEmpty()) {
                                if (isRequest) {
                                    if (isCelFahr) {
                                        appendLine(getHardwarePropertiesFahrenheit())
                                    } else {
                                        appendLine(getHardwarePropertiesCelsius())
                                    }
                                } else {
                                    if (isCelFahr) {
                                        appendLine(getThermalServiceFahrenheit())
                                    } else {
                                        appendLine(getThermalServiceCelsius())
                                    }
                                }
                            }
                        }
                    }

                    if (!isHideCpuGovernor) {
                        if (cpuGovernor.isNotEmpty()) {
                            appendLine(cpuGovernor)
                        }
                    }

                    if (!isHideEmptyProcessorOtherLine && !isEmptyLine) {
                        appendLine()
                    }

                    if (!isHideOtherTitle) {
                        if (!isTitleLine) {
                            appendLine("\u200E")
                        }
                    }

                    if (!isHideTime) {
                        if (localTime.isNotEmpty()) {
                            appendLine(localTime)
                        }
                    }

                    if (!isHideConnection) {
                        if (getConnectionState().isNotEmpty()) {
                            val modifiedgetConnectionState = when (getConnectionState().trim()) {
                                "WIFI" -> getString(R.string.wifi)
                                "Ethernet" -> getString(R.string.ethernet)
                                else -> getConnectionState()
                            }

                            if (connectionSpeed.isNotEmpty()) {
                                val isMbpsText = sharedPreferences.getBoolean(textMbpsKey, false)

                                if (isMbpsText) {
                                    val speedInMbps = convertSpeedToMbps(connectionSpeed).replace(Regex(","), ".")
                                    val connectionInfo = "$modifiedgetConnectionState | $speedInMbps"
                                    if (modifiedgetConnectionState != getString(R.string.no_connectivity)) {
                                        appendLine(connectionInfo)
                                    } else {
                                        appendLine(getString(R.string.no_connectivity))
                                    }
                                } else {
                                    val formattedSpeed = connectionSpeed.replace(Regex("(\\d)([A-Za-z])"), "$1 $2")
                                    val connectionInfo = "$modifiedgetConnectionState | $formattedSpeed"
                                    if (modifiedgetConnectionState != getString(R.string.no_connectivity)) {
                                        appendLine(connectionInfo)
                                    } else {
                                        appendLine(getString(R.string.no_connectivity))
                                    }
                                }
                            }
                        }
                    }

                    if (!isHideMemoryUsage) {
                        if (memoryUsage.isNotEmpty()) {
                        val memoryUsagePreference = sharedPreferences.getString("memory_usage_key", "mbPercentage")

                        val displayText = when (memoryUsagePreference) {
                            "percentage" -> memoryPercentage
                            "mb" -> memoryMb
                            "mbPercentage" -> memoryMbPercentage
                            "gb" -> memoryGb
                            "gbPercentage" -> memoryGbPercentage
                            else -> memoryMbPercentage
                        }

                        appendLine(displayText)
                        }
                    }

                    if (isUsbDebuggingEnabled()) {
                        if (!isHideAppMemoryUsage) {
                            if (appMemoryUsage.isNotEmpty()) {
                                appendLine(appMemoryUsage)
                            }
                        }
                    }

                    if (!isHideAppName) {
                        if (appName.isNotEmpty()) {
                            appendLine(appName)
                        }
                    }
                }

                val overlayText2 = buildString {
                    if (!isHideVideoTitle) {
                        if (!isTitleLine) {
                            appendLine(getString(R.string.video))
                        }
                    }

                    if (!isHideDisplayResolution) {
                        if (displayResolution.isNotEmpty()) {
                            appendLine(getString(R.string.display_resolution))
                        }
                    }

                    if (!isHideVideoResolution) {
                        if (videoResolution.isNotEmpty()) {
                            appendLine(getString(R.string.video_resolution))
                        }
                    }

                    if (!isHideVideoFormat) {
                        if (videoFormat.isNotEmpty()) {
                            appendLine(getString(R.string.video_format))
                        }
                    }

                    if (!isHideEmptyVideoAudioLine && !isEmptyLine) {
                        appendLine()
                    }

                    if (!isHideAudioTitle) {
                        if (!isTitleLine) {
                            appendLine(getString(R.string.audio))
                        }
                    }

                    if (!isHideAudioMode) {
                        if (audioMode.isNotEmpty()) {
                            appendLine(getString(R.string.audio_mode))
                        }
                    }

                    if (!isHideAudioFormat) {
                        if (digitalAudioFormat.isNotEmpty()) {
                            appendLine(getString(R.string.audio_format))
                        }
                    }

                    if (!isHideEmptyAudioDisplayLine && !isEmptyLine) {
                        appendLine()
                    }

                    if (!isHideDisplayTitle) {
                        if (!isTitleLine) {
                            appendLine(getString(R.string.display))
                        }
                    }

                    if (!isHideHdrStatus) {
                        if (hdrStatus.isNotEmpty()) {
                            appendLine(getString(R.string.hdr_status))
                        }
                    }

                    if (!isHideHdrPriority) {
                        if (hdrPriority.isNotEmpty()) {
                            appendLine(getString(R.string.hdr_priority))
                        }
                    }

                    if (!isHideHdrPolicy) {
                        if (hdrPolicy.isNotEmpty()) {
                            appendLine(getString(R.string.hdr_policy))
                        }
                    }

                    if (!isHideColorSpace) {
                        if (colorSpace.isNotEmpty()) {
                            appendLine(getString(R.string.color_space))
                        }
                    }

                    if (!isHideEmptyDisplayProcessorLine && !isEmptyLine) {
                        appendLine()
                    }

                    if (!isHideProcessorTitle) {
                        if (!isTitleLine) {
                            appendLine(getString(R.string.cpu))
                        }
                    }

                    if (!isHideCpuLoad) {
                        if (cpuUsage.isNotEmpty()) {
                            appendLine(getString(R.string.cpu_usage))
                        }
                    }

                    if (!isHideCpuClock) {
                        appendLine(printCpuIndex())
                    }

                    if (isUsbDebuggingEnabled()) {
                        if (!isHideCpuTemperature) {
                            if (cpuTemp.isNotEmpty()) {
                                appendLine(getString(R.string.cpu_temperature))
                            }
                        }
                    }

                    if (!isHideCpuGovernor) {
                        if (cpuGovernor.isNotEmpty()) {
                            appendLine(getString(R.string.cpu_governor))
                        }
                    }

                    if (!isHideEmptyProcessorOtherLine && !isEmptyLine) {
                        appendLine()
                    }

                    if (!isHideOtherTitle) {
                        if (!isTitleLine) {
                            appendLine(getString(R.string.other))
                        }
                    }

                    if (!isHideTime) {
                        if (localTime.isNotEmpty()) {
                            appendLine(getString(R.string.time))
                        }
                    }

                    if (!isHideConnection) {
                        if (getConnectionState().isNotEmpty()) {
                            appendLine(getString(R.string.connection))

                        }
                    }

                    if (!isHideMemoryUsage) {
                        if (memoryUsage.isNotEmpty()) {
                            appendLine(getString(R.string.memory_usage))
                        }
                    }

                    if (isUsbDebuggingEnabled()) {
                        if (!isHideAppMemoryUsage) {
                            if (appMemoryUsage.isNotEmpty()) {
                                appendLine(getString(R.string.app_memory_usage))
                            }
                        }
                    }

                    if (!isHideAppName) {
                        if (appName.isNotEmpty()) {
                            appendLine(getString(R.string.app_name_tv))
                        }
                    }
                }

                overlayTextView.text = overlayText.trim()
                overlayTextView2.text = overlayText2.trim()

                CoroutineScope(Dispatchers.Main).launch {
                    if (overlayView != null) {
                        handler.postDelayed(updateData, 750)
                    } else {
                        handler.removeCallbacks(updateData)
                    }
                }
            }
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

    private fun updateOverlayKeyButton() {
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
            16 -> KeyEvent.KEYCODE_NUMPAD_MULTIPLY
            17 -> KeyEvent.KEYCODE_F5
            18 -> KeyEvent.KEYCODE_UNKNOWN
            else -> KeyEvent.KEYCODE_BOOKMARK
        }

        sharedPreferences.edit().putString(selectedCodeKey, selectedKeyCodeString).apply()
    }

    private fun updateOverlayTextSize() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textSizeKey = sharedPreferences.getString("text_size_key", "12") ?: "12"
            val textSize = textSizeKey.toFloat()

            overlayTextView.textSize = textSize
            overlayTextView2.textSize = textSize
        }
    }

    private fun updateOverlayTextPadding() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textPaddingKey = sharedPreferences.getString("text_padding_key", "12") ?: "12"
            val textPadding = convertDpToPx(textPaddingKey.toFloat(), this)

            overlayTextView.setPadding(textPadding.toInt(), textPadding.toInt(), textPadding.toInt(), textPadding.toInt())
            overlayTextView2.setPadding(textPadding.toInt(), textPadding.toInt(), textPadding.toInt(), textPadding.toInt())
        }
    }

    private fun updateOverlayMarginWidth() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val marginWidthKey = sharedPreferences.getString("margin_width_key", "14") ?: "14"
            val marginWidth = marginWidthKey.toFloat()

            val scale = resources.displayMetrics.density
            val marginWidthInPx = (marginWidth * scale + 0.5f).toInt()

            val params1 = overlayTextView.layoutParams as ViewGroup.MarginLayoutParams
            params1.rightMargin = marginWidthInPx
            overlayTextView.layoutParams = params1
        }
    }

    private fun updateOverlayMarginHeight() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val marginHeightKey = sharedPreferences.getString("margin_height_key", "14") ?: "14"
            val marginHeight = marginHeightKey.toFloat()

            val scale = resources.displayMetrics.density
            val marginHeightInPx = (marginHeight * scale + 0.5f).toInt()

            val params1 = overlayTextView.layoutParams as ViewGroup.MarginLayoutParams
            params1.topMargin = marginHeightInPx
            overlayTextView.layoutParams = params1

            val params2 = overlayTextView2.layoutParams as ViewGroup.MarginLayoutParams
            params2.topMargin = marginHeightInPx
            overlayTextView2.layoutParams = params2
        }
    }

    private fun updateOverlayMarginBoth() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val marginBothKey = sharedPreferences.getString("margin_both_key", "0") ?: "0"
            val marginBoth = marginBothKey.toFloat()

            val scale = resources.displayMetrics.density
            val marginBothInPx = (marginBoth * scale + 0.5f).toInt()

            val params2 = overlayTextView2.layoutParams as ViewGroup.MarginLayoutParams
            params2.rightMargin = marginBothInPx
            overlayTextView2.layoutParams = params2
        }
    }

    private fun updateOverlayLeftTextColor() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textColorKey = sharedPreferences.getString("text_color_left_key", "#FFFFFF") ?: "#FFFFFF"
            val textColor = Color.parseColor(textColorKey)

            overlayTextView2.setTextColor(textColor)
        }
    }

    private fun updateOverlayRightTextColor() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textColorKey = sharedPreferences.getString("text_color_right_key", "#FFFFFF") ?: "#FFFFFF"
            val textColor = Color.parseColor(textColorKey)

            overlayTextView.setTextColor(textColor)
        }
    }

    private fun updateOverlayLeftTextAlign() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textAlignKey = sharedPreferences.getString("text_align_left_key", "textStart") ?: "textStart"
            val textAlign: Int = when (textAlignKey) {
                "start" -> View.TEXT_ALIGNMENT_TEXT_START
                "center" -> View.TEXT_ALIGNMENT_CENTER
                "end" -> View.TEXT_ALIGNMENT_TEXT_END
                else -> View.TEXT_ALIGNMENT_TEXT_START
            }

            overlayTextView2.textAlignment = textAlign
        }
    }

    private fun updateOverlayRightTextAlign() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textAlignKey = sharedPreferences.getString("text_align_right_key", "textStart") ?: "textStart"
            val textAlign: Int = when (textAlignKey) {
                "start" -> View.TEXT_ALIGNMENT_TEXT_START
                "center" -> View.TEXT_ALIGNMENT_CENTER
                "end" -> View.TEXT_ALIGNMENT_TEXT_END
                else -> View.TEXT_ALIGNMENT_TEXT_START
            }

            overlayTextView.textAlignment = textAlign
        }
    }

    private fun updateOverlayLeftBackground() {
        val isRoundedCornerLeftOverall = sharedPreferences.getBoolean(roundedCornerOverallLeftKey, false)

        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val backgroundColorKey = sharedPreferences.getString("background_color_left_key", "#000000") ?: "#000000"
            val backgroundColor = Color.parseColor(backgroundColorKey)

            val backgroundAlphaKey = sharedPreferences.getString("background_alpha_left_key", "0.9") ?: "0.9"
            val backgroundAlpha = backgroundAlphaKey.toFloatOrNull()?.coerceIn(0.0f, 1.0f) ?: 0.9f

            val roundedCornersKey = sharedPreferences.getString("rounded_corners_left_key", "18") ?: "18"
            val roundedCornersPx = convertDpToPx(roundedCornersKey.toFloat(), this)
            val backgroundDrawable2 = GradientDrawable()

            val backgroundColorWithAlpha = ColorUtils.setAlphaComponent(backgroundColor, (backgroundAlpha * 255).toInt())
            backgroundDrawable2.setColor(backgroundColorWithAlpha)

            if (isRoundedCornerLeftOverall) {
                backgroundDrawable2.cornerRadii = floatArrayOf(roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx)
            } else {
                backgroundDrawable2.cornerRadii = floatArrayOf(roundedCornersPx, roundedCornersPx, 0f, 0f, 0f, 0f, roundedCornersPx, roundedCornersPx)
            }

            overlayTextView2.background = backgroundDrawable2
        }
    }

    private fun updateOverlayRightBackground() {
        val isRoundedCornerRightOverall = sharedPreferences.getBoolean(roundedCornerOverallRightKey, false)

        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val backgroundColorKey = sharedPreferences.getString("background_color_right_key", "#000000") ?: "#000000"
            val backgroundColor = Color.parseColor(backgroundColorKey)

            val backgroundAlphaKey = sharedPreferences.getString("background_alpha_right_key", "0.9") ?: "0.9"
            val backgroundAlpha = backgroundAlphaKey.toFloatOrNull()?.coerceIn(0.0f, 1.0f) ?: 0.9f

            val roundedCornersKey = sharedPreferences.getString("rounded_corners_right_key", "18") ?: "18"
            val roundedCornersPx = convertDpToPx(roundedCornersKey.toFloat(), this)
            val backgroundDrawable1 = GradientDrawable()

            val backgroundColorWithAlpha = ColorUtils.setAlphaComponent(backgroundColor, (backgroundAlpha * 255).toInt())
            backgroundDrawable1.setColor(backgroundColorWithAlpha)

            if (isRoundedCornerRightOverall) {
                backgroundDrawable1.cornerRadii = floatArrayOf(roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx)
            } else {
                backgroundDrawable1.cornerRadii = floatArrayOf(0f, 0f, roundedCornersPx, roundedCornersPx, roundedCornersPx, roundedCornersPx, 0f, 0f)
            }

            overlayTextView.background = backgroundDrawable1
        }
    }

    private fun updateOverlayTextFont() {
        if (::overlayTextView.isInitialized && ::overlayTextView2.isInitialized) {
            val textFontKey = sharedPreferences.getString("text_font_key", "jetbrainsmono") ?: "jetbrainsmono"
            val fontResId = getFontResourceId(textFontKey)
            overlayTextView.typeface = ResourcesCompat.getFont(this, fontResId)
            overlayTextView2.typeface = ResourcesCompat.getFont(this, fontResId)
        }
    }

    private fun getFontResourceId(fontName: String): Int {
        return when (fontName) {
            "anonymouspro" -> R.font.anonymouspro
            "chakrapetch" -> R.font.chakrapetch
            "comfortaa" -> R.font.comfortaa
            "electrolize" -> R.font.electrolize
            "ibmplexmono" -> R.font.ibmplexmono
            "inter" -> R.font.inter
            "jetbrainsmono" -> R.font.jetbrainsmono
            "kodemono" -> R.font.kodemono
            "martianmono" -> R.font.martianmono
            "ojuju" -> R.font.ojuju
            "overpassmono" -> R.font.overpassmono
            "poetsenone" -> R.font.poetsenone
            "poppins" -> R.font.poppins
            "quicksand" -> R.font.quicksand
            "redditmono" -> R.font.redditmono
            "roboto" -> R.font.roboto
            "robotomono" -> R.font.robotomono
            "sharetechmono" -> R.font.sharetechmono
            "silkscreen" -> R.font.silkscreen
            "sono" -> R.font.sono
            "spacemono" -> R.font.spacemono
            "vt323" -> R.font.vt323
            else -> R.font.jetbrainsmono
        }
    }

    @Suppress("DEPRECATION")
    private fun getConnectionState(): String {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo

        return if (networkInfo != null && networkInfo.isConnected) {
            networkInfo.typeName
        } else {
            getString(R.string.no_connectivity)
        }
    }

    private fun convertSpeedToMbps(speed: String): String {
        val regex = Regex("(\\d+\\.?\\d*)\\s*([kKmM]?[bB]?[iI]?[tT]?[pP]?/[sS])")
        val matchResult = regex.find(speed)

        return if (matchResult != null) {
            val value = matchResult.groupValues[1].toDouble()
            val unit = matchResult.groupValues[2].lowercase()

            val speedInMbps = when (unit) {
                "kb/s" -> value / 125
                "mb/s" -> value * 8
                else -> value
            }

            String.format(getString(R.string.mbps), speedInMbps)
        } else {
            speed
        }
    }

    private fun getCpuFrequency(): List<Long> {
        val cpuFrequencies = mutableListOf<Long>()

        for (i in 0 until Runtime.getRuntime().availableProcessors()) {
            val cpuFreqFilePath = "/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq"
            try {
                val cpuFreqFile = File(cpuFreqFilePath)
                if (cpuFreqFile.exists()) {
                    val frequency = cpuFreqFile.readText().trim().toLong()
                    cpuFrequencies.add(frequency)
                } else {
                    cpuFrequencies.add(0L)
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return cpuFrequencies
    }

    private fun getMinCpuFrequency(): List<Long> {
        val maxCpuFrequencies = mutableListOf<Long>()

        for (i in 0 until Runtime.getRuntime().availableProcessors()) {
            val maxCpuFreqFilePath = "/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq"
            try {
                val maxCpuFreqFile = File(maxCpuFreqFilePath)
                if (maxCpuFreqFile.exists()) {
                    val maxFrequency = maxCpuFreqFile.readText().trim().toLong()
                    maxCpuFrequencies.add(maxFrequency)
                } else {
                    maxCpuFrequencies.add(0L)
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return maxCpuFrequencies
    }

    private fun getMaxCpuFrequency(): List<Long> {
        val maxCpuFrequencies = mutableListOf<Long>()

        for (i in 0 until Runtime.getRuntime().availableProcessors()) {
            val maxCpuFreqFilePath = "/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq"
            try {
                val maxCpuFreqFile = File(maxCpuFreqFilePath)
                if (maxCpuFreqFile.exists()) {
                    val maxFrequency = maxCpuFreqFile.readText().trim().toLong()
                    maxCpuFrequencies.add(maxFrequency)
                } else {
                    maxCpuFrequencies.add(0L)
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return maxCpuFrequencies
    }

    private fun printCpuCurrPerc(): String {
        val frequencies = getCpuFrequency()
        val minFrequencies = getMinCpuFrequency()
        val maxFrequencies = getMaxCpuFrequency()
        return frequencies.mapIndexed { index, frequency ->
            val frequencyInMHz = frequency / 1000
            val minFrequency = minFrequencies[index]
            val maxFrequency = maxFrequencies[index]

            val utilization = if (maxFrequency > minFrequency) {
                val utilizationValue = (frequency - minFrequency).toDouble() / (maxFrequency - minFrequency) * 100
                utilizationValue.toInt()
            } else {
                0
            }

            "$frequencyInMHz MHz ($utilization%)"
        }.joinToString("\n")
    }

    private fun printCpuCurrMaxPerc(): String {
        val frequencies = getCpuFrequency()
        val maxFrequencies = getMaxCpuFrequency()
        return frequencies.mapIndexed { index, frequency ->
            val frequencyInMHz = frequency / 1000
            val maxFrequencyInMHz = maxFrequencies[index] / 1000
            val utilization = if (maxFrequencies[index] > 0) {
                (frequency.toDouble() / maxFrequencies[index] * 100).toInt()
            } else {
                0
            }
            "$frequencyInMHz | $maxFrequencyInMHz MHz ($utilization%)"
        }.joinToString(separator = "\n")
    }

    private fun printCpuCurr(): String {
        val frequencies = getCpuFrequency()
        return frequencies.mapIndexed { _, frequency ->
            val frequencyInMHz = frequency / 1000

            "$frequencyInMHz MHz"
        }.joinToString("\n")
    }

    private fun printCpuCurrMax(): String {
        val frequencies = getCpuFrequency()
        val maxFrequencies = getMaxCpuFrequency()
        return frequencies.mapIndexed { index, frequency ->
            val frequencyInMHz = frequency / 1000
            val maxFrequencyInMHz = maxFrequencies[index] / 1000

            "$frequencyInMHz | $maxFrequencyInMHz MHz"
        }.joinToString(separator = "\n")
    }

    private fun printCpuPercentage(): String {
        val frequencies = getCpuFrequency()
        val minFrequencies = getMinCpuFrequency()
        val maxFrequencies = getMaxCpuFrequency()
        return frequencies.mapIndexed { index, frequency ->
            val minFrequency = minFrequencies[index]
            val maxFrequency = maxFrequencies[index]

            val utilization = if (maxFrequency > minFrequency) {
                val utilizationValue = (frequency - minFrequency).toDouble() / (maxFrequency - minFrequency) * 100
                utilizationValue.toInt()
            } else {
                0
            }

            "$utilization%"
        }.joinToString("\n")
    }

    private fun printCpuIndex(): String {
        val frequencies = getCpuFrequency()
        return List(frequencies.size) { index ->
            getString(R.string.cpu_index, index)
        }.joinToString("\n")
    }

    private fun printCpuGovernor(): String {
        val governorFilePath = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"
        return try {
            val file = File(governorFilePath)
            if (file.exists()) {
                file.readText().trim()
            } else {
                "Governor file does not exist"
            }
        } catch (e: Exception) {
            "Error reading governor: ${e.message}"
        }
    }

    private fun formatMemoryUsagePercentage(memoryUsage: String): String {
        val pattern = """(\d+) / (\d+) \(MB\)""".toRegex()
        val matchResult = pattern.find(memoryUsage)

        return if (matchResult != null) {
            val (usedMB, totalMB) = matchResult.destructured
            val usedMBInt = usedMB.toInt()
            val totalMBInt = totalMB.toInt()
            val percentage = (usedMBInt.toDouble() / totalMBInt) * 100

            "%d%%".format(percentage.toInt())
        } else {
            memoryUsage
        }
    }

    private fun formatMemoryUsageMB(memoryUsage: String): String {
        val formattedMemoryUsage = memoryUsage.replace("(MB)", "MB").replace("/", "|")
        return formattedMemoryUsage
    }

    private fun formatMemoryUsageGB(memoryUsage: String): String {
        val pattern = """(\d+) / (\d+) \(MB\)""".toRegex()
        val matchResult = pattern.find(memoryUsage)

        return if (matchResult != null) {
            val (usedMB, totalMB) = matchResult.destructured
            val usedGB = usedMB.toInt() / 1000.0
            val totalGB = totalMB.toInt() / 1000.0

            "%.2f | %.2f GB".format(usedGB, totalGB)
        } else {
            memoryUsage
        }
    }

    private fun formatMemoryUsageMBPercentage(memoryUsage: String): String {
        val pattern = """(\d+) / (\d+) \(MB\)""".toRegex()
        val matchResult = pattern.find(memoryUsage)

        return if (matchResult != null) {
            val (usedMB, totalMB) = matchResult.destructured
            val usedMBInt = usedMB.toInt()
            val totalMBInt = totalMB.toInt()
            val percentage = (usedMBInt.toDouble() / totalMBInt) * 100

            "%d | %d MB (%d%%)".format(usedMBInt, totalMBInt, percentage.toInt())
        } else {
            memoryUsage
        }
    }

    private fun formatMemoryUsageGBPercentage(memoryUsage: String): String {
        val pattern = """(\d+) / (\d+) \(MB\)""".toRegex()
        val matchResult = pattern.find(memoryUsage)

        return if (matchResult != null) {
            val (usedMB, totalMB) = matchResult.destructured
            val usedMBInt = usedMB.toInt()
            val totalMBInt = totalMB.toInt()
            val usedGB = usedMBInt / 1024.0
            val totalGB = totalMBInt / 1024.0
            val percentage = (usedGB / totalGB) * 100

            "%.2f | %.2f GB (%.0f%%)".format(usedGB, totalGB, percentage)
        } else {
            memoryUsage
        }
    }

    private fun getCurrentTimeFormatted(context: Context): String {
        val now = Date()
        val is24HourFormat = AndroidDateFormat.is24HourFormat(context)
        val isSecondsText = sharedPreferences.getBoolean(textSecondsKey, false)

        val timeFormatPattern = when {
            is24HourFormat && isSecondsText -> "H:mm:ss"
            is24HourFormat -> "H:mm"
            isSecondsText -> "h:mm:ss a"
            else -> "h:mm a"
        }

        val timeFormat = SimpleDateFormat(timeFormatPattern, Locale.getDefault())
        val formattedTime = timeFormat.format(now)

        return formattedTime
    }


    private fun convertDpToPx(dp: Float, context: Context): Float {
        val density = context.resources.displayMetrics.density
        return dp * density
    }

    private fun onKeyCE() {
        connection = null
        stream = null

        myAsyncTask = MyAsyncTask(this)
        myAsyncTask?.cancel()
        myAsyncTask?.execute(ipAddress)
    }

    suspend fun adbCommander(ip: String?) {
        val socket = withContext(Dispatchers.IO) {
            Socket(ip, 5555)
        }
        val crypto = readCryptoConfig(filesDir) ?: writeNewCryptoConfig(filesDir)

        if (crypto == null) {
            Log.d("TAG", "Failed to generate/load RSA key pair")
            return
        }

        try {
            if (stream == null || connection == null) {
                connection = AdbConnection.create(socket, crypto)
                connection?.connect()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
            Thread.currentThread().interrupt()
        }
    }

    private suspend fun appMemoryUsage(): String {
        return withContext(Dispatchers.IO) {
            try {
                val appName = getSystemProperty("sys.nes.info.app_name")
                val memoryUsageStream = connection?.open("shell:dumpsys meminfo $appName | awk '/TOTAL PSS/ {printf \"%.0f MB\", \$3/1024}'")
                val memoryUsageOutputBytes = memoryUsageStream?.read()

                return@withContext memoryUsageOutputBytes?.decodeToString() ?: "-- MB"
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "-- MB"
            }
        }
    }

    private suspend fun getAppMemoryUsage(): String {
        val appMemoryUsage = appMemoryUsage()
        return appMemoryUsage
    }

    private suspend fun thermalServiceCelsius(): String {
        return withContext(Dispatchers.IO) {
            try {
                val thermalServiceStream = connection?.open("shell:dumpsys thermalservice | awk -F= '/mValue/{printf \"%.1f\\n\", \$2}' | sed -n '2p'")
                val thermalServiceOutputBytes = thermalServiceStream?.read()

                return@withContext thermalServiceOutputBytes?.decodeToString()?.replace("\n", "") ?: "--"
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "--"
            }
        }
    }

    private suspend fun getThermalServiceCelsius(): String {
        val thermalServiceCelsius = thermalServiceCelsius()
        return "$thermalServiceCelsius°C"
    }

    private suspend fun hardwarePropertiesCelsius(): String {
        return withContext(Dispatchers.IO) {
            try {
                val hardwarePropertiesStream = connection?.open("shell:dumpsys hardware_properties | grep \"CPU temperatures\" | cut -d \"[\" -f2 | cut -d \"]\" -f1 | awk '{printf(\"%.1f\", \$1)}'\n")
                val hardwarePropertiesOutputBytes = hardwarePropertiesStream?.read()

                return@withContext hardwarePropertiesOutputBytes?.decodeToString()?.replace("\n", "") ?: "--"
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "--"
            }
        }
    }

    private suspend fun getHardwarePropertiesCelsius(): String {
        val hardwarePropertiesCelsius = hardwarePropertiesCelsius()
        return "$hardwarePropertiesCelsius°C"
    }

    private suspend fun thermalServiceFahrenheit(): String {
        return withContext(Dispatchers.IO) {
            try {
                val thermalServiceStream = connection?.open("shell:dumpsys thermalservice | awk -F= '/mValue/{printf \"%.1f\\n\", \$2}' | sed -n '2p'")
                val thermalServiceOutputBytes = thermalServiceStream?.read()
                var thermalServiceMessage: String = thermalServiceOutputBytes?.decodeToString() ?: "--"

                thermalServiceMessage = thermalServiceMessage.replace("\n", "")

                val decimalFormat = DecimalFormat("0.0", DecimalFormatSymbols(Locale.ENGLISH))
                val celsiusTemperature = thermalServiceMessage.toDoubleOrNull() ?: 0.0
                val fahrenheitTemperature = celsiusTemperature * 9/5 + 32

                return@withContext decimalFormat.format(fahrenheitTemperature)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "--"
            }
        }
    }

    private suspend fun getThermalServiceFahrenheit(): String {
        val thermalServiceFahrenheit = thermalServiceFahrenheit()
        return "$thermalServiceFahrenheit°F"
    }

    private suspend fun hardwarePropertiesFahrenheit(): String {
        return withContext(Dispatchers.IO) {
            try {
                val hardwarePropertiesStream = connection?.open("shell:dumpsys hardware_properties | grep \"CPU temperatures\" | cut -d \"[\" -f2 | cut -d \"]\" -f1")
                val hardwarePropertiesOutputBytes = hardwarePropertiesStream?.read()
                var hardwarePropertiesMessage: String = hardwarePropertiesOutputBytes?.decodeToString() ?: "--"

                hardwarePropertiesMessage = hardwarePropertiesMessage.replace("\n", "")

                val decimalFormat = DecimalFormat("0.0", DecimalFormatSymbols(Locale.ENGLISH))
                val celsiusTemperature = hardwarePropertiesMessage.toDoubleOrNull() ?: 0.0
                val fahrenheitTemperature = celsiusTemperature * 9/5 + 32

                return@withContext decimalFormat.format(fahrenheitTemperature)
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext "--"
            }
        }
    }

    private suspend fun getHardwarePropertiesFahrenheit(): String {
        val hardwarePropertiesFahrenheit = hardwarePropertiesFahrenheit()
        return "$hardwarePropertiesFahrenheit°F"
    }

    private fun readCryptoConfig(dataDir: File?): AdbCrypto? {
        val pubKey = File(dataDir, publicKeyName)
        val privKey = File(dataDir, privateKeyName)

        var crypto: AdbCrypto? = null
        if (pubKey.exists() && privKey.exists()) {
            crypto = try {
                AdbCrypto.loadAdbKeyPair(AndroidBase64(), privKey, pubKey)
            } catch (e: Exception) {
                null
            }
        }

        return crypto
    }

    private fun writeNewCryptoConfig(dataDir: File?): AdbCrypto? {
        val pubKey = File(dataDir, publicKeyName)
        val privKey = File(dataDir, privateKeyName)

        var crypto: AdbCrypto?

        try {
            crypto = AdbCrypto.generateAdbKeyPair(AndroidBase64())
            crypto.saveAdbKeyPair(privKey, pubKey)
        } catch (e: Exception) {
            crypto = null
        }

        return crypto
    }

    class MyAsyncTask internal constructor(context: MainActivity) {
        private val activityReference: WeakReference<MainActivity> = WeakReference(context)
        private var job: Job? = null

        fun execute(ip: String?) {
            val activity = activityReference.get() ?: return
            job = CoroutineScope(Dispatchers.IO).launch {
                activity.adbCommander(ip)
            }
            job?.start()
        }

        fun cancel() {
            job?.cancel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceConnection?.let { unbindService(it) }

        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
    }

    class AndroidBase64 : AdbBase64 {
        override fun encodeToString(bArr: ByteArray): String {
            return Base64.encodeToString(bArr, Base64.NO_WRAP)
        }
    }
}
