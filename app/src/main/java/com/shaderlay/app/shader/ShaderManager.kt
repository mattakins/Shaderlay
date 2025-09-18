package com.shaderlay.app.shader

import android.content.Context
import android.opengl.GLES20
import android.util.Log

class ShaderManager(private val context: Context) {

    companion object {
        private const val TAG = "ShaderManager"
    }

    private val shaderCache = mutableMapOf<String, Int>()
    private val compilationCache = ShaderCache(context)
    private val nativeCompiler = NativeShaderCompiler()
    private val externalShaderManager = ExternalShaderManager(context)

    // Store loaded external shaders
    private val externalShaders = mutableMapOf<String, ExternalShaderManager.ExternalShader>()

    fun createShaderProgram(shaderName: String): Int {
        // Check cache first
        shaderCache[shaderName]?.let { program ->
            if (GLES20.glIsProgram(program)) {
                return program
            } else {
                shaderCache.remove(shaderName)
            }
        }

        val vertexShader = loadVertexShader(shaderName)
        val fragmentShader = loadFragmentShader(shaderName)

        if (vertexShader == 0 || fragmentShader == 0) {
            return 0
        }

        val program = GLES20.glCreateProgram()
        if (program == 0) {
            Log.e(TAG, "Could not create program")
            return 0
        }

        GLES20.glAttachShader(program, vertexShader)
        checkGLError("glAttachShader vertex")

        GLES20.glAttachShader(program, fragmentShader)
        checkGLError("glAttachShader fragment")

        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            val error = GLES20.glGetProgramInfoLog(program)
            Log.e(TAG, "Could not link program: $error")
            GLES20.glDeleteProgram(program)
            return 0
        }

        // Clean up shaders (they're linked into the program now)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        // Cache the program
        shaderCache[shaderName] = program

