#version 150
in vec3 Position;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec4 vertexColor;
out vec3 viewPosition;
void main() {
    viewPosition = (ModelViewMat * vec4(Position, 1.0)).xyz;
    gl_Position = ProjMat * vec4(viewPosition, 1.0);
    gl_Position.z = 0.0;
    vertexColor = Color;
}
