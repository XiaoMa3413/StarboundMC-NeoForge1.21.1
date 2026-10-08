#version 150
in vec3 Position;
uniform mat4 ProjMat;
uniform vec3 CenterView;
uniform float QuadRadius;
out vec3 viewPosition;
void main() {
    vec3 axis = normalize(CenterView);
    vec3 reference = abs(axis.y) > 0.99 ? vec3(1,0,0) : vec3(0,1,0);
    vec3 right = normalize(cross(reference, axis));
    vec3 up = cross(axis, right);
    viewPosition = CenterView + (right * Position.x + up * Position.y) * QuadRadius;
    gl_Position = ProjMat * vec4(viewPosition, 1.0);
    gl_Position.z = 0.0;
}
