#version 150
uniform mat4 InverseViewProjection;
uniform vec3 TintColor;
uniform float TintAmount;
in vec2 screenPosition;
out vec4 fragColor;

float hash3(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}
float noise3(vec3 p) {
    vec3 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash3(i), hash3(i + vec3(1,0,0)), f.x),
                   mix(hash3(i + vec3(0,1,0)), hash3(i + vec3(1,1,0)), f.x), f.y),
               mix(mix(hash3(i + vec3(0,0,1)), hash3(i + vec3(1,0,1)), f.x),
                   mix(hash3(i + vec3(0,1,1)), hash3(i + vec3(1,1,1)), f.x), f.y), f.z);
}
// Pure world-direction field. Future curved rays can use the same background function.
vec3 spaceRadiance(vec3 direction) {
    vec3 axis = normalize(vec3(0.24, 0.87, 0.43));
    float latitude = dot(direction, axis);
    float band = exp(-latitude * latitude * 95.0);
    float broad = noise3(direction * 3.1 + vec3(17.2, 4.1, 9.7));
    float detail = noise3(direction * 8.7 + vec3(3.4, 19.1, 7.5));
    float dust = smoothstep(0.35, 0.70, broad * 0.72 + detail * 0.28);
    float core = band * (0.25 + broad * 0.75) * (1.0 - dust * 0.65);
    vec3 black = mix(vec3(0.004, 0.005, 0.008), vec3(0.007, 0.008, 0.012),
                     direction.y * 0.5 + 0.5);
    vec3 light = core * mix(vec3(0.010, 0.012, 0.018), vec3(0.018, 0.014, 0.011), broad);
    vec3 color = black + light;
    return mix(color, color * (0.75 + TintColor * 0.25), clamp(TintAmount, 0.0, 1.0));
}
void main() {
    vec4 ray = InverseViewProjection * vec4(screenPosition, 1.0, 1.0);
    fragColor = vec4(spaceRadiance(normalize(ray.xyz)), 1.0);
}
