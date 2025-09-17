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

    // LCD grid pattern
    vec2 grid = abs(fract(uv * params.OutputSize.xy / 3.0) - 0.5);
    float line = min(grid.x, grid.y) * 6.0;
    line = 1.0 - min(line, 1.0);

    // Subpixel pattern
    vec3 subpixel = vec3(1.0);
    float mod_x = mod(uv.x * params.OutputSize.x, 3.0);
    if (mod_x < 1.0)
        subpixel = vec3(1.0, 0.3, 0.3); // Red
    else if (mod_x < 2.0)
        subpixel = vec3(0.3, 1.0, 0.3); // Green
    else
        subpixel = vec3(0.3, 0.3, 1.0); // Blue

    // Combine original color with LCD effect
    col = mix(col, col * subpixel, 0.5);
    col = mix(col, vec3(0.0), line * 0.3);

    FragColor = vec4(col, 1.0);
}