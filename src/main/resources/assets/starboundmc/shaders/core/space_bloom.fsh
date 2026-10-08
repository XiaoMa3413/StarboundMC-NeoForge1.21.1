#version 150
uniform sampler2D Sampler0;
uniform vec2 TexelSize;
uniform float Prefilter;
uniform float Gain;
in vec2 texCoord;
out vec4 fragColor;
vec3 tap(vec2 offset) {
    vec3 c = max(texture(Sampler0, texCoord + offset * TexelSize).rgb, vec3(0));
    if (Prefilter > .5) {
        float energy = max(c.r, max(c.g, c.b));
        float knee = clamp(energy - .4, 0.0, .8);
        float contribution = max(energy - .8, knee * knee / 1.6);
        c *= contribution / max(energy, .00001);
    }
    return c;
}
void main() {
    vec3 c = tap(vec2(0)) * 4.0;
    c += (tap(vec2(-1,0)) + tap(vec2(1,0)) + tap(vec2(0,-1)) + tap(vec2(0,1))) * 2.0;
    c += tap(vec2(-1,-1)) + tap(vec2(-1,1)) + tap(vec2(1,-1)) + tap(vec2(1,1));
    fragColor = vec4(c * (Gain / 16.0), 0);
}
