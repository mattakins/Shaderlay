#pragma once

#include <string>
#include <vector>
#include <memory>

namespace Shaderlay {

enum class ShaderType {
    Vertex = 0,
    Fragment = 1
};

class ShaderCompiler {
public:
    ShaderCompiler();
    ~ShaderCompiler();

    bool initialize();
    void cleanup();

    // Compile GLSL source to optimized GLSL
    std::string compileGLSL(const std::string& source, ShaderType type);

    // Compile to SPIR-V (for future Vulkan support)
    std::vector<uint32_t> compileToSPIRV(const std::string& source, ShaderType type);

    // Validate shader source
    bool validateShader(const std::string& source, ShaderType type);

private:
    std::string preprocessGLSL(const std::string& source, ShaderType type);
    std::string replaceSlangKeywords(const std::string& line);
    size_t findMatchingParen(const std::string& str, size_t start);

    bool initialized_ = false;
};

} // namespace Shaderlay