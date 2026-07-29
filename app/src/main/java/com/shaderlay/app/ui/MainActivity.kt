package com.shaderlay.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import android.widget.TextView
import com.shaderlay.app.R
import com.shaderlay.app.service.OverlayService

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_OVERLAY_PERMISSION = 1001
        private const val REQUEST_STORAGE_PERMISSION = 1002
    }

    private lateinit var overlaySwitch: MaterialSwitch
    private lateinit var openShaderButton: MaterialButton
    private lateinit var settingsButton: MaterialButton
    private lateinit var currentShaderDisplay: TextView

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Log.d(TAG, "Overlay permission granted")
                enableOverlayControls()
            } else {
                Log.d(TAG, "Overlay permission denied")
                showPermissionDeniedDialog()
            }
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                handleShaderFileSelection(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupListeners()
        checkPermissions()
    }

    private fun initializeViews() {
        overlaySwitch = findViewById(R.id.switch_overlay)
        openShaderButton = findViewById(R.id.button_open_shader)
        settingsButton = findViewById(R.id.button_settings)
        currentShaderDisplay = findViewById(R.id.current_shader_display)

        updateShaderDisplay()
    }

    private fun setupListeners() {
        overlaySwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startOverlayService()
            } else {
                stopOverlayService()
            }
        }

        openShaderButton.setOnClickListener {
            openShaderFilePicker()
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlayPermission()
                return
            }
        }

        // Check notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_OVERLAY_PERMISSION
                )
                return
            }
        }

        // Check storage permission for external shaders
        checkStoragePermissions()

        enableOverlayControls()
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AlertDialog.Builder(this)
                .setTitle(R.string.permission_overlay_title)
                .setMessage(R.string.permission_overlay_message)
                .setPositiveButton(R.string.permission_grant) { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    overlayPermissionLauncher.launch(intent)
                }
                .setNegativeButton(R.string.permission_cancel) { _, _ ->
                    showPermissionDeniedDialog()
                }
                .setCancelable(false)
                .show()
        } else {
            enableOverlayControls()
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Overlay permission is required for the app to function properly.")
            .setPositiveButton("Try Again") { _, _ ->
                requestOverlayPermission()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun enableOverlayControls() {
        overlaySwitch.isEnabled = true
        openShaderButton.isEnabled = true
        settingsButton.isEnabled = true
        Log.d(TAG, "Overlay controls enabled")
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_START_OVERLAY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        Toast.makeText(this, "Shader overlay started", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Started overlay service")
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP_OVERLAY
        }
        startService(intent)

        Toast.makeText(this, "Shader overlay stopped", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "Stopped overlay service")
    }

    private fun checkStoragePermissions() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+ uses granular media permissions
                val permissions = arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )

                val missingPermissions = permissions.filter {
                    ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                }

                if (missingPermissions.isNotEmpty()) {
                    ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), REQUEST_STORAGE_PERMISSION)
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6-12 uses READ_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        REQUEST_STORAGE_PERMISSION
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_OVERLAY_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Notification permission granted")
                    enableOverlayControls()
                } else {
                    Log.d(TAG, "Notification permission denied")
                    // Still enable overlay controls, notification permission is not critical
                    enableOverlayControls()
                }
            }
            REQUEST_STORAGE_PERMISSION -> {
                val granted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                if (granted) {
                    Log.d(TAG, "Storage permissions granted")
                    Toast.makeText(this, "Storage access granted - you can now load external shaders", Toast.LENGTH_LONG).show()
                } else {
                    Log.d(TAG, "Storage permissions denied")
                    Toast.makeText(this, "Storage access denied - external shader loading may not work", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openShaderFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "text/plain",
                "text/*",
                "application/octet-stream"
            ))
        }

        try {
            filePickerLauncher.launch(Intent.createChooser(intent, "Select Shader File"))
        } catch (e: Exception) {
            Toast.makeText(this, "No file manager app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleShaderFileSelection(uri: Uri) {
        Log.d(TAG, "Shader file selected: $uri")

        // Store the selected shader URI in the default SharedPreferences
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit().putString("selected_shader_uri", uri.toString()).apply()
        prefs.edit().putString("shader_selection", "external").apply()

        Toast.makeText(this, "Shader loaded: ${uri.lastPathSegment}", Toast.LENGTH_SHORT).show()

        // Update the shader display
        updateShaderDisplay()

        // If overlay is active, restart it to apply the new shader
        if (overlaySwitch.isChecked) {
            stopOverlayService()
            startOverlayService()
        }
    }

    private fun updateShaderDisplay() {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this)
        val shaderSelection = prefs.getString("shader_selection", "red_test") ?: "red_test"

        val displayText = when (shaderSelection) {
            "external" -> {
                val shaderUri = prefs.getString("selected_shader_uri", null)
                if (shaderUri != null) {
                    val uri = Uri.parse(shaderUri)
                    "Current shader: ${uri.lastPathSegment ?: "external"}"
                } else {
                    "Current shader: red_test (fallback)"
                }
            }
            "red_test" -> "Current shader: red_test (debug overlay)"
            "crt" -> "Current shader: crt (CRT monitor effect)"
            "scanlines" -> "Current shader: scanlines (retro scanlines)"
            "lcd" -> "Current shader: lcd (LCD grid pattern)"
            "none" -> "Current shader: none (transparent)"
            else -> "Current shader: $shaderSelection"
        }

        currentShaderDisplay.text = displayText
    }

    override fun onResume() {
        super.onResume()

        // Update shader display in case preferences changed
        updateShaderDisplay()

        // Update switch state based on service running state
        // This is a simplified check - in a real app you might want to track service state more precisely
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val canOverlay = Settings.canDrawOverlays(this)
            overlaySwitch.isEnabled = canOverlay
            openShaderButton.isEnabled = canOverlay
            settingsButton.isEnabled = canOverlay
        }
    }
}
