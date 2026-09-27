#version 150

uniform vec3 SunDirection;
uniform vec3 CameraPositionMesh;
uniform vec3 AtmosphereColor;
uniform float AtmosphereStrength;
uniform float GlobalAlpha;
uniform float NightResidual;
uniform float InnerRadius;
uniform float OuterRadius;
uniform float OpticalDepthMax;

in vec3 shellNormal;
in vec3 meshPosition;
out vec4 fragColor;

vec3 safeNormalize(vec3 value, vec3 fallbackValue) {
    float lengthSquared = dot(value, value);
    return lengthSquared > 0.00000001 ? value * inversesqrt(lengthSquared) : fallbackValue;
}

void main() {
    vec3 normal = safeNormalize(shellNormal, vec3(0.0, 0.0, 1.0));
    vec3 lightDirection = safeNormalize(SunDirection, normal);

    vec3 rayVector = meshPosition - CameraPositionMesh;
    float rayLengthSquared = dot(rayVector, rayVector);
    float opticalDepth = 0.0;
    if (rayLengthSquared > 0.00000001) {
        vec3 rayDirection = rayVector * inversesqrt(rayLengthSquared);
        float impact = length(cross(CameraPositionMesh, rayDirection));
        float outerPath = sqrt(max(OuterRadius * OuterRadius - impact * impact, 0.0));

        if (impact < InnerRadius) {
            float innerPath = sqrt(max(InnerRadius * InnerRadius - impact * impact, 0.0));
            opticalDepth = max(outerPath - innerPath, 0.0);
        } else if (impact < OuterRadius) {
            opticalDepth = 2.0 * outerPath;
        }
    }

    float optical01 = clamp(opticalDepth / max(OpticalDepthMax, 0.00001), 0.0, 1.0);
    optical01 = pow(optical01, 1.15);
    float daylight = smoothstep(-0.20, 0.20, dot(normal, lightDirection));
    float illumination = mix(clamp(NightResidual, 0.0, 1.0), 1.0, daylight);
    float alpha = clamp(optical01 * illumination * AtmosphereStrength * GlobalAlpha, 0.0, 1.0);

    fragColor = vec4(AtmosphereColor, alpha);
}
