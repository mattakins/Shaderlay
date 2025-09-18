package com.shaderlay.app.ui

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import com.shaderlay.app.R
import com.shaderlay.app.shader.ShaderManager

class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.settings_title)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        companion object {
            const val KEY_OVERLAY_ENABLED = "overlay_enabled"
            const val KEY_SHADER_SELECTION = "shader_selection"
            const val KEY_OVERLAY_OPACITY = "overlay_opacity"
            const val KEY_PERFORMANCE_MODE = "performance_mode"
            const val KEY_FPS_LIMIT = "fps_limit"
            const val KEY_AUTO_START = "auto_start"
        }

        private lateinit var shaderManager: ShaderManager
        private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            // Initialize shader manager
            shaderManager = ShaderManager(requireContext())

            // Set up file picker launcher
            filePickerLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val uri = result.data?.data
                handleFilePickerResult(uri)
            }

            setupPreferences()
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        }

        private fun setupPreferences() {
            // Set up custom shader selection preference
            setupCustomShaderPreference()

            // Performance mode
            findPreference<ListPreference>(KEY_PERFORMANCE_MODE)?.apply {
                entries = arrayOf(
                    getString(R.string.performance_mode_high),
                    getString(R.string.performance_mode_balanced),
                    getString(R.string.performance_mode_battery)
                )
                entryValues = arrayOf("high", "balanced", "battery")
                setDefaultValue("balanced")
                summary = "%s"
            }

            // FPS limit
            findPreference<ListPreference>(KEY_FPS_LIMIT)?.apply {
                entries = arrayOf("30 FPS", "60 FPS", "120 FPS", "Unlimited")
                entryValues = arrayOf("30", "60", "120", "-1")
                setDefaultValue("60")
                summary = "%s"
            }

            // Opacity
            findPreference<SeekBarPreference>(KEY_OVERLAY_OPACITY)?.apply {
                min = 0
                max = 100
                setDefaultValue(50)
                showSeekBarValue = true
                title = getString(R.string.overlay_opacity)
                summary = "Adjust overlay transparency"
            }

            updatePreferenceSummaries()
        }

        private fun setupCustomShaderPreference() {
            // Remove the old ListPreference if it exists
            findPreference<ListPreference>(KEY_SHADER_SELECTION)?.let { oldPref ->
                preferenceScreen.removePreference(oldPref)
            }

            // Create and add custom shader preference
            val customShaderPref = CustomShaderPreference(requireContext()).apply {
                key = KEY_SHADER_SELECTION
                setShaderManager(shaderManager)
                setFilePickerLauncher(filePickerLauncher)
                order = 0 // Place at top of overlay settings
            }

            // Add to the overlay settings category
            val overlayCategory = findPreference<androidx.preference.PreferenceCategory>("overlay_settings")
            overlayCategory?.addPreference(customShaderPref)
        }

        private fun handleFilePickerResult(uri: Uri?) {
            findPreference<CustomShaderPreference>(KEY_SHADER_SELECTION)?.handleFilePickerResult(uri)
        }

        private fun updatePreferenceSummaries() {
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

            // Update performance mode summary
            findPreference<ListPreference>(KEY_PERFORMANCE_MODE)?.let { pref ->
                val value = sharedPrefs.getString(pref.key, "balanced")
                val index = pref.findIndexOfValue(value)
                if (index >= 0) {
                    pref.summary = pref.entries[index]
                }
            }

            // Update FPS limit summary
            findPreference<ListPreference>(KEY_FPS_LIMIT)?.let { pref ->
                val value = sharedPrefs.getString(pref.key, "60")
                val index = pref.findIndexOfValue(value)
                if (index >= 0) {
                    pref.summary = pref.entries[index]
                }
            }

            // Update opacity summary
            findPreference<SeekBarPreference>(KEY_OVERLAY_OPACITY)?.let { pref ->
                val value = sharedPrefs.getInt(pref.key, 50)
                pref.summary = "Transparency: ${value}%"
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            Log.d(TAG, "Preference changed: $key")

            when (key) {
                KEY_SHADER_SELECTION -> {
                    val shader = sharedPreferences?.getString(key, "none") ?: "none"
                    Log.d(TAG, "Shader changed to: $shader")
                    // Update overlay shader in real-time
                }

                KEY_OVERLAY_OPACITY -> {
                    val opacity = sharedPreferences?.getInt(key, 50) ?: 50
                    Log.d(TAG, "Opacity changed to: $opacity")
                    // Update overlay opacity in real-time
                }

                KEY_PERFORMANCE_MODE -> {
                    val mode = sharedPreferences?.getString(key, "balanced") ?: "balanced"
                    Log.d(TAG, "Performance mode changed to: $mode")
                    // Update performance mode in real-time
                }

                KEY_FPS_LIMIT -> {
                    val fps = sharedPreferences?.getString(key, "60") ?: "60"
                    Log.d(TAG, "FPS limit changed to: $fps")
                    // Update FPS limit in real-time
                }
            }

            updatePreferenceSummaries()
        }
    }
}
