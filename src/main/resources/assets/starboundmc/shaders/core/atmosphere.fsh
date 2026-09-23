#version 150

uniform vec3 AtmosphereCenter;
uniform float PlanetRadius;
uniform float ShellRadius;
uniform vec3 SunDirection;
uniform vec3 AtmosphereColor;
uniform float Density;

in vec3 shellDirection;
in vec3 shellNormal;
in vec3 sunDirectionView;

out vec4 fragColor;

void main() {
    // Angular distance of this shell point from the planet's centre direction,
    // against the planet's projected limb and the shell's outer edge: the same
    // ramp the CPU bake wrote per vertex, now per fragment.
    float d = length(AtmosphereCenter);
    vec3 toCenter = AtmosphereCenter / max(d, 0.0001);
    float angle = acos(clamp(dot(shellDirection, toCenter), -1.0, 1.0));
    float limbAngle = asin(clamp(PlanetRadius / max(d, 0.0001), 0.0, 1.0));
    float outerAngle = asin(clamp(ShellRadius / max(d, 0.0001), 0.0, 1.0));
    float innerAngle = max(0.0, limbAngle - 0.05);

    float limbFactor = 0.0;
    if (angle >= innerAngle && angle <= limbAngle) {
        float t = clamp((angle - innerAngle) / max(limbAngle - innerAngle, 0.0001), 0.0, 1.0);
        limbFactor = t * t * (3.0 - 2.0 * t);
    } else if (angle > limbAngle && angle <= outerAngle) {
        float t = clamp((angle - limbAngle) / max(outerAngle - limbAngle, 0.0001), 0.0, 1.0);
        limbFactor = pow(1.0 - t, 1.5);
    }

    // Sun dependence: full on the day side, a faint contour on the night side,
    // and a small twilight lift around the terminator.
    float sunDot = dot(normalize(shellNormal), normalize(sunDirectionView));
    float twilight = exp(-abs(sunDot) * 5.0) * 0.22;
    float sunFacing = 0.12 + 0.88 * smoothstep(-0.35, 0.25, sunDot) + twilight;

    float alpha = Density * limbFactor * sunFacing;
    fragColor = vec4(AtmosphereColor, alpha);
}
