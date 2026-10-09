#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D HeatSampler;
uniform vec2 OutSize;
// Seconds, wrapping every 1000
uniform float ThermalTime;
// 0 white-hot, 1 green-hot, 2 red-hot
uniform float Palette;
// 0 clean (the original Ultimate FLIR), 1 rough, 2 middle, 3 clearest (Labjac Edition generations)
uniform float Generation;

in vec2 texCoord;
out vec4 fragColor;

float noise(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

// Background level in the white palette maps onto the other palettes like the Labjac shaders did
vec3 background(float level, int palette) {
    if (palette == 1)
        return vec3(0.0, level * (0.42 / 0.36), 0.0);
    if (palette == 2)
        return vec3(level, 0.0, 0.0);
    return vec3(level);
}

vec3 hot(float level, int palette) {
    if (palette == 1)
        return vec3(0.25, level, 0.25);
    if (palette == 2)
        return vec3(level, 0.05, 0.02);
    return vec3(level);
}

void main() {
    int palette = int(Palette + 0.5);
    int generation = int(Generation + 0.5);
    vec2 texel = 1.0 / max(OutSize, vec2(1.0));
    float t = ThermalTime;

    if (generation <= 0) {
        vec3 scene = texture(DiffuseSampler, texCoord).rgb;
        float level = 0.08 + dot(scene, vec3(0.2126, 0.7152, 0.0722)) * 0.35;
        float heat = texture(HeatSampler, texCoord).a;
        fragColor = vec4(mix(background(level, palette), hot(1.0, palette), heat), 1.0);
        return;
    }

    vec2 sampleCoord;
    vec3 scene;
    float level;
    float hotLevel = 1.0;
    if (generation == 1) {
        // Rough: jittering rows, blocky grain, heavy scanlines and the odd dropout
        float rowNoise = noise(vec2(floor(texCoord.y * 160.0), floor(t * 3.0)));
        float blockNoise = noise(floor(texCoord * vec2(160.0, 90.0)) + floor(t * 4.0));
        float wave = sin(texCoord.y * 44.0 + texCoord.x * 8.0 + t * 2.0) * 0.002;
        float smear = wave + (rowNoise - 0.5) * 0.0025 + (blockNoise - 0.5) * 0.0014;
        sampleCoord = vec2(clamp(texCoord.x + smear, 0.0, 1.0), texCoord.y);
        scene = texture(DiffuseSampler, sampleCoord).rgb;
        float grain = (blockNoise - 0.5) * 0.03;
        float scanline = sin(texCoord.y * 680.0 + t * 6.0) * 0.012;
        float dropout = step(0.996, rowNoise) * -0.016;
        level = dot(scene, vec3(0.299, 0.587, 0.114)) * 0.34 + grain + scanline + dropout;
        hotLevel = palette == 0 ? 0.86 + (blockNoise - 0.5) * 0.04 : 0.98 + (blockNoise - 0.5) * 0.025;
    }
    else {
        // Middle and clearest: a slight horizontal blur, fine grain and faint scanlines
        bool clearest = generation >= 3;
        float wave = clearest ? sin(texCoord.y * 52.0 + t * 1.5) * 0.00075 : sin(texCoord.y * 52.0 + t * 1.7) * 0.0009;
        float spread = clearest ? 1.0 : 1.2;
        float side = clearest ? 0.09 : 0.11;
        sampleCoord = vec2(clamp(texCoord.x + wave, 0.0, 1.0), texCoord.y);
        scene = texture(DiffuseSampler, sampleCoord).rgb * (1.0 - 2.0 * side);
        scene += texture(DiffuseSampler, sampleCoord + vec2(texel.x * spread, 0.0)).rgb * side;
        scene += texture(DiffuseSampler, sampleCoord - vec2(texel.x * spread, 0.0)).rgb * side;
        float grain = clearest
            ? (noise(floor(texCoord * vec2(420.0, 240.0)) + floor(t * 3.0)) - 0.5) * 0.009
            : (noise(floor(texCoord * vec2(360.0, 210.0)) + floor(t * 4.0)) - 0.5) * 0.014;
        float scanline = clearest ? sin(texCoord.y * 760.0 + t * 4.0) * 0.0035 : sin(texCoord.y * 760.0 + t * 5.0) * 0.005;
        level = dot(scene, vec3(0.299, 0.587, 0.114)) * 0.36 + grain + scanline;
    }

    float heat = texture(HeatSampler, sampleCoord).a;
    vec3 cold = background(clamp(level, 0.0, 1.0), palette);
    fragColor = vec4(clamp(mix(cold, hot(clamp(hotLevel, 0.0, 1.0), palette), heat), 0.0, 1.0), 1.0);
}
