#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform vec3 SunDirection;
uniform vec3 CameraPositionMesh;
uniform float TerminatorWidth;
uniform float NightFloor;
uniform float SpecularStrength;
uniform float Roughness;
uniform float FresnelStrength;
uniform float MaterialMaskEnabled;
uniform float EmissiveStrength;
uniform float SurfaceAlpha;
uniform float Brightness;
uniform float CloudShadowEnabled;
uniform float CloudShellScale;
uniform float CloudOpacity;
uniform float CloudShadowStrength;
uniform vec3 CloudRelativeRotationX;
uniform vec3 CloudRelativeRotationY;
uniform vec3 CloudRelativeRotationZ;

in vec2 texCoord0;
in vec3 surfaceNormal;
in vec3 meshPosition;
out vec4 fragColor;

vec3 safeNormalize(vec3 value, vec3 fallbackValue) {
    float lengthSquared = dot(value, value);
    return lengthSquared > 0.00000001 ? value * inversesqrt(lengthSquared) : fallbackValue;
}

vec3 surfaceToCloudFrame(vec3 point) {
    return CloudRelativeRotationX * point.x
            + CloudRelativeRotationY * point.y
            + CloudRelativeRotationZ * point.z;
}

vec2 cloudShellUv(vec3 point) {
    vec3 direction = safeNormalize(point, vec3(1.0, 0.0, 0.0));
    const float PI = 3.14159265358979323846;
    float longitude = dot(direction.xz, direction.xz) > 0.00000001
            ? atan(direction.z, direction.x) : 0.0;
    float u = fract(longitude / (2.0 * PI) + 1.0);
    float v = acos(clamp(direction.y, -1.0, 1.0)) / PI;
    return vec2(u, v);
}

float projectedCloudShadow(vec3 normal, vec3 lightDirection, float dotNL) {
    if (CloudOpacity <= 0.0 || CloudShadowStrength <= 0.0 || dotNL <= 0.0) {
        return 0.0;
    }

    // Work in unit-sphere mesh space. SunDirection already uses this frame,
    // so the ray reaches the cloud shell at the physically projected location.
    vec3 surfacePoint = safeNormalize(meshPosition, normal);
    float surfaceLightDot = dot(surfacePoint, lightDirection);
    if (surfaceLightDot <= 0.0) {
        return 0.0;
    }

    float cloudRadius = max(CloudShellScale, 1.0001);
    float discriminant = surfaceLightDot * surfaceLightDot
            + cloudRadius * cloudRadius - 1.0;
    float rayDistance = -surfaceLightDot + sqrt(max(discriminant, 0.0));
    vec3 shellPointSurfaceFrame = surfacePoint + lightDirection * rayDistance;
    vec3 shellPointCloudFrame = surfaceToCloudFrame(shellPointSurfaceFrame);
    vec2 cloudUv = cloudShellUv(shellPointCloudFrame);
    float cloudDensity = texture(Sampler2, cloudUv).a * CloudOpacity;
    return clamp(cloudDensity * CloudShadowStrength, 0.0, 0.30);
}

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    vec3 normal = safeNormalize(surfaceNormal, vec3(0.0, 0.0, 1.0));
    vec3 lightDirection = safeNormalize(SunDirection, normal);
    float dotNL = clamp(dot(normal, lightDirection), -1.0, 1.0);
    float width = max(TerminatorWidth, 0.00001);
    float daylight = smoothstep(-width, width, dotNL);
    float shade = 1.0 - daylight;
    shade *= 1.0 - NightFloor;

    vec3 light = mix(vec3(1.0), vec3(0.06, 0.08, 0.20), shade);
    if (CloudShadowEnabled > 0.5) {
        float cloudShadow = projectedCloudShadow(normal, lightDirection, dotNL);
        float directDaylight = daylight * (1.0 - cloudShadow);
        float sunlight = NightFloor + (1.0 - NightFloor) * directDaylight;
        light = mix(vec3(0.06, 0.08, 0.20), vec3(1.0), sunlight);
    }
    float terminator = max(0.0, 1.0 - abs(dotNL) / width);
    light.r += (0.90 - light.r) * terminator * 0.35;
    light.g += (0.55 - light.g) * terminator * 0.25;
    light.b += (0.25 - light.b) * terminator * 0.18;

    float materialMask = 1.0;
    float emissiveMask = 0.0;
    if (MaterialMaskEnabled > 0.5) {
        // Packed material channels: R is smooth/specular and G is emissive.
        vec4 materialSample = texture(Sampler1, texCoord0);
        materialMask = clamp(materialSample.r, 0.0, 1.0);
        emissiveMask = clamp(materialSample.g, 0.0, 1.0);
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

    vec3 baseColor = texel.rgb * materialLight * Brightness;
    vec3 finalColor = baseColor;
    float emissiveStrength = clamp(EmissiveStrength, 0.0, 1.0);
    if (emissiveMask > 0.0 && emissiveStrength > 0.0) {
        // Keep the diffuse texture's lava detail while shifting its glow toward
        // a restrained, dark red instead of the previous bright amber.
        // This contribution is independent of the day/night lighting above.
        vec3 emissiveColor = texel.rgb * vec3(0.72, 0.10, 0.05)
                * emissiveMask * emissiveStrength;
        // Limit the addition before it reaches the ordinary LDR framebuffer.
        finalColor = min(baseColor + emissiveColor, vec3(1.05));
    }

    fragColor = vec4(finalColor, texel.a * SurfaceAlpha);
}
