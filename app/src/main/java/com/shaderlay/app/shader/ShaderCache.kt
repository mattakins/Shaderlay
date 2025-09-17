package com.shaderlay.app.shader

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class ShaderCache(private val context: Context) {

    companion object {
        private const val TAG = "ShaderCache"
        private const val CACHE_DIR = "shader_cache"
        private const val CACHE_VERSION = 1
    }

    private val memoryCache = ConcurrentHashMap<String, CachedShader>()
    private val cacheDir: File

    init {
        cacheDir = File(context.cacheDir, CACHE_DIR)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        cleanupOldCache()
    }

    data class CachedShader(
        val compiledSource: String,
        val timestamp: Long,
        val sourceHash: String,
        val version: Int = CACHE_VERSION
    )

    fun getCompiledShader(originalSource: String, shaderType: Int): String? {
        val key = generateCacheKey(originalSource, shaderType)

        // Check memory cache first
        memoryCache[key]?.let { cached ->
            if (isValidCache(cached, originalSource)) {
                Log.d(TAG, "Cache hit (memory): $key")
                return cached.compiledSource
            } else {
                memoryCache.remove(key)
            }
        }

        // Check disk cache
        val diskCached = loadFromDisk(key)
        if (diskCached != null && isValidCache(diskCached, originalSource)) {
            Log.d(TAG, "Cache hit (disk): $key")
            memoryCache[key] = diskCached
            return diskCached.compiledSource
        }

        Log.d(TAG, "Cache miss: $key")
        return null
    }

    fun putCompiledShader(originalSource: String, shaderType: Int, compiledSource: String) {
        val key = generateCacheKey(originalSource, shaderType)
        val sourceHash = hashString(originalSource)
        val cached = CachedShader(
            compiledSource = compiledSource,
            timestamp = System.currentTimeMillis(),
            sourceHash = sourceHash
        )

        // Store in memory cache
        memoryCache[key] = cached

        // Store in disk cache
        saveToDisk(key, cached)

        Log.d(TAG, "Cached shader: $key")
    }

    fun clearCache() {
        Log.d(TAG, "Clearing shader cache")

        memoryCache.clear()

        cacheDir.listFiles()?.forEach { file ->
            file.delete()
        }
    }

    fun getCacheStats(): CacheStats {
        val diskFiles = cacheDir.listFiles()?.size ?: 0
        val totalDiskSize = cacheDir.listFiles()?.sumOf { it.length() } ?: 0L

        return CacheStats(
            memoryEntries = memoryCache.size,
            diskEntries = diskFiles,
            totalDiskSize = totalDiskSize
        )
    }

    private fun generateCacheKey(source: String, shaderType: Int): String {
        val sourceHash = hashString(source)
        return "${shaderType}_${sourceHash}"
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun isValidCache(cached: CachedShader, originalSource: String): Boolean {
        // Check version compatibility
        if (cached.version != CACHE_VERSION) {
            return false
        }

        // Check if source has changed
        val currentHash = hashString(originalSource)
        if (cached.sourceHash != currentHash) {
            return false
        }

        // Check age (cache for 7 days)
        val maxAge = 7 * 24 * 60 * 60 * 1000L // 7 days in milliseconds
        val age = System.currentTimeMillis() - cached.timestamp
        if (age > maxAge) {
            return false
        }

        return true
    }

    private fun loadFromDisk(key: String): CachedShader? {
        val file = File(cacheDir, "$key.cache")
        if (!file.exists()) {
            return null
        }

        return try {
            FileInputStream(file).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    ois.readObject() as CachedShader
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cache file: $key", e)
            file.delete()
            null
        }
    }

    private fun saveToDisk(key: String, cached: CachedShader) {
        val file = File(cacheDir, "$key.cache")

        try {
            FileOutputStream(file).use { fos ->
                ObjectOutputStream(fos).use { oos ->
                    oos.writeObject(cached)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save cache file: $key", e)
        }
    }

    private fun cleanupOldCache() {
        // Remove cache files older than 7 days
        val maxAge = 7 * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        cacheDir.listFiles()?.forEach { file ->
            if (currentTime - file.lastModified() > maxAge) {
                file.delete()
                Log.d(TAG, "Removed old cache file: ${file.name}")
            }
        }

        // Limit memory cache size
        if (memoryCache.size > 50) {
            val sortedEntries = memoryCache.entries.sortedBy { it.value.timestamp }
            val toRemove = sortedEntries.take(memoryCache.size - 50)
            toRemove.forEach { entry ->
                memoryCache.remove(entry.key)
            }
            Log.d(TAG, "Trimmed memory cache, removed ${toRemove.size} entries")
        }
    }

    data class CacheStats(
        val memoryEntries: Int,
        val diskEntries: Int,
        val totalDiskSize: Long
    )
}