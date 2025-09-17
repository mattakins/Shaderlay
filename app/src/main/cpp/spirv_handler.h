#pragma once

#include "shader_compiler.h"
#include <vector>
#include <string>
#include <cstdint>

namespace Shaderlay {

class SPIRVHandler {
public:
    SPIRVHandler();
    ~SPIRVHandler();

    bool initialize();
    void cleanup();

    // Convert SPIR-V bytecode to GLSL
    std::string convertSPIRVToGLSL(const std::vector<uint32_t>& spirv, ShaderType type);

    // Optimize SPIR-V bytecode
    std::vector<uint32_t> optimizeSPIRV(const std::vector<uint32_t>& spirv);

    // Validate SPIR-V bytecode
    bool validateSPIRV(const std::vector<uint32_t>& spirv);

private:
    std::string generateBasicGLSL(ShaderType type);

    bool initialized_ = false;
};

} // namespace Shaderlay