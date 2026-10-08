#version 150
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec2 texCoord;
out vec4 vertexColor;
out vec3 viewPosition;
out vec3 meshPosition;
void main() {
    viewPosition = (ModelViewMat * vec4(Position, 1.0)).xyz;
    gl_Position = ProjMat * vec4(viewPosition, 1.0);
    gl_Position.z = 0.0;
    texCoord = UV0;
    vertexColor = Color;
    meshPosition = Position;
}
