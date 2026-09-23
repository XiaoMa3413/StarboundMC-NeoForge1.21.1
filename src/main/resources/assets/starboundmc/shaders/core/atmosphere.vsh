#version 150

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

// Sun direction in the atmosphere mesh's local frame; the shell is a sphere,
// so the local frame is the body frame up to the model's uniform scale.
uniform vec3 SunDirection;
uniform vec3 AtmosphereCenter;

out vec3 shellDirection;
out vec3 shellNormal;
out vec3 sunDirectionView;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    shellDirection = normalize(viewPos.xyz - AtmosphereCenter);
    shellNormal = normalize(mat3(ModelViewMat) * normalize(Position));
    sunDirectionView = normalize(mat3(ModelViewMat) * SunDirection);
}
