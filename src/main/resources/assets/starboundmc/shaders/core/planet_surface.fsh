#version 150

uniform sampler2D Sampler0;

// Sun direction in the sphere mesh's local frame, so the dot product below is
// invariant under the orientation and spin the model matrix applies.
uniform vec3 SunDirection;
uniform float NightFloor;
uniform float TerminatorWidth;
uniform float GlobalAlpha;

in vec2 texCoord0;
in vec3 surfaceNormal;

out vec4 fragColor;

void main() {
    vec3 normal = normalize(surfaceNormal);
    float sunDot = dot(normal, SunDirection);

    // Day/night terminator: same smoothstep curve the CPU bake used, evaluated
    // per fragment so the boundary stays smooth across the sphere.
    float shade = 1.0 - smoothstep(0.0, 1.0, (sunDot + TerminatorWidth) / (TerminatorWidth * 2.0));
    shade *= 1.0 - NightFloor;
    vec3 light = vec3(
        mix(1.0, 0.06, shade),
        mix(1.0, 0.08, shade),
        mix(1.0, 0.20, shade));

    // Warm lift right at the terminator, scaled down away from it.
    float terminator = max(0.0, 1.0 - abs(sunDot) / TerminatorWidth);
    light.r += (0.90 - light.r) * terminator * 0.35;
    light.g += (0.55 - light.g) * terminator * 0.25;
    light.b += (0.25 - light.b) * terminator * 0.18;

    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < 0.1) {
        discard;
    }
    fragColor = vec4(texel.rgb * light, texel.a) * GlobalAlpha;
}
