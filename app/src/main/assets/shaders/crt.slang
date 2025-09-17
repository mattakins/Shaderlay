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
    vec2 dc = abs(0.5 - uv);
    dc *= dc;

    // CRT curvature effect
    uv.x -= 0.5;
    uv.x *= 1.0 + (dc.y * 0.3);
    uv.y -= 0.5;
    uv.y *= 1.0 + (dc.x * 0.4);
    uv += 0.5;

    // Vignette effect
    float vig = 1.0 - dot(dc, dc);
    vig = pow(vig, 0.8);

    // Sample the original color
    vec3 col = texture(Source, uv).rgb;

    // Scanlines
    float scanline = sin(uv.y * params.OutputSize.y * 3.14159) * 0.04;
    col += scanline;

    // Apply vignette
    col *= vig;

    // Add green phosphor tint
    col = mix(col, col * vec3(0.2, 1.0, 0.3), 0.3);

    FragColor = vec4(col, 1.0);
}