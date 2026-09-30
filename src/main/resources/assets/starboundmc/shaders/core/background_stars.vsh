#version 150
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ConvergenceAxis;
uniform float Convergence;
uniform float StarAlpha;
uniform float TimePhase;
uniform vec3 TintColor;
uniform float TintAmount;
out vec2 starUv;
out vec4 starColor;
void main() {
    vec3 axis = normalize(ConvergenceAxis);
    float axial = dot(Position, axis);
    float front = smoothstep(-0.05, 1.0, axial);
    vec3 direction = normalize(axis * axial + (Position - axis * axial)
                              * (1.0 - 0.82 * Convergence * front));
    vec3 right = abs(direction.y) > 0.99 ? vec3(1,0,0)
                : normalize(vec3(-direction.z, 0, direction.x));
    vec3 up = cross(right, direction);
    int corner = gl_VertexID % 4;
    starUv = corner == 0 ? vec2(1,1) : corner == 1 ? vec2(-1,1)
           : corner == 2 ? vec2(-1,-1) : vec2(1,-1);
    float size = UV0.x * (1.0 + Convergence * front * 0.65);
    vec3 point = direction * 200.0 + (right * starUv.x + up * starUv.y) * size;
    gl_Position = ProjMat * ModelViewMat * vec4(point, 1.0);
    gl_Position.z = gl_Position.w;
    float twinkle = 0.975 + 0.025 * sin(TimePhase * float(2 + (gl_VertexID / 4) % 4) + UV0.y);
    starColor = vec4(mix(Color.rgb, TintColor, clamp(TintAmount, 0.0, 1.0)),
                     Color.a * StarAlpha * twinkle * (1.0 + Convergence * front * 1.15));
}
