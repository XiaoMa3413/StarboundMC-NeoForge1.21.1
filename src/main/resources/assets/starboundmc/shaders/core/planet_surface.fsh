#version 150
#moj_import <starboundmc:space_common.glsl>
#moj_import <starboundmc:space_texture.glsl>
uniform float DistanceScale;
uniform float LinearColor;

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
in vec3 viewPosition;
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
    float cloudDensity = (DistanceScale > 0.0 ? spaceBilinear(Sampler2,cloudUv,false).a
            : texture(Sampler2,cloudUv).a) * CloudOpacity;
    return clamp(cloudDensity * CloudShadowStrength, 0.0, 0.30);
}

void main() {
    vec4 texel = DistanceScale > 0.0 ? spaceBilinear(Sampler0,texCoord0,true) : texture(Sampler0, texCoord0);
    vec3 normal = safeNormalize(surfaceNormal, vec3(0.0, 0.0, 1.0));
    vec3 lightDirection = safeNormalize(SunDirection, normal);
    float dotNL = clamp(dot(normal, lightDirection), -1.0, 1.0);
    gl_FragDepth = DistanceScale > 0.0
            ? celestialDepth(length(viewPosition) * DistanceScale) : gl_FragCoord.z;
    if (DistanceScale > 0.0) {
        vec3 albedo = texel.rgb;
        vec3 view = safeNormalize(CameraPositionMesh - meshPosition, normal);
        float nl = max(dotNL, 0.0), nv = max(dot(normal, view), 0.001);
        vec2 material = MaterialMaskEnabled > .5 ? spaceBilinear(Sampler1,texCoord0,false).rg : vec2(1, 0);
        float shadow = CloudShadowEnabled > .5 ? projectedCloudShadow(normal, lightDirection, dotNL) : 0.0;
        vec3 ambient = vec3(.45, .65, 1.0) * clamp(NightFloor * .04, .001, .015);
        vec3 radiance = albedo * (ambient + vec3(2.2 * nl * (1.0 - shadow)));
        if (nl > 0.0 && SpecularStrength > 0.0) {
            vec3 halfVector = safeNormalize(lightDirection + view, normal);
            float nh = max(dot(normal, halfVector), 0.0);
            float vh = max(dot(view, halfVector), 0.0);
            float roughness = max(mix(1.0, Roughness, material.r), .08);
            float a2 = pow(roughness, 4.0);
            float denom = nh * nh * (a2 - 1.0) + 1.0;
            float distribution = a2 / max(3.14159265 * denom * denom, .00001);
            float k = pow(roughness + 1.0, 2.0) / 8.0;
            float geometry = nl / (nl * (1.0 - k) + k) * nv / (nv * (1.0 - k) + k);
            float fresnel = .04 + .96 * pow(1.0 - vh, 5.0);
            float specular = distribution * geometry * fresnel / max(4.0 * nl * nv, .001);
            radiance += vec3(specular * SpecularStrength * material.r * 2.2 * nl * (1.0 - shadow));
        }
        // G remains a linear emission mask; diffuse color supplies the authored lava hue.
        radiance += albedo * material.g * max(EmissiveStrength, 0.0) * 6.0;
        radiance *= Brightness;
        if (LinearColor < .5) radiance = spaceToDisplay(radiance * (1.0 + radiance / 16.0) / (1.0 + radiance));
        fragColor = vec4(radiance, texel.a * SurfaceAlpha);
        return;
    }
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
