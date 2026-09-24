#version 150

uniform sampler2D Sampler0;
uniform vec3 SunDirection;
uniform float TerminatorWidth;
uniform float NightFloor;
uniform float SurfaceAlpha;
uniform float Brightness;

in vec2 texCoord0;
in vec3 surfaceNormal;
out vec4 fragColor;

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    float dotNL = clamp(dot(normalize(surfaceNormal), normalize(SunDirection)), -1.0, 1.0);
    float width = max(TerminatorWidth, 0.00001);
    float shade = 1.0 - smoothstep(-width, width, dotNL);
    shade *= 1.0 - NightFloor;

    vec3 light = mix(vec3(1.0), vec3(0.06, 0.08, 0.20), shade);
    float terminator = max(0.0, 1.0 - abs(dotNL) / width);
    light.r += (0.90 - light.r) * terminator * 0.35;
    light.g += (0.55 - light.g) * terminator * 0.25;
    light.b += (0.25 - light.b) * terminator * 0.18;

    fragColor = vec4(texel.rgb * light * Brightness, texel.a * SurfaceAlpha);
}