        Log.d(TAG, "Successfully created shader program for: $shaderName")
        return program
    }

    private fun loadVertexShader(shaderName: String): Int {
        val vertexShaderCode = when (shaderName) {
            "none" -> getDefaultVertexShader()
            "crt" -> getCRTVertexShader()
            "scanlines" -> getScanlinesVertexShader()
            "lcd" -> getLCDVertexShader()
            else -> {
                Log.w(TAG, "Unknown shader: $shaderName, using default")
                getDefaultVertexShader()
            }
        }

        return loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
    }

    private fun loadFragmentShader(shaderName: String): Int {
        val originalShaderCode = when {
            shaderName == "none" -> getDefaultFragmentShader()
            shaderName == "crt" -> getCRTFragmentShader()
            shaderName == "scanlines" -> getScanlinesFragmentShader()
            shaderName == "lcd" -> getLCDFragmentShader()
            externalShaders.containsKey(shaderName) -> {
                loadExternalFragmentShader(shaderName) ?: getDefaultFragmentShader()
            }
            else -> {
                Log.w(TAG, "Unknown shader: $shaderName, using default")
                getDefaultFragmentShader()
            }
        }

        // Try to get compiled shader from cache
        val cachedShader = compilationCache.getCompiledShader(
            originalShaderCode,
            NativeShaderCompiler.SHADER_TYPE_FRAGMENT
        )

        val finalShaderCode = if (cachedShader != null) {
            Log.d(TAG, "Using cached fragment shader for: $shaderName")
            cachedShader
        } else {
            // Compile using native compiler if available
            val compiled = nativeCompiler.compileShader(
                originalShaderCode,
                NativeShaderCompiler.SHADER_TYPE_FRAGMENT
            )

            if (compiled != null) {
                Log.d(TAG, "Compiled fragment shader with native compiler: $shaderName")
                // Cache the compiled result
                compilationCache.putCompiledShader(
                    originalShaderCode,
                    NativeShaderCompiler.SHADER_TYPE_FRAGMENT,
                    compiled
                )
                compiled
            } else {
                Log.d(TAG, "Using original fragment shader: $shaderName")
                originalShaderCode
            }
        }

        return loadShader(GLES20.GL_FRAGMENT_SHADER, finalShaderCode)
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) {
            Log.e(TAG, "Could not create shader of type: $type")
            return 0
        }

        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)

        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] != GLES20.GL_TRUE) {
            val error = GLES20.glGetShaderInfoLog(shader)
            Log.e(TAG, "Could not compile shader of type $type: $error")
            Log.e(TAG, "Shader code: $shaderCode")
            GLES20.glDeleteShader(shader)
            return 0
        }

        return shader
    }

    private fun getDefaultVertexShader(): String {
        return """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;

            uniform mat4 u_MVPMatrix;

            varying vec2 v_TexCoord;

            void main() {
                gl_Position = u_MVPMatrix * a_Position;
                v_TexCoord = a_TexCoord;
            }
        """.trimIndent()
    }

    private fun getDefaultFragmentShader(): String {
        return """
            precision mediump float;

            uniform float u_Opacity;
            uniform float u_Time;
            uniform vec2 u_Resolution;

            varying vec2 v_TexCoord;

            void main() {
                // Simple pass-through with opacity
                vec3 color = vec3(0.0, 0.0, 0.0);
                gl_FragColor = vec4(color, 0.0);
            }
        """.trimIndent()
    }

    private fun getCRTVertexShader(): String {
        return getDefaultVertexShader()
    }

    private fun getCRTFragmentShader(): String {
        return """
            precision mediump float;

            uniform float u_Opacity;
            uniform float u_Time;
            uniform vec2 u_Resolution;

            varying vec2 v_TexCoord;

            void main() {
                vec2 uv = v_TexCoord;
                vec2 dc = abs(0.5 - uv);
                dc *= dc;

                // CRT curvature
                uv.x -= 0.5; uv.x *= 1.0 + (dc.y * (0.3 * 0.5));
                uv.y -= 0.5; uv.y *= 1.0 + (dc.x * (0.4 * 0.5));
                uv += 0.5;

                // Vignette
                float vig = (1.0 - dot(dc, dc));
                vig = pow(vig, 0.5);

                // Scanlines
                float scanline = sin(uv.y * u_Resolution.y * 3.14159) * 0.04;

                // Sample color (simulated screen content)
                vec3 col = vec3(0.2, 0.8, 0.3); // Green phosphor
                col += scanline;
                col *= vig;

                gl_FragColor = vec4(col, u_Opacity * 0.3);
            }
        """.trimIndent()
    }

    private fun getScanlinesVertexShader(): String {
        return getDefaultVertexShader()
    }

    private fun getScanlinesFragmentShader(): String {
        return """
            precision mediump float;

            uniform float u_Opacity;
            uniform float u_Time;
            uniform vec2 u_Resolution;

            varying vec2 v_TexCoord;

            void main() {
                vec2 uv = v_TexCoord;

                // Horizontal scanlines
                float scanline = sin(uv.y * u_Resolution.y * 3.14159 * 2.0) * 0.5 + 0.5;
                scanline = pow(scanline, 2.0);

                // Subtle vertical pattern
                float vertical = sin(uv.x * u_Resolution.x * 3.14159 * 0.5) * 0.1 + 0.9;

                vec3 color = vec3(0.0, 0.0, 0.0);
                float alpha = scanline * vertical * u_Opacity * 0.4;

                gl_FragColor = vec4(color, alpha);
            }
        """.trimIndent()
    }

    private fun getLCDVertexShader(): String {
        return getDefaultVertexShader()
    }

    private fun getLCDFragmentShader(): String {
        return """
            precision mediump float;

            uniform float u_Opacity;
            uniform float u_Time;
            uniform vec2 u_Resolution;

            varying vec2 v_TexCoord;

            void main() {
                vec2 uv = v_TexCoord;

                // LCD grid pattern
                vec2 grid = abs(fract(uv * u_Resolution / 3.0) - 0.5) / fwidth(uv * u_Resolution / 3.0);
                float line = min(grid.x, grid.y);

                // Subpixel pattern
                vec3 subpixel = vec3(1.0);
                float mod_x = mod(uv.x * u_Resolution.x, 3.0);
                if (mod_x < 1.0) subpixel = vec3(1.0, 0.3, 0.3); // Red
                else if (mod_x < 2.0) subpixel = vec3(0.3, 1.0, 0.3); // Green
                else subpixel = vec3(0.3, 0.3, 1.0); // Blue

                vec3 color = mix(vec3(0.0), subpixel * 0.2, 1.0 - min(line, 1.0));

                gl_FragColor = vec4(color, u_Opacity * 0.2);
            }
        """.trimIndent()
    }

    fun cleanup() {
        Log.d(TAG, "Cleaning up ShaderManager")

        shaderCache.values.forEach { program ->
            if (GLES20.glIsProgram(program)) {
                GLES20.glDeleteProgram(program)
            }
        }
        shaderCache.clear()
        nativeCompiler.cleanup()
    }

    fun clearShaderCache() {
        compilationCache.clearCache()
    }

    fun getCacheStats(): ShaderCache.CacheStats {
        return compilationCache.getCacheStats()
    }

    // External shader management
    fun loadExternalShader(uri: android.net.Uri): String? {
        val externalShader = externalShaderManager.loadShaderFromUri(uri)
        return if (externalShader != null) {
            externalShaders[externalShader.name] = externalShader
            Log.d(TAG, "Loaded external shader: ${externalShader.name}")
            externalShader.name
        } else {
            null
        }
    }

    private fun loadExternalFragmentShader(shaderName: String): String? {
        val externalShader = externalShaders[shaderName] ?: return null

        return try {
            if (externalShader.isPreset) {
                // Parse preset and load first shader
                val presetContent = externalShader.presetContent ?: return null
                val parsed = externalShaderManager.parseExternalPreset(presetContent, externalShader.uri)

                if (parsed != null && parsed.shaderPaths.isNotEmpty()) {
                    val firstShaderPath = parsed.shaderPaths[0] ?: return null
                    externalShaderManager.loadShaderContentFromPreset(externalShader.uri, firstShaderPath)
                        ?: generateSimpleFragmentShader()
                } else {
                    generateSimpleFragmentShader()
                }
            } else {
                // Direct shader file
                externalShaderManager.readFileContent(externalShader.uri)
                    ?: generateSimpleFragmentShader()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load external fragment shader: $shaderName", e)
            generateSimpleFragmentShader()
        }
    }

    private fun generateSimpleFragmentShader(): String {
        return """
            #version 100
            precision mediump float;

            uniform float u_Opacity;
            uniform float u_Time;
            uniform vec2 u_Resolution;

            varying vec2 v_TexCoord;

            void main() {
                vec2 uv = v_TexCoord;
                vec3 color = vec3(0.5 + 0.3 * sin(u_Time + uv.x * 10.0));
                gl_FragColor = vec4(color, u_Opacity * 0.5);
            }
        """.trimIndent()
    }

    fun getAvailableShaders(): List<String> {
        val builtin = listOf("none", "crt", "scanlines", "lcd")
        val external = externalShaders.keys.toList()
        return builtin + external
    }

    fun removeExternalShader(shaderName: String): Boolean {
        return externalShaders.remove(shaderName) != null
    }

    fun getExternalShaderInfo(shaderName: String): ExternalShaderManager.ExternalShader? {
        return externalShaders[shaderName]
    }

    private fun checkGLError(operation: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "$operation: glError $error")
        }
    }
}
