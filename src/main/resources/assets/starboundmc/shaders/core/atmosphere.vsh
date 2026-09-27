#version 150

in vec3 Position;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 shellNormal;
out vec3 meshPosition;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    shellNormal = Normal;
    meshPosition = Position;
}
