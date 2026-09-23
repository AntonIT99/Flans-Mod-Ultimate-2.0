#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
out vec4 fragColor;

void main() {
    vec4 sampled = texture(Sampler0, texCoord0);
    if (sampled.a < 0.1) discard;
    vec4 lit = sampled * vertexColor * ColorModulator;
    lit.rgb = mix(overlayColor.rgb, lit.rgb, overlayColor.a);
    fragColor = linear_fog(lit * lightMapColor, vertexDistance, FogStart, FogEnd, FogColor);
}
