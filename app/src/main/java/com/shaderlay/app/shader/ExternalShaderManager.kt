package com.shaderlay.app.shader

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.InputStreamReader

class ExternalShaderManager(private val context: Context) {

    companion object {
        private const val TAG = "ExternalShaderManager"
    }

    data class ExternalShader(
        val name: String,
        val uri: Uri,
        val isPreset: Boolean,
        val presetContent: String? = null,
        val shaderPaths: List<String> = emptyList()
    )

    fun loadShaderFromUri(uri: Uri): ExternalShader? {
        return try {
            val documentFile = DocumentFile.fromSingleUri(context, uri)
            val fileName = documentFile?.name ?: return null

            Log.d(TAG, "Loading shader from: $fileName")

            when {
                fileName.endsWith(".slangp") -> loadPresetFile(uri, fileName)
                fileName.endsWith(".slang") -> loadShaderFile(uri, fileName)
                else -> {
                    Log.w(TAG, "Unsupported file type: $fileName")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load shader from URI: $uri", e)
            null
        }
    }

    private fun loadPresetFile(uri: Uri, fileName: String): ExternalShader? {
        val content = readFileContent(uri) ?: return null

        return ExternalShader(
            name = fileName.removeSuffix(".slangp"),
            uri = uri,
            isPreset = true,
            presetContent = content
        )
    }

    private fun loadShaderFile(uri: Uri, fileName: String): ExternalShader? {
        val content = readFileContent(uri) ?: return null

        return ExternalShader(
            name = fileName.removeSuffix(".slang"),
            uri = uri,
            isPreset = false,
            shaderPaths = listOf(content)
        )
    }

    fun readFileContent(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read file content", e)
            null
        }
    }

    fun parseExternalPreset(content: String, baseUri: Uri): ParsedPreset? {
        return try {
            val preset = ParsedPreset()
            val lines = content.lines()

            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

                val parts = trimmed.split("=", limit = 2)
                if (parts.size != 2) continue

                val key = parts[0].trim()
                val value = parts[1].trim().removeSurrounding("\"")

                when {
                    key == "shaders" -> preset.shaderCount = value.toIntOrNull() ?: 0
                    key.startsWith("shader") && !key.contains("_") -> {
                        val index = key.removePrefix("shader").toIntOrNull() ?: continue
                        preset.shaderPaths[index] = value
                    }
                    key.startsWith("filter_linear") -> {
                        val index = key.removePrefix("filter_linear").toIntOrNull() ?: continue
                        preset.filterLinear[index] = value.toBoolean()
                    }
                    key.startsWith("scale_type") -> {
                        val index = key.removePrefix("scale_type").toIntOrNull() ?: continue
                        preset.scaleTypes[index] = value
                    }
                    key.startsWith("scale") && !key.contains("_") -> {
                        val index = key.removePrefix("scale").toIntOrNull() ?: continue
                        preset.scales[index] = value.toFloatOrNull() ?: 1.0f
                    }
                }
            }

            preset
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse preset", e)
            null
        }
    }

    data class ParsedPreset(
        var shaderCount: Int = 0,
        val shaderPaths: MutableMap<Int, String> = mutableMapOf(),
        val filterLinear: MutableMap<Int, Boolean> = mutableMapOf(),
        val scaleTypes: MutableMap<Int, String> = mutableMapOf(),
        val scales: MutableMap<Int, Float> = mutableMapOf()
    )

    fun loadShaderContentFromPreset(presetUri: Uri, shaderPath: String): String? {
        return try {
            // Try to resolve shader path relative to preset file
            val parentUri = getParentUri(presetUri) ?: return null
            val shaderUri = DocumentFile.fromTreeUri(context, parentUri)
                ?.findFile(shaderPath)?.uri ?: return null

            readFileContent(shaderUri)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load shader content: $shaderPath", e)
            null
        }
    }

    private fun getParentUri(uri: Uri): Uri? {
        return try {
            val documentFile = DocumentFile.fromSingleUri(context, uri)
            documentFile?.parentFile?.uri
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get parent URI", e)
            null
        }
    }

    fun validateShaderFile(content: String): Boolean {
        return content.contains("#version") &&
               (content.contains("void main") || content.contains("main()"))
    }

    fun getSupportedFileTypes(): Array<String> {
        return arrayOf("*/*") // Allow all files, we'll filter by extension
    }

    fun getMimeTypes(): Array<String> {
        return arrayOf(
            "text/plain",
            "text/*",
            "application/octet-stream",
            "*/*"
        )
    }
}