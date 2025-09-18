package com.shaderlay.app.renderer

import android.content.Context
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log

class GLOverlaySurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    companion object {
        private const val TAG = "GLOverlaySurfaceView"
    }

    private val shaderRenderer: ShaderRenderer

    init {
        Log.d(TAG, "Initializing GLOverlaySurfaceView")

        // Configure for transparency
        setZOrderOnTop(true)
        holder.setFormat(PixelFormat.RGBA_8888)

        // Set OpenGL ES version
        setEGLContextClientVersion(2)

        // Configure EGL for transparency
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)

        // Create and set renderer
        shaderRenderer = ShaderRenderer(context)
        setRenderer(shaderRenderer)

        // Set render mode to continuous for smooth animation
        renderMode = RENDERMODE_CONTINUOUSLY

        Log.d(TAG, "GLOverlaySurfaceView initialized successfully")
    }

    fun updateShader(shaderName: String) {
        queueEvent {
            shaderRenderer.loadShader(shaderName)
        }
    }

    fun updateOpacity(opacity: Float) {
        queueEvent {
            shaderRenderer.setOpacity(opacity)
        }
    }

    fun updatePerformanceMode(mode: ShaderRenderer.PerformanceMode) {
        queueEvent {
            shaderRenderer.setPerformanceMode(mode)
        }
    }

    fun onDestroy() {
        Log.d(TAG, "Destroying GLOverlaySurfaceView")

        queueEvent {
            shaderRenderer.cleanup()
        }

        onPause()
    }

    override fun onResume() {
        Log.d(TAG, "GLOverlaySurfaceView onResume")
        super.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "GLOverlaySurfaceView onPause")
        super.onPause()
    }
}
