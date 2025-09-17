#include "slang_parser.h"
#include <android/log.h>
#include <sstream>
#include <regex>

#define LOG_TAG "SlangParser"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace Shaderlay {

SlangParser::SlangParser() {
    LOGI("SlangParser initialized");
}

SlangParser::~SlangParser() = default;

bool SlangParser::parseSlangPreset(const std::string& presetContent) {
    LOGI("Parsing slang preset");

    preset_ = SlangPreset{};
    std::istringstream stream(presetContent);
    std::string line;

    while (std::getline(stream, line)) {
        line = trim(line);

        // Skip empty lines and comments
        if (line.empty() || line[0] == '#') {
            continue;
        }

        parseLine(line);
    }

    LOGI("Parsed preset with %zu shaders", preset_.shaders.size());
    return !preset_.shaders.empty();
}

SlangPreset SlangParser::getPreset() const {
    return preset_;
}

void SlangParser::parseLine(const std::string& line) {
    size_t equalPos = line.find('=');
    if (equalPos == std::string::npos) {
        return;
    }

    std::string key = trim(line.substr(0, equalPos));
    std::string value = trim(line.substr(equalPos + 1));

    // Remove quotes from value
    if (value.front() == '"' && value.back() == '"') {
        value = value.substr(1, value.length() - 2);
    }

    if (key == "shaders") {
        preset_.shaderCount = std::stoi(value);
    } else if (key.find("shader") == 0) {
        parseShaderLine(key, value);
    } else if (key.find("filter_linear") == 0) {
        parseFilterLine(key, value);
    } else if (key.find("scale_type") == 0) {
        parseScaleLine(key, value);
    } else if (key.find("scale") == 0) {
        parseScaleValueLine(key, value);
    }
}

void SlangParser::parseShaderLine(const std::string& key, const std::string& value) {
    // Extract shader index from key like "shader0", "shader1", etc.
    std::regex indexRegex(R"(shader(\d+))");
    std::smatch match;

    if (std::regex_search(key, match, indexRegex)) {
        int index = std::stoi(match[1]);

        // Ensure we have enough shader entries
        while (static_cast<int>(preset_.shaders.size()) <= index) {
            preset_.shaders.emplace_back();
        }

        preset_.shaders[index].path = value;
        LOGI("Shader %d: %s", index, value.c_str());
    }
}

void SlangParser::parseFilterLine(const std::string& key, const std::string& value) {
    std::regex indexRegex(R"(filter_linear(\d+))");
    std::smatch match;

    if (std::regex_search(key, match, indexRegex)) {
        int index = std::stoi(match[1]);

        while (static_cast<int>(preset_.shaders.size()) <= index) {
            preset_.shaders.emplace_back();
        }

        preset_.shaders[index].filterLinear = (value == "true");
    }
}

void SlangParser::parseScaleLine(const std::string& key, const std::string& value) {
    std::regex indexRegex(R"(scale_type(\d+))");
    std::smatch match;

    if (std::regex_search(key, match, indexRegex)) {
        int index = std::stoi(match[1]);

        while (static_cast<int>(preset_.shaders.size()) <= index) {
            preset_.shaders.emplace_back();
        }

        if (value == "source") {
            preset_.shaders[index].scaleType = ScaleType::Source;
        } else if (value == "viewport") {
            preset_.shaders[index].scaleType = ScaleType::Viewport;
        } else if (value == "absolute") {
            preset_.shaders[index].scaleType = ScaleType::Absolute;
        }
    }
}

void SlangParser::parseScaleValueLine(const std::string& key, const std::string& value) {
    std::regex indexRegex(R"(scale(\d+))");
    std::smatch match;

    if (std::regex_search(key, match, indexRegex)) {
        int index = std::stoi(match[1]);

        while (static_cast<int>(preset_.shaders.size()) <= index) {
            preset_.shaders.emplace_back();
        }

        preset_.shaders[index].scale = std::stof(value);
    }
}

std::string SlangParser::loadShaderSource(const std::string& shaderPath) {
    LOGI("Loading shader source: %s", shaderPath.c_str());

    // In a real implementation, this would load from assets or filesystem
    // For now, return a simple placeholder
    return generatePlaceholderShader(shaderPath);
}

