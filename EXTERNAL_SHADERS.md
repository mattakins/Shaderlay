# Loading External Slang Shaders

The Shaderlay app now supports loading custom RetroArch slang shaders from your Android file system.

## **🔵 ACTION →** *How to Load External Shaders*

### **Method 1: Through Settings (Recommended)**

1. **Open Shaderlay** → Tap "Settings"
2. **Find "Shader Selection"** → Tap on it
3. **Choose "📂 Load External Shader..."** from the list
4. **Browse and select** your `.slang` or `.slangp` file
5. **Grant storage permissions** if prompted
6. **Shader loads automatically** and becomes available for selection

### **Method 2: File Manager Integration**

1. **Use any file manager** (Files, Solid Explorer, etc.)
2. **Navigate to your shader files**
3. **Long-press a `.slangp` file** → Share → Shaderlay
4. **App opens and loads the shader**

## **📍 REFERENCE →** *Supported File Types*

### **Preset Files (.slangp)**
```
shaders = 2
shader0 = first-pass.slang
shader1 = second-pass.slang
filter_linear0 = true
scale_type0 = source
scale0 = 2.0
```

### **Individual Shader Files (.slang)**
```glsl
#version 450
// Vertex stage
#pragma stage vertex
layout(location = 0) in vec4 Position;
layout(location = 1) in vec2 TexCoord;
layout(location = 0) out vec2 vTexCoord;

void main() {
    gl_Position = global.MVP * Position;
    vTexCoord = TexCoord;
}

// Fragment stage
#pragma stage fragment
layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out vec4 FragColor;
layout(set = 0, binding = 2) uniform sampler2D Source;

void main() {
    vec3 color = texture(Source, vTexCoord).rgb;
    // Your shader effects here
    FragColor = vec4(color, 1.0);
}
```

## **📍 REFERENCE →** *Where to Find Shaders*

### **RetroArch Shader Repository**
- **GitHub**: [libretro/slang-shaders](https://github.com/libretro/slang-shaders)
- **Download location**: `/storage/emulated/0/Download/`
- **Popular folders**: `crt/`, `handheld/`, `scanlines/`, `anti-aliasing/`

### **Recommended Download Locations**
```
/storage/emulated/0/
├── Download/shaders/           ← Download here
├── Documents/RetroArch/        ← Or here
└── Pictures/Shaders/           ← Or here
```

## **📝 NOTE →** *Shader Management Features*

### **Available in Settings:**
- **View loaded shaders** - See all external shaders
- **Remove shaders** - Delete from app (doesn't delete file)
- **Switch between shaders** - Toggle between built-in and external
- **Shader information** - View preset vs single file type

### **File Requirements:**
- **Must be valid slang format** - .slang or .slangp extension
- **Readable by app** - Proper storage permissions needed
- **Compatible syntax** - Modern slang shader syntax

## **⚠️ WARNING →** *Performance Considerations*

### **Complex shaders may impact performance:**
- **Monitor FPS** in overlay
- **Adjust performance mode** if needed (Settings → Performance)
- **Lower opacity** for better battery life
- **Use "Battery Saver" mode** for complex multi-pass shaders

### **Multi-pass shaders:**
- **Load preset files** (.slangp) for multi-pass effects
- **Automatic pass management** - App handles pass sequence
- **Resource intensive** - May require performance tuning

## **🔧 TROUBLESHOOTING**

### **Shader won't load:**
- **Check file extension** - Must be .slang or .slangp
- **Verify permissions** - Grant storage access in Android settings
- **Check file location** - Use standard Downloads or Documents folder
- **File corruption** - Re-download from source

### **Shader loads but has no effect:**
- **Check opacity setting** - Increase overlay opacity
- **Verify shader syntax** - Some slang features may not be supported
- **Try built-in shaders first** - Ensure overlay system is working

### **Performance issues:**
- **Lower FPS limit** - Settings → Performance → FPS Limit
- **Reduce opacity** - Less GPU load
- **Use simpler shaders** - Single-pass instead of multi-pass
- **Monitor temperature** - Avoid device overheating

## **📍 REFERENCE →** *Storage Permissions*

The app requests these permissions for external shader loading:

### **Android 13+:**
- `READ_MEDIA_IMAGES`
- `READ_MEDIA_VIDEO`
- `READ_MEDIA_AUDIO`

### **Android 6-12:**
- `READ_EXTERNAL_STORAGE`

**Permissions are granted automatically** when you first try to load an external shader.