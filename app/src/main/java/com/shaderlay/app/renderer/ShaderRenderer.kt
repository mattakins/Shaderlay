package com.shaderlay.app.renderer

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.shaderlay.app.shader.ShaderManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ShaderRenderer(private val context: Context) : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "ShaderRenderer"

        // Vertex coordinates for full-screen quad
        private val VERTEX_COORDS = floatArrayOf(
            -1.0f, -1.0f, 0.0f,  // Bottom left
             1.0f, -1.0f, 0.0f,  // Bottom right
            -1.0f,  1.0f, 0.0f,  // Top left
             1.0f,  1.0f, 0.0f   // Top right
        )

        // Texture coordinates
        private val TEXTURE_COORDS = floatArrayOf(
            0.0f, 1.0f,  // Bottom left
            1.0f, 1.0f,  // Bottom right
            0.0f, 0.0f,  // Top left
            1.0f, 0.0f   // Top right
        )

        private const val COORDS_PER_VERTEX = 3
        private const val COORDS_PER_TEXTURE = 2
        private const val VERTEX_STRIDE = COORDS_PER_VERTEX * 4 // 4 bytes per float
        private const val TEXTURE_STRIDE = COORDS_PER_TEXTURE * 4
    }

    enum class PerformanceMode {
        HIGH_QUALITY,
        BALANCED,
        BATTERY_SAVER
    }

    private var shaderManager: ShaderManager? = null
    private var vertexBuffer: FloatBuffer? = null
    private var textureBuffer: FloatBuffer? = null

    private var shaderProgram = 0
    private var vertexHandle = 0
    private var textureHandle = 0
    private var mvpMatrixHandle = 0
    private var opacityHandle = 0
    private var timeHandle = 0
    private var resolutionHandle = 0

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    private var currentOpacity = 1.0f
    private var currentShader = "none"
    private var performanceMode = PerformanceMode.BALANCED
    private var startTime = 0L
    private var frameCount = 0
    private var lastFpsTime = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated")

        // Enable blending for transparency
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Clear color with transparency
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        // Initialize buffers
        initializeBuffers()

        // Initialize shader manager
        shaderManager = ShaderManager(context)

        // Load default shader
        loadShader(currentShader)

        startTime = System.currentTimeMillis()
        lastFpsTime = startTime
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: ${width}x${height}")

        GLES20.glViewport(0, 0, width, height)

        // Set up projection matrix
        val ratio = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)

        // Set up view matrix
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1f, 0f)

        // Calculate MVP matrix
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Update resolution uniform
        if (resolutionHandle != 0) {
            GLES20.glUseProgram(shaderProgram)
            GLES20.glUniform2f(resolutionHandle, width.toFloat(), height.toFloat())
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear the screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        if (shaderProgram == 0) return

        // Use shader program
        GLES20.glUseProgram(shaderProgram)

        // Update uniforms
        updateUniforms()

        // Bind vertex position
        GLES20.glEnableVertexAttribArray(vertexHandle)
        GLES20.glVertexAttribPointer(
            vertexHandle, COORDS_PER_VERTEX,
            GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer
        )

        // Bind texture coordinates
        GLES20.glEnableVertexAttribArray(textureHandle)
        GLES20.glVertexAttribPointer(
            textureHandle, COORDS_PER_TEXTURE,
            GLES20.GL_FLOAT, false, TEXTURE_STRIDE, textureBuffer
        )

        // Draw the quad
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Disable vertex arrays
        GLES20.glDisableVertexAttribArray(vertexHandle)
        GLES20.glDisableVertexAttribArray(textureHandle)

        // Update frame counter
        updateFrameStats()
    }

    private fun initializeBuffers() {
        // Initialize vertex buffer
        val vbb = ByteBuffer.allocateDirect(VERTEX_COORDS.size * 4)
        vbb.order(ByteOrder.nativeOrder())
        vertexBuffer = vbb.asFloatBuffer()
        vertexBuffer?.put(VERTEX_COORDS)
        vertexBuffer?.position(0)

        // Initialize texture buffer
        val tbb = ByteBuffer.allocateDirect(TEXTURE_COORDS.size * 4)
        tbb.order(ByteOrder.nativeOrder())
        textureBuffer = tbb.asFloatBuffer()
        textureBuffer?.put(TEXTURE_COORDS)
        textureBuffer?.position(0)
    }

    private fun updateUniforms() {
        val currentTime = System.currentTimeMillis()
        val timeSeconds = (currentTime - startTime) / 1000.0f

        // Update MVP matrix
        if (mvpMatrixHandle != 0) {
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        }

        // Update opacity
        if (opacityHandle != 0) {
            GLES20.glUniform1f(opacityHandle, currentOpacity)
        }

        // Update time
        if (timeHandle != 0) {
            GLES20.glUniform1f(timeHandle, timeSeconds)
        }
    }

    private fun updateFrameStats() {
        frameCount++
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastFpsTime >= 1000) {
            val fps = frameCount * 1000.0f / (currentTime - lastFpsTime)
            Log.d(TAG, "FPS: %.1f".format(fps))

            frameCount = 0
            lastFpsTime = currentTime

            // Adjust performance based on FPS and mode
            adjustPerformanceIfNeeded(fps)
        }
    }

    private fun adjustPerformanceIfNeeded(fps: Float) {
        when (performanceMode) {
            PerformanceMode.BATTERY_SAVER -> {
                if (fps > 35) {
                    // Could reduce quality further
                }
            }
            PerformanceMode.BALANCED -> {
                if (fps < 25) {
                    Log.d(TAG, "Performance below threshold, consider optimizations")
                }
            }
            PerformanceMode.HIGH_QUALITY -> {
                // Maintain highest quality regardless of performance
            }
        }
    }

    fun loadShader(shaderName: String) {
        Log.d(TAG, "Loading shader: $shaderName")

        shaderManager?.let { manager ->
            val program = manager.createShaderProgram(shaderName)
            if (program != 0) {
                // Clean up old program
                if (shaderProgram != 0) {
                    GLES20.glDeleteProgram(shaderProgram)
                }

                shaderProgram = program
                currentShader = shaderName

                // Get shader attribute and uniform handles
                vertexHandle = GLES20.glGetAttribLocation(shaderProgram, "a_Position")
                textureHandle = GLES20.glGetAttribLocation(shaderProgram, "a_TexCoord")
                mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "u_MVPMatrix")
                opacityHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Opacity")
                timeHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Time")
                resolutionHandle = GLES20.glGetUniformLocation(shaderProgram, "u_Resolution")

                Log.d(TAG, "Shader loaded successfully: $shaderName")
            } else {
                Log.e(TAG, "Failed to load shader: $shaderName")
            }
        }
    }

    fun setOpacity(opacity: Float) {
        currentOpacity = opacity.coerceIn(0.0f, 1.0f)
        Log.d(TAG, "Opacity set to: $currentOpacity")
    }

    fun setPerformanceMode(mode: PerformanceMode) {
        performanceMode = mode
        Log.d(TAG, "Performance mode set to: $mode")
    }

    fun cleanup() {
        Log.d(TAG, "Cleaning up ShaderRenderer")

        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram)
            shaderProgram = 0
        }

        shaderManager?.cleanup()
        shaderManager = null
    }

    private fun checkGLError(operation: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "$operation: glError $error")
            throw RuntimeException("$operation: glError $error")
        }
    }
}
