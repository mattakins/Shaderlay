# 🚀 Shaderlay Implementation Plan: Slang Shader Support

## 🎯 Project Goal
Implement authentic RetroArch slang shader support in Android system overlays using OpenGL ES + slang → GLSL transpilation pipeline.

## 📋 7-Week Implementation Roadmap

### ✅ **Phase 1: Fix OpenGL Overlay Rendering** (Week 1)
**Current Status**: ✅ Simple View overlays work perfectly
**Goal**: Get shader effects visible in overlay windows

**Critical Tasks**:
1. **Fix GLSurfaceView transparency**
   - Replace `setZOrderOnTop(true)` with `setZOrderMediaOverlay(true)`
   - Use `setEGLConfigChooser(8, 8, 8, 8, 16, 0)` for proper alpha
   - Maintain app accessibility (proven to work with simple View)

2. **Test Current Shaders**
   - Verify "red_test" shader shows visual effect
   - Confirm overlay doesn't block app interaction

**Deliverable**: Working OpenGL shader overlays with app accessibility

---

### 🔧 **Phase 2: Slang Transpilation Pipeline** (Weeks 2-3)
**Goal**: Core slang → GLSL ES conversion using industry standard tools

**Architecture**:
```
Slang Source → glslang → SPIR-V → SPIRV-Cross → GLSL ES 3.0
```

**Key Components**:
1. **Native SlangCompiler** (`cpp/slang_compiler.cpp`)
   - Integrate glslang library for slang compilation
   - Add SPIRV-Cross for GLSL ES transpilation
   - Handle slang-specific features (push constants, descriptor sets)

2. **Enhanced ShaderManager** (`shader/SlangShaderManager.kt`)
   - .slang file loading and preprocessing
   - Parameter extraction and uniform mapping
   - Caching of compiled shaders

**Target Shaders**: lcd1x.slang, crt-geom-mini.slang
**Deliverable**: Working slang shader rendering for simple effects

---

### 🎬 **Phase 3: Multi-Pass Rendering** (Weeks 4-5)
**Goal**: Support complex shader pipelines like crt-guest-advanced-ntsc (18 passes)

**New Components**:
1. **MultiPassRenderer** (`renderer/MultiPassShaderRenderer.kt`)
   - Framebuffer chain management
   - Pass dependency resolution
   - Texture alias and feedback handling

2. **SlangPresetParser** (`shader/SlangPresetParser.kt`)
   - .slangp file parsing
   - Scale type and parameter handling
   - LUT texture loading

3. **Framebuffer Management**
   - Float precision framebuffers (GL_RGBA16F)
   - Memory optimization for mobile devices
   - Automatic cleanup and reuse

**Target Shaders**: crt-guest-advanced-ntsc.slangp
**Deliverable**: Full-featured multi-pass shader pipeline

---

### ⚡ **Phase 4: Adaptive Performance** (Week 6)
**Goal**: 60fps performance across Android device spectrum

**Performance Strategy**:
1. **Device Profiling**
   ```kotlin
   enum class GpuPerformanceClass { HIGH, MEDIUM, LOW }

   class DeviceProfiler {
       fun benchmarkGpuPerformance(): GpuPerformanceClass
       fun getOptimalShaderComplexity(): ShaderComplexity
   }
   ```

2. **Quality Scaling**
   - Flagship devices: Full crt-guest-advanced-ntsc (18 passes)
   - Mid-range devices: Simplified version (6-8 passes)
   - Budget devices: Single-pass approximations

3. **Smart Fallbacks**
   - Automatic shader complexity reduction
   - Resolution scaling for performance
   - Frame rate limiting and thermal management

**Deliverable**: Intelligent performance adaptation system

---

### 🎨 **Phase 5: Polish & Optimization** (Week 7)
**Goal**: Production-ready shader overlay system

**Optimization Features**:
1. **Aggressive Caching**
   - SPIR-V cache for faster loading
   - Device-specific compiled shader cache
   - Background compilation for smooth UX

2. **Error Handling**
   - Graceful shader compilation failures
   - Automatic fallback chains
   - User-friendly error messages

3. **Performance Monitoring**
   - Real-time FPS tracking
   - GPU memory usage monitoring
   - Shader complexity analytics

**Deliverable**: Production-quality overlay shader system

---

## 🏗️ Technical Architecture

### Core Pipeline
```
User Selects Shader → SlangPresetParser → Multi-Pass Setup →
DeviceProfiler → Quality Adaptation → SlangCompiler →
SPIR-V → SPIRV-Cross → GLSL ES → OpenGL Rendering
```

### Key Libraries
- **glslang**: Slang → SPIR-V compilation
- **SPIRV-Cross**: SPIR-V → GLSL ES transpilation
- **OpenGL ES 3.0**: GPU rendering
- **TYPE_APPLICATION_OVERLAY**: System overlay integration

### Performance Targets
- **Simple shaders** (lcd1x): 60fps on 95% of devices
- **Medium shaders** (crt-geom-mini): 60fps on 80% of devices
- **Complex shaders** (guest-advanced): 60fps on flagship devices, adaptive quality on others

---

## 📊 Expected Outcomes

### Shader Compatibility
- ✅ **100% compatibility** with simple RetroArch slang shaders
- ✅ **95% compatibility** with medium complexity shaders
- ✅ **70% compatibility** with complex multi-pass shaders (with adaptive quality)

### Device Support
- ✅ **Universal compatibility** (100% Android devices via OpenGL ES)
- ✅ **60fps performance** on 80% of target devices
- ✅ **Graceful degradation** on older hardware

### Feature Parity
- ✅ **Authentic RetroArch shader rendering** via slang transpilation
- ✅ **Parameter adjustment** support
- ✅ **Multi-pass pipeline** for complex effects
- ✅ **LUT texture** support
- ✅ **Overlay transparency** with app accessibility

---

## 🚦 Success Metrics

### Phase 1 Success
- [ ] GLSurfaceView shows shader effects in overlay
- [ ] App remains accessible when overlay is active
- [ ] 60fps with simple test shaders

### Phase 2 Success
- [ ] lcd1x.slang renders identically to RetroArch
- [ ] crt-geom-mini.slang works with all parameters
- [ ] Slang compilation pipeline functional

### Phase 3 Success
- [ ] crt-guest-advanced-ntsc.slangp loads and renders
- [ ] All 18 passes execute correctly
- [ ] LUT textures and complex parameters work

### Phase 4 Success
- [ ] 60fps maintained on 80% of test devices
- [ ] Automatic quality scaling functional
- [ ] Thermal and battery management effective

### Phase 5 Success
- [ ] Sub-100ms shader loading (cached)
- [ ] Robust error handling and fallbacks
- [ ] Production-ready performance monitoring

This plan provides a clear path to authentic RetroArch slang shader support while maintaining universal Android compatibility and 60fps performance.