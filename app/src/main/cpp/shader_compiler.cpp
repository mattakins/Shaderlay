#include "shader_compiler.h"
#include <android/log.h>
#include <string>
#include <vector>
#include <fstream>
#include <sstream>

#define LOG_TAG "ShaderCompiler"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace Shaderlay {

ShaderCompiler::ShaderCompiler() {
    LOGI("ShaderCompiler initialized");
}

ShaderCompiler::~ShaderCompiler() {
    cleanup();
}

bool ShaderCompiler::initialize() {
    // For now, we'll use a simplified approach without glslang
    // In a full implementation, this would initialize glslang
    LOGI("Shader compiler initialization (simplified)");
    return true;
}

void ShaderCompiler::cleanup() {
    // Cleanup resources
    LOGI("Shader compiler cleanup");
}

std::string ShaderCompiler::compileGLSL(const std::string& source, ShaderType type) {
    LOGI("Compiling GLSL shader, type: %d", static_cast<int>(type));

    // For now, return the source as-is since we're using GLSL directly
    // In a full implementation, this would compile to SPIR-V and back to GLSL
    return preprocessGLSL(source, type);
}

std::vector<uint32_t> ShaderCompiler::compileToSPIRV(const std::string& source, ShaderType type) {
    LOGI("Compiling to SPIR-V, type: %d", static_cast<int>(type));

    // Placeholder implementation
    // In a real implementation, this would use glslang to compile to SPIR-V
    std::vector<uint32_t> spirv;

    // Return empty vector for now
    LOGI("SPIR-V compilation not yet implemented");
    return spirv;
}

std::string ShaderCompiler::preprocessGLSL(const std::string& source, ShaderType type) {
    std::stringstream processed;
    std::istringstream sourceStream(source);
    std::string line;

    // Add version header if not present
    bool hasVersion = false;
    std::vector<std::string> lines;

    while (std::getline(sourceStream, line)) {
        if (line.find("#version") != std::string::npos) {
            hasVersion = true;
        }
        lines.push_back(line);
    }

    if (!hasVersion) {
        processed << "#version 100\n";
        if (type == ShaderType::Fragment) {
            processed << "precision mediump float;\n";
        }
    }

    // Process each line
    for (const auto& sourceLine : lines) {
        std::string processedLine = sourceLine;

        // Handle common slang-to-GLSL conversions
        processedLine = replaceSlangKeywords(processedLine);

        processed << processedLine << "\n";
    }

    return processed.str();
}

std::string ShaderCompiler::replaceSlangKeywords(const std::string& line) {
    std::string result = line;

    // Replace common slang keywords with GLSL equivalents
    // This is a simplified version - a full implementation would be more comprehensive

    // Replace float2, float3, float4 with vec2, vec3, vec4
    size_t pos = 0;
    while ((pos = result.find("float2", pos)) != std::string::npos) {
        result.replace(pos, 6, "vec2");
        pos += 4;
    }

    pos = 0;
    while ((pos = result.find("float3", pos)) != std::string::npos) {
        result.replace(pos, 6, "vec3");
        pos += 4;
    }

    pos = 0;
    while ((pos = result.find("float4", pos)) != std::string::npos) {
        result.replace(pos, 6, "vec4");
        pos += 4;
    }

    // Replace lerp with mix
    pos = 0;
    while ((pos = result.find("lerp(", pos)) != std::string::npos) {
        result.replace(pos, 4, "mix");
        pos += 3;
    }

    // Replace saturate with clamp(..., 0.0, 1.0)
    pos = 0;
    while ((pos = result.find("saturate(", pos)) != std::string::npos) {
        size_t endPos = findMatchingParen(result, pos + 9);
        if (endPos != std::string::npos) {
            std::string expr = result.substr(pos + 9, endPos - pos - 9);
            result.replace(pos, endPos - pos + 1, "clamp(" + expr + ", 0.0, 1.0)");
        }
        pos++;
    }

    return result;
}

size_t ShaderCompiler::findMatchingParen(const std::string& str, size_t start) {
    int count = 1;
    for (size_t i = start; i < str.length(); ++i) {
        if (str[i] == '(') count++;
        else if (str[i] == ')') count--;

        if (count == 0) return i;
    }
    return std::string::npos;
}

bool ShaderCompiler::validateShader(const std::string& source, ShaderType type) {
    // Basic validation - check for common issues
    if (source.empty()) {
        LOGE("Shader source is empty");
        return false;
    }

    // Check for main function
    if (source.find("void main") == std::string::npos) {
        LOGE("Shader missing main function");
        return false;
    }

    // Type-specific validation
    if (type == ShaderType::Fragment) {
        if (source.find("gl_FragColor") == std::string::npos) {
            LOGE("Fragment shader missing gl_FragColor assignment");
            return false;
        }
    }

    return true;
}

} // namespace Shaderlay