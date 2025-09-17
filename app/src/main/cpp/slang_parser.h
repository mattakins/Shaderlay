#pragma once

#include <string>
#include <vector>

namespace Shaderlay {

enum class ScaleType {
    Source,
    Viewport,
    Absolute
};

struct SlangShader {
    std::string path;
    bool filterLinear = true;
    ScaleType scaleType = ScaleType::Source;
    float scale = 1.0f;
    int frameCountMod = 0;
    bool floatFramebuffer = false;
    bool srgbFramebuffer = false;
};

struct SlangPreset {
    int shaderCount = 0;
    std::vector<SlangShader> shaders;

    // Global parameters
    struct {
        std::string name;
        float defaultValue = 0.0f;
        float minimum = 0.0f;
        float maximum = 1.0f;
        float step = 0.01f;
    } parameters[32]; // Max 32 parameters

    int parameterCount = 0;
};

class SlangParser {
public:
    SlangParser();
    ~SlangParser();

    bool parseSlangPreset(const std::string& presetContent);
    SlangPreset getPreset() const;

    // Load shader source from path (placeholder implementation)
    std::string loadShaderSource(const std::string& shaderPath);

private:
    void parseLine(const std::string& line);
    void parseShaderLine(const std::string& key, const std::string& value);
    void parseFilterLine(const std::string& key, const std::string& value);
    void parseScaleLine(const std::string& key, const std::string& value);
    void parseScaleValueLine(const std::string& key, const std::string& value);

    std::string generatePlaceholderShader(const std::string& shaderPath);
    std::string generateCRTShader();
    std::string generateScanlineShader();
    std::string generateLCDShader();
    std::string generatePassthroughShader();

    std::string trim(const std::string& str);

    SlangPreset preset_;
};

} // namespace Shaderlay