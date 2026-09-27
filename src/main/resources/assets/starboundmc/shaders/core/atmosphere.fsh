#version 150

uniform vec3 SunDirection;
uniform vec3 CameraPositionMesh;
uniform vec3 AtmosphereColor;
uniform float AtmosphereStrength;
uniform float GlobalAlpha;
uniform float NightResidual;

in vec3 shellNormal;
in vec3 meshPosition;
out vec4 fragColor;

vec3 safeNormalize(vec3 value, vec3 fallbackValue) {
    float lengthSquared = dot(value, value);
    return lengthSquared > 0.00000001 ? value * inversesqrt(lengthSquared) : fallbackValue;
}

void main() {
    vec3 normal = safeNormalize(shellNormal, vec3(0.0, 0.0, 1.0));
    vec3 viewDirection = safeNormalize(CameraPositionMesh - meshPosition, normal);
    vec3 lightDirection = safeNormalize(SunDirection, normal);

    float viewCosine = clamp(dot(normal, viewDirection), 0.0, 1.0);
    float grazing = 1.0 - viewCosine;
    float innerRise = smoothstep(0.68, 0.82, grazing);
    float outerFade = 1.0 - smoothstep(0.94, 1.0, grazing);
    float limb = innerRise * outerFade;
    float daylight = smoothstep(-0.20, 0.20, dot(normal, lightDirection));
    float illumination = mix(clamp(NightResidual, 0.0, 1.0), 1.0, daylight);
    float alpha = clamp(limb * illumination * AtmosphereStrength * GlobalAlpha, 0.0, 1.0);

    fragColor = vec4(AtmosphereColor, alpha);
}