std::string SlangParser::generatePlaceholderShader(const std::string& shaderPath) {
    // Extract shader name from path
    size_t lastSlash = shaderPath.find_last_of('/');
    std::string shaderName = (lastSlash != std::string::npos)
        ? shaderPath.substr(lastSlash + 1)
        : shaderPath;

    // Remove file extension
    size_t lastDot = shaderName.find_last_of('.');
    if (lastDot != std::string::npos) {
        shaderName = shaderName.substr(0, lastDot);
    }

    // Generate a simple shader based on the name
    if (shaderName.find("crt") != std::string::npos) {
        return generateCRTShader();
    } else if (shaderName.find("scanline") != std::string::npos) {
        return generateScanlineShader();
    } else if (shaderName.find("lcd") != std::string::npos) {
        return generateLCDShader();
    } else {
        return generatePassthroughShader();
    }
}

std::string SlangParser::generateCRTShader() {
    return R"(
#version 100
precision mediump float;

uniform float u_Time;
uniform vec2 u_Resolution;
uniform float u_Opacity;

varying vec2 v_TexCoord;

void main() {
    vec2 uv = v_TexCoord;
    vec2 dc = abs(0.5 - uv);
    dc *= dc;

    // CRT curvature
    uv.x -= 0.5; uv.x *= 1.0 + (dc.y * 0.15);
    uv.y -= 0.5; uv.y *= 1.0 + (dc.x * 0.2);
    uv += 0.5;

    // Vignette
    float vig = 1.0 - dot(dc, dc);
    vig = pow(vig, 0.5);

    // Scanlines
    float scanline = sin(uv.y * u_Resolution.y * 3.14159) * 0.04;

    vec3 col = vec3(0.2, 0.8, 0.3);
    col += scanline;
    col *= vig;

    gl_FragColor = vec4(col, u_Opacity * 0.3);
}
)";
}

std::string SlangParser::generateScanlineShader() {
    return R"(
#version 100
precision mediump float;

uniform float u_Time;
uniform vec2 u_Resolution;
uniform float u_Opacity;

varying vec2 v_TexCoord;

void main() {
    vec2 uv = v_TexCoord;

    float scanline = sin(uv.y * u_Resolution.y * 3.14159 * 2.0) * 0.5 + 0.5;
    scanline = pow(scanline, 2.0);

    vec3 color = vec3(0.0);
    float alpha = scanline * u_Opacity * 0.4;

    gl_FragColor = vec4(color, alpha);
}
)";
}

std::string SlangParser::generateLCDShader() {
    return R"(
#version 100
precision mediump float;

uniform float u_Time;
uniform vec2 u_Resolution;
uniform float u_Opacity;

varying vec2 v_TexCoord;

void main() {
    vec2 uv = v_TexCoord;

    vec2 grid = abs(fract(uv * u_Resolution / 3.0) - 0.5);
    float line = min(grid.x, grid.y) * 2.0;

    vec3 subpixel = vec3(1.0);
    float mod_x = mod(uv.x * u_Resolution.x, 3.0);
    if (mod_x < 1.0) subpixel = vec3(1.0, 0.3, 0.3);
    else if (mod_x < 2.0) subpixel = vec3(0.3, 1.0, 0.3);
    else subpixel = vec3(0.3, 0.3, 1.0);

    vec3 color = mix(vec3(0.0), subpixel * 0.2, 1.0 - min(line, 1.0));

    gl_FragColor = vec4(color, u_Opacity * 0.2);
}
)";
}

std::string SlangParser::generatePassthroughShader() {
    return R"(
#version 100
precision mediump float;

uniform float u_Time;
uniform vec2 u_Resolution;
uniform float u_Opacity;

varying vec2 v_TexCoord;

void main() {
    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
}
)";
}

std::string SlangParser::trim(const std::string& str) {
    size_t start = str.find_first_not_of(" \t\r\n");
    if (start == std::string::npos) {
        return "";
    }

    size_t end = str.find_last_not_of(" \t\r\n");
    return str.substr(start, end - start + 1);
}

} // namespace Shaderlay