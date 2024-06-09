package com.jamal2367.videoinfooverlay

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.beginTransaction().replace(android.R.id.content, SettingsFragment()).commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private var hideLeftOverlayPreference: CheckBoxPreference? = null
        private var emptyTitlePreference: CheckBoxPreference? = null
        private var emptyLinePreference: CheckBoxPreference? = null
        private var roundedCornerOverallLeftPreference: CheckBoxPreference? = null
        private var roundedCornerOverallRightPreference: CheckBoxPreference? = null
        private var marginBothPreference: ListPreference? = null
        private var textColorLeftPreference: ListPreference? = null
        private var textAlignLeftPreference: ListPreference? = null
        private var backgroundColorLeftPreference: ListPreference? = null
        private var backgroundAlphaLeftPreference: ListPreference? = null
        private var roundedCornersLeftPreference: ListPreference? = null

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.activity_settings, rootKey)

            // Find the preferences
            hideLeftOverlayPreference = findPreference("hide_left_overlay_key")
            emptyTitlePreference = findPreference("empty_title_key")
            emptyLinePreference = findPreference("empty_line_key")
            roundedCornerOverallLeftPreference = findPreference("rounded_corner_overall_left_key")
            roundedCornerOverallRightPreference = findPreference("rounded_corner_overall_right_key")
            marginBothPreference = findPreference("margin_both_key")
            textColorLeftPreference = findPreference("text_color_left_key")
            textAlignLeftPreference = findPreference("text_align_left_key")
            backgroundColorLeftPreference = findPreference("background_color_left_key")
            backgroundAlphaLeftPreference = findPreference("background_alpha_left_key")
            roundedCornersLeftPreference = findPreference("rounded_corners_left_key")

            // Check if preferences are not null
            if (hideLeftOverlayPreference == null || emptyTitlePreference == null) {
                throw IllegalStateException("Preferences not found in XML")
            }

            // Set initial state of emptyTitlePreference
            updateEmptyTitleState()

            // Listener for hideLeftOverlayPreference
            hideLeftOverlayPreference?.setOnPreferenceChangeListener { _, newValue ->
                val hideLeftOverlay = newValue as Boolean
                updateEmptyTitleState(hideLeftOverlay)
                true
            }

            // Set up "App Version" preference
            val preferenceAppVersion = findPreference<Preference>("pref_version_key")
            val packageName = requireContext().packageName
            val packageInfo = requireContext().packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName

            preferenceAppVersion?.title = getString(R.string.app_name) + " $versionName"
            preferenceAppVersion?.summary = "https://github.com/jamal2362/SEI804_VideoInfoOverlay"
            preferenceAppVersion?.setIcon(R.drawable.ic_info_24dp)
            preferenceAppVersion?.setOnPreferenceClickListener {
                Toast.makeText(requireContext(), "❤", Toast.LENGTH_LONG).show()
                true
            }

            hideLeftOverlayPreference?.setOnPreferenceClickListener {
                Toast.makeText(requireContext(), getString(R.string.restart_overlay_toast), Toast.LENGTH_SHORT).show()
                true
            }

            roundedCornerOverallLeftPreference?.setOnPreferenceClickListener {
                Toast.makeText(requireContext(), getString(R.string.restart_overlay_toast), Toast.LENGTH_SHORT).show()
                true
            }

            roundedCornerOverallRightPreference?.setOnPreferenceClickListener {
                Toast.makeText(requireContext(), getString(R.string.restart_overlay_toast), Toast.LENGTH_SHORT).show()
                true
            }
        }

        private fun updateEmptyTitleState(hideLeftOverlay: Boolean = hideLeftOverlayPreference?.isChecked ?: false) {

            emptyTitlePreference?.let {
                if (hideLeftOverlay) {
                    it.isChecked = false
                    it.isEnabled = false
                } else {
                    it.isEnabled = true
                }
            }

            roundedCornerOverallRightPreference?.let {
                if (hideLeftOverlay) {
                    it.isChecked = true
                    it.isEnabled = false
                } else {
                    it.isEnabled = true
                }
            }

            emptyLinePreference?.let {
                if (hideLeftOverlay) {
                    it.isChecked = false
                }
            }

            roundedCornerOverallLeftPreference?.isEnabled = !hideLeftOverlay
            marginBothPreference?.isEnabled = !hideLeftOverlay
            textColorLeftPreference?.isEnabled = !hideLeftOverlay
            textAlignLeftPreference?.isEnabled = !hideLeftOverlay
            backgroundColorLeftPreference?.isEnabled = !hideLeftOverlay
            backgroundAlphaLeftPreference?.isEnabled = !hideLeftOverlay
            roundedCornersLeftPreference?.isEnabled = !hideLeftOverlay
        }
    }
}
