package com.shaderlay.app.renderer

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface

class GLOverlayTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : TextureView(context, attrs), TextureView.SurfaceTextureListener {

    companion object {
        private const val TAG = "GLOverlayTextureView"
    }

    private var shaderRenderer: ShaderRenderer? = null
    private var renderThread: GLRenderThread? = null
    private var isRendering = false

    init {
        Log.d(TAG, "Initializing GLOverlayTextureView")

        // Configure TextureView for transparency
        isOpaque = false
        surfaceTextureListener = this

        // Create shader renderer
        shaderRenderer = ShaderRenderer(context)

        Log.d(TAG, "GLOverlayTextureView initialized successfully")
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        Log.d(TAG, "Surface texture available: ${width}x${height}")

        renderThread = GLRenderThread(surface, width, height, shaderRenderer!!)
        renderThread?.start()
        isRendering = true
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        Log.d(TAG, "Surface texture size changed: ${width}x${height}")
        renderThread?.updateSize(width, height)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        Log.d(TAG, "Surface texture destroyed")

        renderThread?.stopRendering()
        renderThread = null
        isRendering = false
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // Called each frame - no action needed
    }

    fun updateShader(shaderName: String) {
        renderThread?.updateShader(shaderName)
    }

    fun updateOpacity(opacity: Float) {
        renderThread?.updateOpacity(opacity)
    }

    fun updatePerformanceMode(mode: ShaderRenderer.PerformanceMode) {
        renderThread?.updatePerformanceMode(mode)
    }

    fun onDestroy() {
        Log.d(TAG, "Destroying GLOverlayTextureView")

        renderThread?.stopRendering()
        renderThread = null
        shaderRenderer?.cleanup()
        shaderRenderer = null
    }

    private class GLRenderThread(
        private val surfaceTexture: SurfaceTexture,
        private var width: Int,
        private var height: Int,
        private val renderer: ShaderRenderer
    ) : Thread("GLRenderThread") {

        private var running = false
        private var egl: EGL10? = null
        private var eglDisplay: EGLDisplay? = null
        private var eglContext: EGLContext? = null
        private var eglSurface: EGLSurface? = null

        override fun run() {
            Log.d(TAG, "GL render thread started")

            if (!initEGL()) {
                Log.e(TAG, "Failed to initialize EGL")
                return
            }

            // Initialize renderer
            renderer.onSurfaceCreated(null, null)
            renderer.onSurfaceChanged(null, width, height)

            running = true
            var lastFrameTime = System.currentTimeMillis()

            while (running) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = currentTime - lastFrameTime

                // Target 60 FPS (16.67ms per frame)
                if (deltaTime >= 16) {
                    // Render frame
                    renderer.onDrawFrame(null)

                    // Swap buffers
                    egl?.eglSwapBuffers(eglDisplay, eglSurface)

                    lastFrameTime = currentTime
                }

                // Small sleep to prevent busy waiting
                try {
                    sleep(1)
                } catch (e: InterruptedException) {
                    break
                }
            }

            cleanup()
            Log.d(TAG, "GL render thread finished")
        }

        private fun initEGL(): Boolean {
            try {
                egl = EGLContext.getEGL() as EGL10
                eglDisplay = egl?.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

                if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
                    Log.e(TAG, "eglGetDisplay failed")
                    return false
                }

                val version = IntArray(2)
                if (!egl!!.eglInitialize(eglDisplay, version)) {
                    Log.e(TAG, "eglInitialize failed")
                    return false
                }

                val configs = arrayOfNulls<EGLConfig>(1)
                val numConfigs = IntArray(1)
                val configAttribs = intArrayOf(
                    EGL10.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES2_BIT
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 16,
                    EGL10.EGL_STENCIL_SIZE, 0,
                    EGL10.EGL_NONE
                )

                if (!egl!!.eglChooseConfig(eglDisplay, configAttribs, configs, 1, numConfigs)) {
                    Log.e(TAG, "eglChooseConfig failed")
                    return false
                }

                val contextAttribs = intArrayOf(
                    0x3098, 2, // EGL_CONTEXT_CLIENT_VERSION
                    EGL10.EGL_NONE
                )

                eglContext = egl!!.eglCreateContext(
                    eglDisplay,
                    configs[0],
                    EGL10.EGL_NO_CONTEXT,
                    contextAttribs
                )

                if (eglContext == EGL10.EGL_NO_CONTEXT) {
                    Log.e(TAG, "eglCreateContext failed")
                    return false
                }

                eglSurface = egl!!.eglCreateWindowSurface(
                    eglDisplay,
                    configs[0],
                    surfaceTexture,
                    null
                )

                if (eglSurface == EGL10.EGL_NO_SURFACE) {
                    Log.e(TAG, "eglCreateWindowSurface failed")
                    return false
                }

                if (!egl!!.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                    Log.e(TAG, "eglMakeCurrent failed")
                    return false
                }

                return true

            } catch (e: Exception) {
                Log.e(TAG, "EGL initialization failed", e)
                return false
            }
        }

        fun updateSize(newWidth: Int, newHeight: Int) {
            width = newWidth
            height = newHeight
            renderer.onSurfaceChanged(null, width, height)
        }

        fun updateShader(shaderName: String) {
            renderer.loadShader(shaderName)
        }

        fun updateOpacity(opacity: Float) {
            renderer.setOpacity(opacity)
        }

        fun updatePerformanceMode(mode: ShaderRenderer.PerformanceMode) {
            renderer.setPerformanceMode(mode)
        }

        fun stopRendering() {
            running = false
            interrupt()
        }

        private fun cleanup() {
            egl?.let { egl ->
                egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
                egl.eglDestroySurface(eglDisplay, eglSurface)
                egl.eglDestroyContext(eglDisplay, eglContext)
                egl.eglTerminate(eglDisplay)
            }
        }
    }
}