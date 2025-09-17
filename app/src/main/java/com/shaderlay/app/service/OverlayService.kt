package com.shaderlay.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.shaderlay.app.R
import com.shaderlay.app.renderer.GLOverlaySurfaceView
import com.shaderlay.app.ui.MainActivity

class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "overlay_service_channel"

        const val ACTION_START_OVERLAY = "com.shaderlay.app.START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "com.shaderlay.app.STOP_OVERLAY"
        const val ACTION_TOGGLE_OVERLAY = "com.shaderlay.app.TOGGLE_OVERLAY"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: GLOverlaySurfaceView? = null
    private var isOverlayActive = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "OverlayService created")

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_OVERLAY -> startOverlay()
            ACTION_STOP_OVERLAY -> stopOverlay()
            ACTION_TOGGLE_OVERLAY -> toggleOverlay()
            else -> startOverlay()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startOverlay() {
        if (isOverlayActive) {
            Log.d(TAG, "Overlay already active")
            return
        }

        try {
            // Create overlay view
            overlayView = GLOverlaySurfaceView(this)

            // Configure window layout parameters
            val layoutParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                format = PixelFormat.RGBA_8888

                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                       WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                       WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                       WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                       WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 0
            }

            // Add overlay to window manager
            windowManager?.addView(overlayView, layoutParams)
            isOverlayActive = true

            // Start foreground service
            startForeground(NOTIFICATION_ID, createNotification())

            Log.d(TAG, "Overlay started successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start overlay", e)
            stopSelf()
        }
    }

    private fun stopOverlay() {
        if (!isOverlayActive) {
            Log.d(TAG, "Overlay not active")
            return
        }

        try {
            overlayView?.let { view ->
                windowManager?.removeView(view)
                view.onDestroy()
            }
            overlayView = null
            isOverlayActive = false

            Log.d(TAG, "Overlay stopped successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop overlay", e)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun toggleOverlay() {
        if (isOverlayActive) {
            stopOverlay()
        } else {
            startOverlay()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_service_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_service_description)
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, OverlayService::class.java).apply {
            action = ACTION_TOGGLE_OVERLAY
        }

        val togglePendingIntent = PendingIntent.getService(
            this, 1, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.overlay_notification_title))
            .setContentText(getString(R.string.overlay_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(mainPendingIntent)
            .addAction(
                R.drawable.ic_toggle,
                if (isOverlayActive) "Stop" else "Start",
                togglePendingIntent
            )
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        Log.d(TAG, "OverlayService destroyed")
        stopOverlay()
        super.onDestroy()
    }
}