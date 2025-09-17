#version 450

layout(push_constant) uniform Push
{
    vec4 SourceSize;
    vec4 OriginalSize;
    vec4 OutputSize;
    uint FrameCount;
} params;

layout(std140, set = 0, binding = 0) uniform UBO
{
    mat4 MVP;
} global;

#pragma stage vertex
layout(location = 0) in vec4 Position;
layout(location = 1) in vec2 TexCoord;
layout(location = 0) out vec2 vTexCoord;

void main()
{
    gl_Position = global.MVP * Position;
    vTexCoord = TexCoord;
}

#pragma stage fragment
layout(location = 0) in vec2 vTexCoord;
layout(location = 0) out vec4 FragColor;

layout(set = 0, binding = 2) uniform sampler2D Source;

void main()
{
    vec2 uv = vTexCoord;

    // Sample the original color
    vec3 col = texture(Source, uv).rgb;

    // Horizontal scanlines
    float scanline = sin(uv.y * params.OutputSize.y * 3.14159 * 2.0) * 0.5 + 0.5;
    scanline = pow(scanline, 1.5);

    // Subtle vertical pattern
    float vertical = sin(uv.x * params.OutputSize.x * 3.14159 * 0.5) * 0.1 + 0.9;

    // Apply scanline effect
    col *= scanline * vertical;

    FragColor = vec4(col, 1.0);
}