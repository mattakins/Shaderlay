package com.shaderlay.app.shader

class NativeShaderCompiler {

    companion object {
        init {
            System.loadLibrary("shaderlaynative")
        }

        const val SHADER_TYPE_VERTEX = 0
        const val SHADER_TYPE_FRAGMENT = 1
    }

    external fun initialize(): Boolean
    external fun cleanup()

    external fun compileShader(source: String, type: Int): String?
    external fun parseSlangPreset(presetContent: String): Boolean
    external fun getShaderSource(shaderPath: String): String?
    external fun validateShader(source: String, type: Int): Boolean
}
