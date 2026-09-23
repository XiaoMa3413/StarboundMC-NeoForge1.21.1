#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

// Sun direction in the sphere mesh's local frame.
uniform vec3 SunDirection;

out vec2 texCoord0;
out vec3 viewNormal;
out vec3 viewPosition;
out vec3 sunDirectionView;
out vec3 spherePosition;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    texCoord0 = UV0;
    // Everything the fragment stage evaluates is view space: the sphere mesh is
    // centred on the body with a uniform radius, so the normalized position is
    // the outward surface normal, and the sun rides the same matrix that
    // positions the vertices. Sharing the matrix keeps the terminator dot
    // product identical to the mesh-local one, so the day/night boundary stays
    // glued to the body instead of swinging with the camera.
    viewNormal = normalize(mat3(ModelViewMat) * normalize(Position));
    viewPosition = viewPos.xyz;
    sunDirectionView = normalize(mat3(ModelViewMat) * SunDirection);
    // The mesh-local position, for the procedural cloud density: sampling the
    // noise on the sphere's own frame keeps the pattern seamless at the poles
    // and free of texture-wrap seams, and rotates it with the mesh.
    spherePosition = Position;
}
