#version 150
#moj_import <starboundmc:space_common.glsl>
uniform vec3 CenterView;
uniform float SphereRadius;
uniform float DistanceScale;
uniform float CoronaPass;
uniform float Brightness;
uniform float Detail;
uniform float CoronaDetail;
uniform float FlareStrength;
uniform float TimePhase;
uniform float LinearColor;
uniform vec3 SurfaceColor;
uniform vec3 CoronaColor;
uniform mat4 ViewToWorld;
in vec3 viewPosition;
out vec4 fragColor;

float granulation(vec3 p) {
    return fract(sin(dot(floor(p * 110.0), vec3(12.9898,78.233,45.164))) * 43758.5453);
}

void main() {
    vec3 ray = normalize(viewPosition);
    float centerDistance = length(CenterView);
    float along = dot(ray, CenterView);
    float impact2 = max(dot(CenterView, CenterView) - along * along, 0.0);
    float radius2 = SphereRadius * SphereRadius;
    if (CoronaPass < 0.5) {
        if (impact2 > radius2 || along <= 0.0) discard;
        float rayDistance = along - sqrt(max(radius2 - impact2, 0.0));
        vec3 normal = normalize(ray * rayDistance - CenterView);
        float mu = max(dot(normal, -ray), 0.0);
        float limb = 0.4 + 0.6 * mu;
        // Rotation is periodic; broad spots and fine granulation stay attached to the sphere.
        vec3 worldNormal = mat3(ViewToWorld) * normal;
        float c = cos(TimePhase), s = sin(TimePhase);
        vec3 p = vec3(worldNormal.x * c - worldNormal.z * s, worldNormal.y, worldNormal.x * s + worldNormal.z * c);
        float cellFade = 1.0 - smoothstep(.5, 1.5, max(length(dFdx(p)), length(dFdy(p))) * 110.0);
        float cells = mix(1.0, 0.87 + 0.24 * granulation(p), Detail * cellFade);
        float spot = pow(max(dot(p, normalize(vec3(.30, .20, -.90))), 0.0), 350.0)
                + .6 * pow(max(dot(p, normalize(vec3(-.60, -.20, .70))), 0.0), 550.0);
        vec3 color = spaceToLinear(SurfaceColor) * limb * cells * (1.0 - spot * Detail * 0.6);
        color *= 5.0 * Brightness;
        if (LinearColor < 0.5) color = spaceToDisplay(color / (1.0 + color));
        fragColor = vec4(color, 1.0);
        gl_FragDepth = celestialDepth(max(rayDistance, 0.0) * DistanceScale);
    } else {
        float radius = sqrt(impact2) / max(SphereRadius, 0.001);
        if (radius < 1.0 || radius > 3.6 || along <= 0.0) discard;
        float falloff = exp(-(radius - 1.0) * 4.8) * (1.0 - smoothstep(2.2, 3.6, radius));
        vec3 axis = normalize(CenterView);
        vec3 right = normalize(cross(abs(axis.y) > .99 ? vec3(1,0,0) : vec3(0,1,0), axis));
        vec3 perpendicular = mat3(ViewToWorld) * (ray * along - CenterView);
        axis = normalize(mat3(ViewToWorld) * axis);
        right = normalize(cross(abs(axis.y) > .99 ? vec3(1,0,0) : vec3(0,1,0), axis));
        float angle = atan(dot(perpendicular, cross(axis, right)), dot(perpendicular, right));
        float wisps = .78 + .22 * sin(angle * 19.0 + sin(angle * 7.0 + TimePhase) * 2.0);
        vec3 color = spaceToLinear(CoronaColor) * falloff * wisps * Brightness
                * (.15 + .65 * CoronaDetail + FlareStrength * .15);
        if (LinearColor < .5) color = spaceToDisplay(color) * .3;
        fragColor = vec4(color, 0.0);
        gl_FragDepth = celestialDepth(centerDistance * DistanceScale);
    }
}
