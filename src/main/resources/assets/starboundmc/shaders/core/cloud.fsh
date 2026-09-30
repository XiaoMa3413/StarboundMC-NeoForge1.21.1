#version 150
#moj_import <starboundmc:space_common.glsl>
#moj_import <starboundmc:space_texture.glsl>
#moj_import <starboundmc:space_atmosphere.glsl>
#moj_import <starboundmc:space_cloud_optics.glsl>
uniform float AtmosphereGroundRadius;
uniform float AtmosphereTopRadius;
uniform vec3 AtmosphereColor;
uniform float AtmosphereStrength;
uniform float DistanceScale;
uniform float LinearColor;

uniform sampler2D Sampler0;
uniform vec3 SunDirection;
uniform vec3 CameraPositionMesh;
uniform float CloudOpacity;
uniform float GlobalAlpha;

in vec2 texCoord0;
in vec3 cloudNormal;
in vec3 meshPosition;
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
    float alpha = cloud.a * CloudOpacity * GlobalAlpha;
    if (DistanceScale > 0.0) {
        vec3 view = safeNormalize(CameraPositionMesh-meshPosition,normal);
        float opticalDepth = spaceCloudOpticalDepth(cloud.a,CloudOpacity);
        float nv = abs(dot(normal,view));
        alpha = (1.0-spaceCloudTransmission(opticalDepth,nv))*GlobalAlpha;
        vec3 sunlight = spaceSurfaceSun(normal*50.0,lightDirection,AtmosphereGroundRadius,
                AtmosphereTopRadius,AtmosphereColor,AtmosphereStrength);
        if (AtmosphereStrength <= 0.0)
            sunlight *= spaceSunVisibility(normal*50.0,lightDirection,AtmosphereGroundRadius);
        // A forward-scattered component lets sunlit thin edges wrap around the
        // terminator; dense interiors suppress this light instead of glowing.
        float forward = spaceCloudPhase(-dot(view,lightDirection));
        float through = spaceCloudTransmission(opticalDepth,sunDot);
        cloudColor = cloud.rgb * (vec3(.0015) + sunlight *
                (max(sunDot,0.0)*2.2 + SPACE_SOLAR_IRRADIANCE*forward*through*.22));
        if (LinearColor < .5) cloudColor = spaceToDisplay(cloudColor * (1.0 + cloudColor / 16.0) / (1.0 + cloudColor));
    }
    fragColor = vec4(cloudColor, alpha);
    gl_FragDepth = DistanceScale > 0.0
            ? celestialDepth(length(viewPosition) * DistanceScale) : gl_FragCoord.z;
}
