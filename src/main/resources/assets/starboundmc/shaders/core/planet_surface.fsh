#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec3 SunDirection;
uniform vec3 CameraPositionMesh;
uniform float TerminatorWidth;
uniform float NightFloor;
uniform float SpecularStrength;
uniform float Roughness;
uniform float FresnelStrength;
uniform float MaterialMaskEnabled;
uniform float SurfaceAlpha;
uniform float Brightness;

in vec2 texCoord0;
in vec3 surfaceNormal;
in vec3 meshPosition;
out vec4 fragColor;

vec3 safeNormalize(vec3 value, vec3 fallbackValue) {
    float lengthSquared = dot(value, value);
    return lengthSquared > 0.00000001 ? value * inversesqrt(lengthSquared) : fallbackValue;
}

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    vec3 normal = safeNormalize(surfaceNormal, vec3(0.0, 0.0, 1.0));
    vec3 lightDirection = safeNormalize(SunDirection, normal);
    float dotNL = clamp(dot(normal, lightDirection), -1.0, 1.0);
    float width = max(TerminatorWidth, 0.00001);
    float shade = 1.0 - smoothstep(-width, width, dotNL);
    shade *= 1.0 - NightFloor;

    vec3 light = mix(vec3(1.0), vec3(0.06, 0.08, 0.20), shade);
    float terminator = max(0.0, 1.0 - abs(dotNL) / width);
    light.r += (0.90 - light.r) * terminator * 0.35;
    light.g += (0.55 - light.g) * terminator * 0.25;
    light.b += (0.25 - light.b) * terminator * 0.18;

    float materialMask = 1.0;
    if (MaterialMaskEnabled > 0.5) {
        // R is the specular/smooth-surface mask: black land, white ocean.
        materialMask = clamp(texture(Sampler1, texCoord0).r, 0.0, 1.0);
    }
    float specularStrength = SpecularStrength * materialMask;
    float roughness = mix(1.0, clamp(Roughness, 0.0, 1.0), materialMask);
    float specular = 0.0;
    float fresnel = 0.0;
    if (specularStrength > 0.0 || FresnelStrength > 0.0) {
        vec3 viewDirection = safeNormalize(CameraPositionMesh - meshPosition, normal);
        if (specularStrength > 0.0 && dotNL > 0.0) {
            vec3 halfVector = safeNormalize(lightDirection + viewDirection, normal);
            float shininess = mix(96.0, 4.0, roughness);
            float roughnessEnergy = mix(1.0, 0.28, roughness);
            specular = specularStrength * roughnessEnergy
                    * pow(max(dot(normal, halfVector), 0.0), shininess);
            specular = clamp(specular, 0.0, 0.18);
        }
        if (FresnelStrength > 0.0) {
            float viewEdge = pow(1.0 - max(dot(normal, viewDirection), 0.0), 5.0);
            fresnel = clamp(FresnelStrength * viewEdge, 0.0, 0.08);
        }
    }
    vec3 materialLight = min(light + vec3(specular)
            + vec3(0.35, 0.55, 0.75) * fresnel, vec3(1.22));

    fragColor = vec4(texel.rgb * materialLight * Brightness, texel.a * SurfaceAlpha);
}
