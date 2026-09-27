#version 150

uniform vec3 SunDirection;
uniform vec3 CameraPositionMesh;
uniform vec3 AtmosphereColor;
uniform float AtmosphereStrength;
uniform float GlobalAlpha;
uniform float NightFraction;
uniform float TwilightStrength;
uniform float InnerRadius;
uniform float OuterRadius;
uniform float OpticalDepthMax;

in vec3 shellNormal;
in vec3 meshPosition;
out vec4 fragColor;

const vec3 TWILIGHT_TINT = vec3(1.0, 0.32, 0.10);
const float TWILIGHT_WIDTH = 0.28;

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
    float sunDot = clamp(dot(normal, lightDirection), -1.0, 1.0);
    float daylight = smoothstep(-0.20, 0.20, sunDot);
    float illumination = mix(clamp(NightFraction, 0.0, 0.25), 1.0, daylight);
    float twilightBand = 1.0 - smoothstep(0.0, TWILIGHT_WIDTH, abs(sunDot));
    float twilightNightFade = smoothstep(-0.42, -0.06, sunDot);
    float twilight = twilightBand * twilightNightFade;
    vec3 subtleTwilightColor = mix(AtmosphereColor, TWILIGHT_TINT, 0.35);
    vec3 scatterColor = mix(AtmosphereColor, subtleTwilightColor,
            twilight * clamp(TwilightStrength, 0.0, 1.0));
    float alpha = clamp(optical01 * illumination * AtmosphereStrength * GlobalAlpha, 0.0, 1.0);

    fragColor = vec4(scatterColor, alpha);
}
