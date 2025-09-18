package com.shaderlay.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import com.shaderlay.app.shader.ShaderManager

class CustomShaderPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : Preference(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "CustomShaderPreference"
    }

    private var shaderManager: ShaderManager? = null
    private var filePickerLauncher: ActivityResultLauncher<Intent>? = null

    init {
        title = "Shader Selection"
        summary = "Choose built-in or external shader"
        isSelectable = true
    }

    fun setShaderManager(manager: ShaderManager) {
        this.shaderManager = manager
    }

    fun setFilePickerLauncher(launcher: ActivityResultLauncher<Intent>) {
        this.filePickerLauncher = launcher
    }

    override fun onClick() {
        showShaderSelectionDialog()
    }

    private fun showShaderSelectionDialog() {
        val manager = shaderManager ?: return
        val availableShaders = manager.getAvailableShaders()

        val options = mutableListOf<String>()
        val shaderNames = mutableListOf<String>()

        // Add built-in shaders
        options.add("None (Transparent)")
        shaderNames.add("none")

        options.add("CRT Monitor Effect")
        shaderNames.add("crt")

        options.add("Scanlines Effect")
        shaderNames.add("scanlines")

        options.add("LCD Grid Effect")
        shaderNames.add("lcd")

        // Add external shaders
        availableShaders.filter { !listOf("none", "crt", "scanlines", "lcd").contains(it) }
            .forEach { shaderName ->
                val info = manager.getExternalShaderInfo(shaderName)
                options.add("📁 $shaderName${if (info?.isPreset == true) " (Preset)" else ""}")
                shaderNames.add(shaderName)
            }

        // Add option to load new shader
        options.add("📂 Load External Shader...")
        shaderNames.add("__load_external__")

        val currentShader = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(key, "none") ?: "none"

        val selectedIndex = shaderNames.indexOf(currentShader).takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(context)
            .setTitle("Select Shader")
            .setSingleChoiceItems(options.toTypedArray(), selectedIndex) { dialog, which ->
                val selectedShader = shaderNames[which]

                if (selectedShader == "__load_external__") {
                    dialog.dismiss()
                    openFilePicker()
                } else {
                    // Save selection
                    PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putString(key, selectedShader)
                        .apply()

                    updateSummary(selectedShader)
                    notifyChanged()
                    dialog.dismiss()

                    Log.d(TAG, "Selected shader: $selectedShader")
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Manage External") { _, _ ->
                showManageExternalDialog()
            }
            .show()
    }

    private fun openFilePicker() {
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
            filePickerLauncher?.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open file picker", e)

            // Fallback to simpler intent
            val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }

            try {
                filePickerLauncher?.launch(fallbackIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Fallback file picker also failed", e2)
                showErrorDialog("File picker not available")
            }
        }
    }

    fun handleFilePickerResult(uri: Uri?) {
        if (uri == null) {
            Log.d(TAG, "File picker cancelled")
            return
        }

        val manager = shaderManager ?: return

        Log.d(TAG, "Loading shader from URI: $uri")

        val shaderName = manager.loadExternalShader(uri)
        if (shaderName != null) {
            // Save selection
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(key, shaderName)
                .apply()

            updateSummary(shaderName)
            notifyChanged()

            showSuccessDialog("Loaded shader: $shaderName")
        } else {
            showErrorDialog("Failed to load shader file. Make sure it's a valid .slang or .slangp file.")
        }
    }

    private fun showManageExternalDialog() {
        val manager = shaderManager ?: return
        val externalShaders = manager.getAvailableShaders()
            .filter { !listOf("none", "crt", "scanlines", "lcd").contains(it) }

        if (externalShaders.isEmpty()) {
            showInfoDialog("No external shaders loaded", "Load external shaders using the file picker.")
            return
        }

        val options = externalShaders.map { shaderName ->
            val info = manager.getExternalShaderInfo(shaderName)
            "$shaderName${if (info?.isPreset == true) " (Preset)" else ""}"
        }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("Manage External Shaders")
            .setItems(options) { _, which ->
                val shaderName = externalShaders[which]
                showShaderOptionsDialog(shaderName)
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showShaderOptionsDialog(shaderName: String) {
        val manager = shaderManager ?: return

        AlertDialog.Builder(context)
            .setTitle("Shader: $shaderName")
            .setItems(arrayOf("Use This Shader", "Remove Shader")) { _, which ->
                when (which) {
                    0 -> {
                        // Use shader
                        PreferenceManager.getDefaultSharedPreferences(context)
                            .edit()
                            .putString(key, shaderName)
                            .apply()
                        updateSummary(shaderName)
                        notifyChanged()
                    }
                    1 -> {
                        // Remove shader
                        manager.removeExternalShader(shaderName)
                        showInfoDialog("Removed", "Shader '$shaderName' has been removed.")
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateSummary(shaderName: String) {
        summary = when (shaderName) {
            "none" -> "None (Transparent)"
            "crt" -> "CRT Monitor Effect"
            "scanlines" -> "Scanlines Effect"
            "lcd" -> "LCD Grid Effect"
            else -> "External: $shaderName"
        }
    }

    private fun showSuccessDialog(message: String) {
        AlertDialog.Builder(context)
            .setTitle("Success")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(context)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showInfoDialog(title: String, message: String) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onSetInitialValue(defaultValue: Any?) {
        val value = getPersistedString(defaultValue as? String ?: "none")
        updateSummary(value)
    }
}
