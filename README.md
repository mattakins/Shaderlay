# Shaderlay - Android Overlay App

[![Android CI/CD](https://github.com/mattakins/Shaderlay/workflows/Android%20CI/CD/badge.svg)](https://github.com/mattakins/Shaderlay/actions)
[![Release](https://img.shields.io/github/v/release/mattakins/Shaderlay)](https://github.com/mattakins/Shaderlay/releases)
[![API Level](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A system-wide shader overlay application for Android that applies RetroArch slang shaders on top of all content displayed on the screen.

![Shaderlay Demo](docs/demo.gif)

## Features

- **System-wide overlay**: Works over games, apps, and system UI using `TYPE_APPLICATION_OVERLAY`
- **RetroArch slang shader compatibility**: Supports parsing and rendering of slang shader format
- **Real-time shader processing**: Hardware-accelerated OpenGL ES rendering
- **Performance optimization**: Shader compilation caching and configurable performance modes
- **Transparency control**: Adjustable opacity for overlay effects
- **Battery efficient**: Power-aware rendering with frame rate limiting

## Architecture

### Core Components

1. **OverlayService**: Manages the system overlay window using WindowManager
2. **GLOverlaySurfaceView**: Transparent OpenGL surface for shader rendering
3. **ShaderRenderer**: OpenGL ES 2.0 renderer with shader pipeline
4. **ShaderManager**: Handles shader compilation and caching
5. **Native Compiler**: C++ shader compilation using simplified glslang approach
6. **SlangParser**: Parses RetroArch .slangp preset files

### Shader Pipeline

```
Slang Shader → Native Compiler → GLSL → OpenGL Program → GPU Rendering
```

### Performance Features

- **Shader Caching**: Compiled shaders cached to disk and memory
- **Dynamic Quality**: Performance-aware quality adjustments
- **Frame Rate Control**: Configurable FPS limits (30/60/120/unlimited)
- **GPU Profiling**: Real-time performance monitoring

## Permissions

- `SYSTEM_ALERT_WINDOW`: Required for overlay functionality
- `FOREGROUND_SERVICE`: For persistent overlay service
- `POST_NOTIFICATIONS`: Notification controls (Android 13+)

## Supported Shaders

### Built-in Effects
- **CRT**: Classic CRT monitor simulation with curvature and scanlines
- **Scanlines**: Horizontal scanline overlay effect
- **LCD Grid**: LCD subpixel pattern simulation
- **Passthrough**: No effect (transparency only)

### Custom Shaders
Place `.slangp` preset files in `assets/shaders/` directory.

## Build Requirements

- Android Studio Arctic Fox or later
- Android NDK for native compilation
- Minimum SDK: Android 8.0 (API 26)
- Target SDK: Android 14 (API 34)

## Configuration

### Performance Modes
- **High Quality**: Maximum visual fidelity
- **Balanced**: Optimized for performance and quality
- **Battery Saver**: Reduced quality for power efficiency

### Settings
- Shader selection
- Opacity control (0-100%)
- Performance mode
- FPS limiting
- Auto-start on boot

## Technical Implementation

### OpenGL ES Setup
```kotlin
setEGLContextClientVersion(2)
setEGLConfigChooser(8, 8, 8, 8, 16, 0)
holder.setFormat(PixelFormat.RGBA_8888)
setZOrderOnTop(true)
```

### Shader Compilation
```cpp
// Native compilation pipeline
GLSL Source → Preprocessing → Validation → GPU Program
```

### Overlay Window Configuration
```kotlin
WindowManager.LayoutParams().apply {
    type = TYPE_APPLICATION_OVERLAY
    format = PixelFormat.RGBA_8888
    flags = FLAG_NOT_FOCUSABLE or FLAG_NOT_TOUCHABLE or FLAG_HARDWARE_ACCELERATED
    width = MATCH_PARENT
    height = MATCH_PARENT
}
```

## Usage

1. **Grant Permissions**: Allow overlay permission in system settings
2. **Enable Overlay**: Toggle overlay switch in main activity
3. **Configure Effects**: Adjust settings for desired visual effects
4. **Performance Tuning**: Monitor FPS and adjust performance mode as needed

## Limitations

- Works on most apps and games
- Cannot overlay on some system-critical windows (lockscreen, secure dialogs)
- Performance impact varies based on shader complexity and device capabilities
- Some anti-cheat systems may detect overlays

## 🚀 Getting Started

### Download & Install
1. **Download APK** from [Releases](https://github.com/mattakins/Shaderlay/releases)
2. **Enable unknown sources** in Android settings
3. **Install the APK** and launch the app
4. **Grant overlay permission** when prompted
5. **Choose a shader** in settings and enable overlay

### Building from Source
```bash
git clone https://github.com/mattakins/Shaderlay.git
cd Shaderlay
./gradlew assembleDebug
```

## 🤝 Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📋 Roadmap

- [ ] Vulkan support for modern devices
- [ ] Real-time shader parameter adjustment
- [ ] Shader hot-loading from external sources
- [ ] Advanced multi-pass shader chains
- [ ] Custom shader editor interface
- [ ] Performance profiling tools

## 🐛 Known Issues

- Some anti-cheat systems may detect overlays
- Complex shaders may impact battery life on older devices
- Certain system dialogs cannot be overlaid (security limitation)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [RetroArch](https://www.retroarch.com/) for the slang shader format
- [libretro/slang-shaders](https://github.com/libretro/slang-shaders) for shader examples
- Android OpenGL ES documentation and community

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/mattakins/Shaderlay/issues)
- **Discussions**: [GitHub Discussions](https://github.com/mattakins/Shaderlay/discussions)

---

**⭐ Star this repository if you find it useful!**