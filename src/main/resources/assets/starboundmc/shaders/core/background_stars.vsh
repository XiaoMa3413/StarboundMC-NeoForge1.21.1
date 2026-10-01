#version 150
#moj_import <starboundmc:space_common.glsl>
#moj_import <starboundmc:space_background_field.glsl>
in vec3 Position;
in vec2 UV0;
in vec4 Color;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 ConvergenceAxis;
uniform float Convergence;
uniform float StarAlpha;
uniform vec3 TintColor;
uniform float TintAmount;
uniform vec2 ViewportSize;
uniform float FieldDetail;
out vec2 pixelOffset;
flat out vec3 starRadiance;
flat out vec2 starOptics;
flat out float starVisibility;
void main() {
    vec3 axis = normalize(ConvergenceAxis);
    float axial = dot(Position, axis);
    float front = smoothstep(-0.05, 1.0, axial);
    vec3 direction = normalize(axis * axial + (Position - axis * axial)
                              * (1.0 - 0.82 * Convergence * front));
    int corner = gl_VertexID % 4;
    vec2 uv = corner == 0 ? vec2(1,1) : corner == 1 ? vec2(-1,1)
            : corner == 2 ? vec2(-1,-1) : vec2(1,-1);
    // Screen-aligned support avoids pole singularities and off-axis foreshortening.
    float extent = UV0.y > .15 ? 7.25 : 2.5;
    pixelOffset = uv*extent;
    gl_Position = ProjMat * ModelViewMat * vec4(direction * 200.0, 1.0);
    gl_Position.xy += pixelOffset*(2.0/max(ViewportSize,vec2(1)))*gl_Position.w;
    gl_Position.z = gl_Position.w;
    vec3 color=spaceToLinear(mix(Color.rgb,TintColor,clamp(TintAmount,0.0,1.0)));
    color/=max(dot(color,vec3(.2126,.7152,.0722)),.05);
    vec3 transmission = Color.a > .001
            ? pow(spaceBackgroundTransmission(normalize(Position),FieldDetail),vec3(Color.a)) : vec3(1);
    starRadiance=color*transmission;
    starOptics = UV0; // sigma and unquantized integrated flux, both float attributes
    starVisibility = StarAlpha;
}
