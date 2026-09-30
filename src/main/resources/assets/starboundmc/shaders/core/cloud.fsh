#version 150
#moj_import <starboundmc:space_common.glsl>
#moj_import <starboundmc:space_texture.glsl>
uniform float DistanceScale;
uniform float LinearColor;

uniform sampler2D Sampler0;
uniform vec3 SunDirection;
uniform float CloudOpacity;
uniform float GlobalAlpha;

in vec2 texCoord0;
in vec3 cloudNormal;
in vec3 viewPosition;
out vec4 fragColor;

vec3 safeNormalize(vec3 value, vec3 fallbackValue) {
    float lengthSquared = dot(value, value);
    return lengthSquared > 0.00000001 ? value * inversesqrt(lengthSquared) : fallbackValue;
}

void main() {
    vec4 cloud = DistanceScale > 0.0 ? spaceBilinear(Sampler0,texCoord0,true) : texture(Sampler0, texCoord0);
    vec3 normal = safeNormalize(cloudNormal, vec3(0.0, 0.0, 1.0));
    vec3 lightDirection = safeNormalize(SunDirection, normal);
    float sunDot = dot(normal, lightDirection);
    float daylight = smoothstep(-0.15, 0.25, sunDot);

    vec3 nightCloud = cloud.rgb * vec3(0.10, 0.13, 0.18);
    vec3 cloudColor = mix(nightCloud, cloud.rgb, daylight);
    if (DistanceScale > 0.0) {
        cloudColor = cloud.rgb * (.006 + max(sunDot, 0.0) * 2.2);
        if (LinearColor < .5) cloudColor = spaceToDisplay(cloudColor * (1.0 + cloudColor / 16.0) / (1.0 + cloudColor));
    }
    float alpha = cloud.a * CloudOpacity * GlobalAlpha;
    fragColor = vec4(cloudColor, alpha);
    gl_FragDepth = DistanceScale > 0.0
            ? celestialDepth(length(viewPosition) * DistanceScale) : gl_FragCoord.z;
}
