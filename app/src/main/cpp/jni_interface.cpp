#include <jni.h>
#include <android/log.h>
#include <string>
#include <memory>

#include "shader_compiler.h"
#include "slang_parser.h"
#include "spirv_handler.h"

#define LOG_TAG "JNIInterface"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

using namespace Shaderlay;

static std::unique_ptr<ShaderCompiler> g_shaderCompiler;
static std::unique_ptr<SlangParser> g_slangParser;
static std::unique_ptr<SPIRVHandler> g_spirvHandler;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_shaderlay_app_shader_NativeShaderCompiler_initialize(JNIEnv *env, jobject thiz) {
    LOGI("Initializing native shader compiler");

    try {
        g_shaderCompiler = std::make_unique<ShaderCompiler>();
        g_slangParser = std::make_unique<SlangParser>();
        g_spirvHandler = std::make_unique<SPIRVHandler>();

        bool success = g_shaderCompiler->initialize() &&
                      g_spirvHandler->initialize();

        LOGI("Native shader compiler initialization: %s", success ? "SUCCESS" : "FAILED");
        return success ? JNI_TRUE : JNI_FALSE;

    } catch (const std::exception& e) {
        LOGE("Exception during initialization: %s", e.what());
        return JNI_FALSE;
    }
}

JNIEXPORT void JNICALL
Java_com_shaderlay_app_shader_NativeShaderCompiler_cleanup(JNIEnv *env, jobject thiz) {
    LOGI("Cleaning up native shader compiler");

    if (g_shaderCompiler) {
        g_shaderCompiler->cleanup();
        g_shaderCompiler.reset();
    }

    if (g_spirvHandler) {
        g_spirvHandler->cleanup();
        g_spirvHandler.reset();
    }

    g_slangParser.reset();
}

JNIEXPORT jstring JNICALL
Java_com_shaderlay_app_shader_NativeShaderCompiler_compileShader(
        JNIEnv *env, jobject thiz, jstring source, jint type) {

    if (!g_shaderCompiler) {
        LOGE("Shader compiler not initialized");
        return nullptr;
    }

    const char* sourceStr = env->GetStringUTFChars(source, nullptr);
    if (!sourceStr) {
        LOGE("Failed to get source string");
        return nullptr;
    }

    try {
        std::string sourceCode(sourceStr);
        ShaderType shaderType = static_cast<ShaderType>(type);

        // Validate shader first
        if (!g_shaderCompiler->validateShader(sourceCode, shaderType)) {
            LOGE("Shader validation failed");
            env->ReleaseStringUTFChars(source, sourceStr);
            return nullptr;
        }

        // Compile shader
        std::string compiledShader = g_shaderCompiler->compileGLSL(sourceCode, shaderType);

        env->ReleaseStringUTFChars(source, sourceStr);

        if (compiledShader.empty()) {
            LOGE("Shader compilation failed");
            return nullptr;
        }

        return env->NewStringUTF(compiledShader.c_str());

    } catch (const std::exception& e) {
        LOGE("Exception during shader compilation: %s", e.what());
        env->ReleaseStringUTFChars(source, sourceStr);
        return nullptr;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_shaderlay_app_shader_NativeShaderCompiler_parseSlangPreset(
        JNIEnv *env, jobject thiz, jstring preset_content) {

    if (!g_slangParser) {
        LOGE("Slang parser not initialized");
        return JNI_FALSE;
    }

    const char* presetStr = env->GetStringUTFChars(preset_content, nullptr);
    if (!presetStr) {
        LOGE("Failed to get preset string");
        return JNI_FALSE;
    }

    try {
        std::string presetContent(presetStr);
        bool success = g_slangParser->parseSlangPreset(presetContent);

        env->ReleaseStringUTFChars(preset_content, presetStr);

        LOGI("Slang preset parsing: %s", success ? "SUCCESS" : "FAILED");
        return success ? JNI_TRUE : JNI_FALSE;

    } catch (const std::exception& e) {
        LOGE("Exception during slang preset parsing: %s", e.what());
        env->ReleaseStringUTFChars(preset_content, presetStr);
        return JNI_FALSE;
    }
}

JNIEXPORT jstring JNICALL
Java_com_shaderlay_app_shader_NativeShaderCompiler_getShaderSource(
        JNIEnv *env, jobject thiz, jstring shader_path) {

    if (!g_slangParser) {
        LOGE("Slang parser not initialized");
        return nullptr;
    }

    const char* pathStr = env->GetStringUTFChars(shader_path, nullptr);
    if (!pathStr) {
        LOGE("Failed to get shader path string");
        return nullptr;
    }

    try {
        std::string shaderPath(pathStr);
        std::string shaderSource = g_slangParser->loadShaderSource(shaderPath);

        env->ReleaseStringUTFChars(shader_path, pathStr);

        if (shaderSource.empty()) {
            LOGE("Failed to load shader source");
            return nullptr;
        }

        return env->NewStringUTF(shaderSource.c_str());

    } catch (const std::exception& e) {
        LOGE("Exception during shader source loading: %s", e.what());
        env->ReleaseStringUTFChars(shader_path, pathStr);
        return nullptr;
    }
}

JNIEXPORT jboolean JNICALL
Java_com_shaderlay_app_shader_NativeShaderCompiler_validateShader(
        JNIEnv *env, jobject thiz, jstring source, jint type) {

    if (!g_shaderCompiler) {
        LOGE("Shader compiler not initialized");
        return JNI_FALSE;
    }

    const char* sourceStr = env->GetStringUTFChars(source, nullptr);
    if (!sourceStr) {
        LOGE("Failed to get source string");
        return JNI_FALSE;
    }

    try {
        std::string sourceCode(sourceStr);
        ShaderType shaderType = static_cast<ShaderType>(type);

        bool isValid = g_shaderCompiler->validateShader(sourceCode, shaderType);

        env->ReleaseStringUTFChars(source, sourceStr);

        return isValid ? JNI_TRUE : JNI_FALSE;

    } catch (const std::exception& e) {
        LOGE("Exception during shader validation: %s", e.what());
        env->ReleaseStringUTFChars(source, sourceStr);
        return JNI_FALSE;
    }
}

} // extern "C"