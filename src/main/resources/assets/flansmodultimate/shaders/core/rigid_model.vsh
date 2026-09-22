#version 150

#moj_import <light.glsl>
#moj_import <fog.glsl>

in vec3 Position;
in vec2 UV0;
in ivec2 UV1;
in vec3 Normal;

uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 IViewRotMat;
uniform int FogShape;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;
// Twenty parts with mat3 normals stay within OpenGL 3.2 vertex-uniform limits.
uniform mat4 PartPose[20];
uniform mat3 PartNormal[20];
uniform mat4 PartData[20];

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec4 overlayColor;
out vec2 texCoord0;

void main() {
    int part = UV1.x;
    vec3 position = (PartPose[part] * vec4(Position, 1.0)).xyz;
    vec3 normal = PartNormal[part] * Normal;
    ivec4 lighting = ivec4(PartData[part][1]);
    gl_Position = ProjMat * ModelViewMat * vec4(position, 1.0);
    vertexDistance = fog_distance(ModelViewMat, IViewRotMat * position, FogShape);
    vertexColor = minecraft_mix_light(Light0_Direction, Light1_Direction, normal, PartData[part][0]);
    lightMapColor = texelFetch(Sampler2, lighting.xy / 16, 0);
    overlayColor = texelFetch(Sampler1, lighting.zw, 0);
    texCoord0 = UV0;
}
