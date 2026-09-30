#version 150

in vec3 Position;
in vec2 UV0;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float DistanceScale;

out vec2 texCoord0;
out vec3 cloudNormal;
out vec3 meshPosition;
out vec3 viewPosition;

void main() {
    viewPosition = (ModelViewMat * vec4(Position, 1.0)).xyz;
    gl_Position = ProjMat * vec4(viewPosition, 1.0);
    if (DistanceScale > 0.0) gl_Position.z = 0.0;
    texCoord0 = UV0;
    cloudNormal = Normal;
    meshPosition = Position;
}
