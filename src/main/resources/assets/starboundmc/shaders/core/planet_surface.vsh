#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord0;
out vec3 surfaceNormal;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    texCoord0 = UV0;
    // The sphere mesh is centred on the body with a uniform radius, so the
    // normalized position is the outward surface normal.
    surfaceNormal = normalize(Position);
}
