package com.shaderlay.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.preference.PreferenceManager
import com.shaderlay.app.service.OverlayService
import com.shaderlay.app.ui.SettingsActivity

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received broadcast: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                handleBootCompleted(context)
            }
        }
    }

    private fun handleBootCompleted(context: Context) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val autoStart = sharedPrefs.getBoolean(SettingsActivity.SettingsFragment.KEY_AUTO_START, false)

        if (autoStart) {
            Log.d(TAG, "Auto-starting overlay service")

            val intent = Intent(context, OverlayService::class.java).apply {
                action = OverlayService.ACTION_START_OVERLAY
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d(TAG, "Overlay service started successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start overlay service", e)
            }
        } else {
            Log.d(TAG, "Auto-start disabled, not starting overlay service")
        }
    }
}