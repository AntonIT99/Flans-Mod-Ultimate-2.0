#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D HeatSampler;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 scene = texture(DiffuseSampler, texCoord).rgb;
    float background = 0.08 + dot(scene, vec3(0.2126, 0.7152, 0.0722)) * 0.35;
    float heat = texture(HeatSampler, texCoord).a;
    fragColor = vec4(vec3(mix(background, 1.0, heat)), 1.0);
}
